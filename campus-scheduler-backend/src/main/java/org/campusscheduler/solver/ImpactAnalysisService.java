package org.campusscheduler.solver;

import ai.timefold.solver.core.api.score.ScoreExplanation;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import lombok.RequiredArgsConstructor;
import org.campusscheduler.domain.room.Room;
import org.campusscheduler.domain.room.RoomRepository;
import org.campusscheduler.domain.schedule.Schedule;
import org.campusscheduler.domain.schedule.ScheduleRepository;
import org.campusscheduler.domain.timeslot.TimeSlot;
import org.campusscheduler.domain.timeslot.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for running impact analysis using Timefold Solver.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImpactAnalysisService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SolverManagerConfiguration solverConfig;
    private final ScheduleRepository scheduleRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;

    public Optional<ImpactAnalysisResponse> analyze(ImpactAnalysisRequest request) {
        if (request.getProposedRoomId() == null && request.getProposedTimeSlotId() == null) {
            throw new ImpactAnalysisStateException("At least one proposed change is required");
        }

        Optional<Schedule> scheduleOpt = scheduleRepository.findById(request.getScheduleId());
        if (scheduleOpt.isEmpty()) {
            return Optional.empty();
        }

        Schedule schedule = scheduleOpt.get();
        Room targetRoom = resolveRoom(request.getProposedRoomId());
        if (request.getProposedRoomId() != null && targetRoom == null) {
            return Optional.empty();
        }
        TimeSlot targetTimeSlot = resolveTimeSlot(request.getProposedTimeSlotId());
        if (request.getProposedTimeSlotId() != null && targetTimeSlot == null) {
            return Optional.empty();
        }

        if (targetRoom == null) {
            targetRoom = schedule.getRoom();
        }
        if (targetTimeSlot == null) {
            targetTimeSlot = schedule.getTimeSlot();
        }

        String semester = schedule.getSemester();
        List<Schedule> schedules = scheduleRepository.findBySemester(semester);
        Map<Long, ScheduleSnapshot> baseline = new HashMap<>();
        List<ScheduleAssignment> assignments = new ArrayList<>();

        for (Schedule item : schedules) {
            ScheduleAssignment assignment = new ScheduleAssignment();
            assignment.setId(item.getId());
            assignment.setCourse(item.getCourse());
            assignment.setSemester(item.getSemester());
            assignment.setRoom(item.getRoom());
            assignment.setTimeSlot(item.getTimeSlot());
            assignment.setPinned(true);
            assignments.add(assignment);
            baseline.put(item.getId(), ScheduleSnapshot.from(item));
        }

        ScheduleAssignment targetAssignment = assignments.stream()
                .filter(a -> a.getId().equals(schedule.getId()))
                .findFirst()
                .orElse(null);
        if (targetAssignment == null) {
            return Optional.empty();
        }

        targetAssignment.setRoom(targetRoom);
        targetAssignment.setTimeSlot(targetTimeSlot);
        targetAssignment.setPinned(true);

        Schedule conflictSchedule = findConflictSchedule(schedule, schedules, targetRoom, targetTimeSlot);
        if (conflictSchedule != null) {
            assignments.stream()
                    .filter(a -> a.getId().equals(conflictSchedule.getId()))
                    .findFirst()
                    .ifPresent(a -> a.setPinned(false));
        }

        ScheduleSolution problem = ScheduleSolution.builder()
                .rooms(roomRepository.findAll())
                .timeSlots(timeSlotRepository.findAll())
                .assignments(assignments)
                .semester(semester)
                .build();

        SolverFactory<ScheduleSolution> factory = solverConfig.createImpactSolverFactory();
        Solver<ScheduleSolution> solver = factory.buildSolver();
        ScheduleSolution solution = solver.solve(problem);

        SolutionManager<ScheduleSolution, HardSoftScore> solutionManager = SolutionManager.create(factory);
        ScoreExplanation<ScheduleSolution, HardSoftScore> explanation = solutionManager.explain(solution);
        HardSoftScore score = explanation.getScore();

        List<ImpactConstraintSummary> constraintSummaries = explanation.getConstraintMatchTotalMap()
                .values()
                .stream()
                .sorted(Comparator.comparing(ConstraintMatchTotal::getConstraintName))
                .map(total -> ImpactConstraintSummary.builder()
                        .constraintName(total.getConstraintRef().constraintName())
                        .constraintId(total.getConstraintRef().constraintId())
                        .score(String.valueOf(total.getScore()))
                        .build())
                .toList();

        List<ImpactAnalysisMove> moves = new ArrayList<>();
        if (solution.getAssignments() != null) {
            for (ScheduleAssignment assignment : solution.getAssignments()) {
                ScheduleSnapshot snapshot = baseline.get(assignment.getId());
                if (snapshot == null) {
                    continue;
                }
                Room fromRoom = snapshot.room();
                TimeSlot fromTimeSlot = snapshot.timeSlot();
                Room toRoom = assignment.getRoom();
                TimeSlot toTimeSlot = assignment.getTimeSlot();
                if (fromRoom == null || fromTimeSlot == null || toRoom == null || toTimeSlot == null) {
                    continue;
                }
                if (fromRoom.getId().equals(toRoom.getId())
                        && fromTimeSlot.getId().equals(toTimeSlot.getId())) {
                    continue;
                }

                moves.add(ImpactAnalysisMove.builder()
                        .scheduleId(assignment.getId())
                        .courseCode(assignment.getCourse() != null ? assignment.getCourse().getCode() : "")
                        .fromRoomId(fromRoom.getId())
                        .fromRoomLabel(formatRoom(fromRoom))
                        .toRoomId(toRoom.getId())
                        .toRoomLabel(formatRoom(toRoom))
                        .fromTimeSlotId(fromTimeSlot.getId())
                        .fromTimeSlotLabel(formatTimeSlot(fromTimeSlot))
                        .toTimeSlotId(toTimeSlot.getId())
                        .toTimeSlotLabel(formatTimeSlot(toTimeSlot))
                        .build());
            }
        }

        ImpactAnalysisResponse.Status status = score != null && score.hardScore() >= 0
                ? ImpactAnalysisResponse.Status.SOLVED
                : ImpactAnalysisResponse.Status.NO_SOLUTION;

        return Optional.of(ImpactAnalysisResponse.builder()
                .status(status)
                .score(score != null ? score.toString() : null)
                .scoreSummary(explanation.getSummary())
                .moves(moves)
                .constraintSummaries(constraintSummaries)
                .build());
    }

    private Schedule findConflictSchedule(Schedule target, List<Schedule> schedules, Room room, TimeSlot timeSlot) {
        if (room != null && timeSlot != null) {
            for (Schedule schedule : schedules) {
                if (schedule.getId().equals(target.getId())) {
                    continue;
                }
                if (schedule.getRoom() == null || schedule.getTimeSlot() == null) {
                    continue;
                }
                if (!schedule.getTimeSlot().getDayOfWeek().equals(timeSlot.getDayOfWeek())) {
                    continue;
                }
                if (schedule.getRoom().getId().equals(room.getId())
                        && schedule.getTimeSlot().overlapsWith(timeSlot)) {
                    return schedule;
                }
            }
        }

        if (target.getCourse() != null && target.getCourse().getInstructor() != null) {
            Long instructorId = target.getCourse().getInstructor().getId();
            for (Schedule schedule : schedules) {
                if (schedule.getId().equals(target.getId())) {
                    continue;
                }
                if (schedule.getCourse() == null || schedule.getCourse().getInstructor() == null) {
                    continue;
                }
                if (!schedule.getCourse().getInstructor().getId().equals(instructorId)) {
                    continue;
                }
                if (schedule.getTimeSlot() == null || timeSlot == null) {
                    continue;
                }
                if (schedule.getTimeSlot().overlapsWith(timeSlot)) {
                    return schedule;
                }
            }
        }

        return null;
    }

    private Room resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId).orElse(null);
    }

    private TimeSlot resolveTimeSlot(Long timeSlotId) {
        if (timeSlotId == null) {
            return null;
        }
        return timeSlotRepository.findById(timeSlotId).orElse(null);
    }

    private String formatRoom(Room room) {
        String code = room.getBuildingCode();
        if (code != null && !code.isBlank()) {
            return code + " " + room.getRoomNumber();
        }
        return room.getRoomNumber();
    }

    private String formatTimeSlot(TimeSlot timeSlot) {
        if (timeSlot.getLabel() != null && !timeSlot.getLabel().isBlank()) {
            return timeSlot.getLabel();
        }
        String day = timeSlot.getDayOfWeek() != null ? timeSlot.getDayOfWeek().toString() : "";
        String start = timeSlot.getStartTime() != null ? timeSlot.getStartTime().format(TIME_FORMAT) : "";
        String end = timeSlot.getEndTime() != null ? timeSlot.getEndTime().format(TIME_FORMAT) : "";
        return String.format("%s %s-%s", day, start, end).trim();
    }

    private record ScheduleSnapshot(Room room, TimeSlot timeSlot) {
        static ScheduleSnapshot from(Schedule schedule) {
            return new ScheduleSnapshot(schedule.getRoom(), schedule.getTimeSlot());
        }
    }
}
