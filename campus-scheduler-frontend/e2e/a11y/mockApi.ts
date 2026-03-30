import type { Page, Route } from '@playwright/test'
import type { A11yMockGap, A11yRole, A11yScenario, A11yTheme } from '../../scripts/a11y/types'

interface MockInstallOptions {
	route: string
	role: A11yRole
	theme: A11yTheme
	scenario: A11yScenario
}

interface MockState {
	buildings: Array<{ id: number; name: string; code: string; address: string | null }>
	rooms: Array<{
		id: number
		roomNumber: string
		capacity: number
		type: string
		features: string | null
		buildingId: number | null
		buildingCode: string | null
		buildingName: string | null
	}>
	instructors: Array<{
		id: number
		firstName: string
		lastName: string
		email: string
		department: string | null
		officeNumber: string | null
	}>
	courses: Array<{
		id: number
		code: string
		name: string
		description: string | null
		credits: number
		enrollmentCapacity: number
		department: string | null
		instructor: { id: number; firstName: string; lastName: string; email: string } | null
	}>
	timeslots: Array<{
		id: number
		dayOfWeek: string
		startTime: string
		endTime: string
		label: string | null
	}>
	schedules: Array<{
		id: number
		course: {
			id: number
			code: string
			name: string
			description: string | null
			credits: number
			enrollmentCapacity: number
			department: string | null
			instructor: { id: number; firstName: string; lastName: string; email: string } | null
		}
		room: {
			id: number
			roomNumber: string
			capacity: number
			type: string
			features: string | null
			buildingId: number | null
			buildingCode: string | null
			buildingName: string | null
		}
		timeSlot: {
			id: number
			dayOfWeek: string
			startTime: string
			endTime: string
			label: string | null
		}
		semester: string
	}>
	changeRequests: Array<{
		id: number
		schedule: MockState['schedules'][number]
		requestedByInstructor: MockState['instructors'][number]
		requestedByRole: 'INSTRUCTOR' | 'ADMIN'
		status: 'PENDING' | 'APPROVED' | 'REJECTED'
		reasonCategory: 'MEDICAL' | 'EQUIPMENT_FAILURE' | 'PEDAGOGICAL_CONFLICT' | 'OTHER'
		reasonDetails: string | null
		proposedRoom: MockState['rooms'][number] | null
		proposedTimeSlot: MockState['timeslots'][number] | null
		originalRoomId: number
		originalTimeSlotId: number
		originalSemester: string
		decisionNote: string | null
		createdAt: string
		reviewedAt: string | null
		appliedAt: string | null
	}>
}

interface MockStudent {
	id: number
	studentNumber: string
	firstName: string
	lastName: string
	email: string
	department: string | null
	yearLevel: number | null
	targetCourseLoad: number | null
	preferredCourseIds: number[]
}

interface MockRoomBookingParticipant {
	id: number
	fullName: string
	email: string
}

interface MockRoomBooking {
	id: number
	room: MockState['rooms'][number]
	timeSlot: MockState['timeslots'][number]
	semester: string
	bookingDate: string | null
	createdAt: string
	participantCount: number
	viewerCanSeeStudentDetails: boolean
	viewerIsOwner: boolean
	viewerIsParticipant: boolean
	bookedBy: MockRoomBookingParticipant | null
	participants: MockRoomBookingParticipant[]
}

interface MockStudentFixtures {
	students: MockStudent[]
	semester: string
	roomBookings: MockRoomBooking[]
	plans: Record<number, { enrolledScheduleIds: number[]; waitlistedScheduleIds: number[] }>
}

const semesterDefinitions = [
	{
		term: 'WINTER',
		displayName: 'Winter',
		startMonth: 12,
		startDay: 21,
		endMonth: 3,
		endDay: 20,
		startYearOffset: -1,
		endYearOffset: 0,
	},
	{
		term: 'SPRING',
		displayName: 'Spring',
		startMonth: 3,
		startDay: 21,
		endMonth: 6,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
	{
		term: 'SUMMER',
		displayName: 'Summer',
		startMonth: 6,
		startDay: 21,
		endMonth: 9,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
	{
		term: 'FALL',
		displayName: 'Fall',
		startMonth: 9,
		startDay: 21,
		endMonth: 12,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
]

function createNormalState(): MockState {
	const buildings = [
		{ id: 1, code: 'ENG', name: 'Engineering Building', address: '123 Tech Lane' },
		{ id: 2, code: 'SCI', name: 'Science Center', address: '456 Lab Road' },
	]

	const rooms = [
		{
			id: 1,
			buildingId: 1,
			buildingCode: 'ENG',
			buildingName: 'Engineering Building',
			roomNumber: '101',
			capacity: 30,
			type: 'CLASSROOM',
			features: 'Projector',
		},
		{
			id: 2,
			buildingId: 2,
			buildingCode: 'SCI',
			buildingName: 'Science Center',
			roomNumber: '202',
			capacity: 45,
			type: 'LAB',
			features: 'Computers',
		},
	]

	const instructors = [
		{
			id: 10,
			firstName: 'Ada',
			lastName: 'Lovelace',
			email: 'ada@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-201',
		},
		{
			id: 11,
			firstName: 'Alan',
			lastName: 'Turing',
			email: 'alan@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-202',
		},
	]

	const courses = [
		{
			id: 20,
			code: 'CS101',
			name: 'Intro to Programming',
			description: null,
			credits: 3,
			enrollmentCapacity: 30,
			department: 'Computer Science',
			instructor: {
				id: 10,
				firstName: 'Ada',
				lastName: 'Lovelace',
				email: 'ada@campus.edu',
			},
		},
		{
			id: 21,
			code: 'CS201',
			name: 'Data Structures',
			description: null,
			credits: 3,
			enrollmentCapacity: 40,
			department: 'Computer Science',
			instructor: {
				id: 11,
				firstName: 'Alan',
				lastName: 'Turing',
				email: 'alan@campus.edu',
			},
		},
	]

	const timeslots = [
		{
			id: 1,
			dayOfWeek: 'MONDAY',
			startTime: '09:00',
			endTime: '10:00',
			label: 'Period 1',
		},
		{
			id: 2,
			dayOfWeek: 'TUESDAY',
			startTime: '10:00',
			endTime: '11:00',
			label: 'Period 2',
		},
		{
			id: 3,
			dayOfWeek: 'WEDNESDAY',
			startTime: '11:00',
			endTime: '12:00',
			label: 'Period 3',
		},
	]

	const schedules = [
		{
			id: 50,
			semester: 'Fall 2026',
			course: courses[0],
			room: rooms[0],
			timeSlot: timeslots[0],
		},
		{
			id: 51,
			semester: 'Fall 2026',
			course: courses[1],
			room: rooms[1],
			timeSlot: timeslots[1],
		},
	]

	const changeRequests = [
		{
			id: 1,
			schedule: schedules[0],
			requestedByInstructor: instructors[0],
			requestedByRole: 'INSTRUCTOR' as const,
			status: 'PENDING' as const,
			reasonCategory: 'OTHER' as const,
			reasonDetails: 'Need a later room',
			proposedRoom: rooms[1],
			proposedTimeSlot: timeslots[1],
			originalRoomId: schedules[0].room.id,
			originalTimeSlotId: schedules[0].timeSlot.id,
			originalSemester: schedules[0].semester,
			decisionNote: null,
			createdAt: '2026-02-12T12:00:00Z',
			reviewedAt: null,
			appliedAt: null,
		},
	]

	return {
		buildings,
		rooms,
		instructors,
		courses,
		timeslots,
		schedules,
		changeRequests,
	}
}

function createEmptyState(): MockState {
	const building = {
		id: 1,
		code: 'ENG',
		name: 'Engineering Building',
		address: '123 Tech Lane',
	}

	const room = {
		id: 1,
		buildingId: 1,
		buildingCode: 'ENG',
		buildingName: 'Engineering Building',
		roomNumber: '100',
		capacity: 20,
		type: 'CLASSROOM',
		features: null,
	}

	const instructor = {
		id: 10,
		firstName: 'Ada',
		lastName: 'Lovelace',
		email: 'ada@campus.edu',
		department: 'Computer Science',
		officeNumber: 'CS-201',
	}

	const course = {
		id: 20,
		code: 'CS100',
		name: 'Foundations of Computing',
		description: null,
		credits: 3,
		enrollmentCapacity: 20,
		department: 'Computer Science',
		instructor: {
			id: instructor.id,
			firstName: instructor.firstName,
			lastName: instructor.lastName,
			email: instructor.email,
		},
	}

	const timeslot = {
		id: 1,
		dayOfWeek: 'MONDAY',
		startTime: '09:00',
		endTime: '10:00',
		label: 'Period 1',
	}

	return {
		buildings: [building],
		rooms: [room],
		instructors: [instructor],
		courses: [course],
		timeslots: [timeslot],
		schedules: [],
		changeRequests: [],
	}
}

function createDenseState(): MockState {
	const state = createNormalState()

	const extraBuildings = [
		{ id: 3, code: 'ART', name: 'Arts Pavilion', address: '50 Studio Drive' },
		{ id: 4, code: 'LIB', name: 'Library Annex', address: '12 Archive Way' },
	]
	state.buildings.push(...extraBuildings)

	const extraRooms = [
		{
			id: 3,
			buildingId: 3,
			buildingCode: 'ART',
			buildingName: 'Arts Pavilion',
			roomNumber: 'A-310',
			capacity: 60,
			type: 'LECTURE_HALL',
			features: 'Projector, Microphone, Assistive Listening',
		},
		{
			id: 4,
			buildingId: 4,
			buildingCode: 'LIB',
			buildingName: 'Library Annex',
			roomNumber: 'L-120',
			capacity: 24,
			type: 'SEMINAR',
			features: 'Whiteboard, Video Conferencing',
		},
		{
			id: 5,
			buildingId: 1,
			buildingCode: 'ENG',
			buildingName: 'Engineering Building',
			roomNumber: 'E-220',
			capacity: 48,
			type: 'LAB',
			features: 'Computers, Projector',
		},
	]
	state.rooms.push(...extraRooms)

	const extraInstructors = [
		{
			id: 12,
			firstName: 'Grace',
			lastName: 'Hopper',
			email: 'grace@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-203',
		},
		{
			id: 13,
			firstName: 'Katherine',
			lastName: 'Johnson',
			email: 'katherine@campus.edu',
			department: 'Mathematics',
			officeNumber: 'MATH-110',
		},
		{
			id: 14,
			firstName: 'Margaret',
			lastName: 'Hamilton',
			email: 'margaret@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-204',
		},
	]
	state.instructors.push(...extraInstructors)

	const extraCourses = [
		{
			id: 22,
			code: 'CS310',
			name: 'Operating Systems and Concurrent Workloads',
			description: 'Dense-profile seed course to exercise long-list rendering paths.',
			credits: 4,
			enrollmentCapacity: 55,
			department: 'Computer Science',
			instructor: {
				id: 12,
				firstName: 'Grace',
				lastName: 'Hopper',
				email: 'grace@campus.edu',
			},
		},
		{
			id: 23,
			code: 'MATH330',
			name: 'Applied Numerical Methods',
			description: 'Cross-department course for analytics filtering states.',
			credits: 3,
			enrollmentCapacity: 45,
			department: 'Mathematics',
			instructor: {
				id: 13,
				firstName: 'Katherine',
				lastName: 'Johnson',
				email: 'katherine@campus.edu',
			},
		},
		{
			id: 24,
			code: 'CS401',
			name: 'Software Reliability Engineering',
			description: 'High-enrollment elective used by dense scenario mocks.',
			credits: 4,
			enrollmentCapacity: 65,
			department: 'Computer Science',
			instructor: {
				id: 14,
				firstName: 'Margaret',
				lastName: 'Hamilton',
				email: 'margaret@campus.edu',
			},
		},
	]
	state.courses.push(...extraCourses)

	const extraTimeslots = [
		{
			id: 4,
			dayOfWeek: 'THURSDAY',
			startTime: '13:00',
			endTime: '14:30',
			label: 'Period 4',
		},
		{
			id: 5,
			dayOfWeek: 'FRIDAY',
			startTime: '08:00',
			endTime: '09:30',
			label: 'Period 5',
		},
		{
			id: 6,
			dayOfWeek: 'FRIDAY',
			startTime: '15:00',
			endTime: '16:30',
			label: 'Period 6',
		},
	]
	state.timeslots.push(...extraTimeslots)

	const extraSchedules = [
		{
			id: 52,
			semester: 'Fall 2026',
			course: state.courses[2],
			room: state.rooms[2],
			timeSlot: state.timeslots[2],
		},
		{
			id: 53,
			semester: 'Fall 2026',
			course: state.courses[3],
			room: state.rooms[3],
			timeSlot: state.timeslots[3],
		},
		{
			id: 54,
			semester: 'Fall 2026',
			course: state.courses[4],
			room: state.rooms[4],
			timeSlot: state.timeslots[4],
		},
		{
			id: 55,
			semester: 'Fall 2026',
			course: state.courses[0],
			room: state.rooms[1],
			timeSlot: state.timeslots[5],
		},
	]
	state.schedules.push(...extraSchedules)

	state.changeRequests.push(
		{
			id: 2,
			schedule: state.schedules[1],
			requestedByInstructor: state.instructors[1],
			requestedByRole: 'INSTRUCTOR',
			status: 'APPROVED',
			reasonCategory: 'PEDAGOGICAL_CONFLICT',
			reasonDetails: 'Need equipment setup before class',
			proposedRoom: state.rooms[2],
			proposedTimeSlot: state.timeslots[3],
			originalRoomId: state.schedules[1].room.id,
			originalTimeSlotId: state.schedules[1].timeSlot.id,
			originalSemester: state.schedules[1].semester,
			decisionNote: 'Approved for accessibility equipment availability.',
			createdAt: '2026-02-10T10:00:00Z',
			reviewedAt: '2026-02-10T13:00:00Z',
			appliedAt: '2026-02-10T13:05:00Z',
		},
		{
			id: 3,
			schedule: state.schedules[2],
			requestedByInstructor: state.instructors[2],
			requestedByRole: 'INSTRUCTOR',
			status: 'REJECTED',
			reasonCategory: 'OTHER',
			reasonDetails: 'Prefers a different room wing',
			proposedRoom: state.rooms[3],
			proposedTimeSlot: null,
			originalRoomId: state.schedules[2].room.id,
			originalTimeSlotId: state.schedules[2].timeSlot.id,
			originalSemester: state.schedules[2].semester,
			decisionNote: 'Rejected due to capacity shortfall.',
			createdAt: '2026-02-11T08:30:00Z',
			reviewedAt: '2026-02-11T09:00:00Z',
			appliedAt: null,
		},
		{
			id: 4,
			schedule: state.schedules[3],
			requestedByInstructor: state.instructors[3],
			requestedByRole: 'INSTRUCTOR',
			status: 'PENDING',
			reasonCategory: 'EQUIPMENT_FAILURE',
			reasonDetails: 'Projector is non-functional in assigned room.',
			proposedRoom: state.rooms[2],
			proposedTimeSlot: state.timeslots[5],
			originalRoomId: state.schedules[3].room.id,
			originalTimeSlotId: state.schedules[3].timeSlot.id,
			originalSemester: state.schedules[3].semester,
			decisionNote: null,
			createdAt: '2026-02-12T14:10:00Z',
			reviewedAt: null,
			appliedAt: null,
		}
	)

	return state
}

function createState(scenario: A11yScenario): MockState {
	if (scenario === 'empty') return createEmptyState()
	if (scenario === 'dense') return createDenseState()
	return createNormalState()
}

function asJson(route: Route, payload: unknown, status = 200): Promise<void> {
	return route.fulfill({
		status,
		contentType: 'application/json',
		body: JSON.stringify(payload),
	})
}

function getNumericPathId(pathname: string): number | null {
	const match = pathname.match(/\/(\d+)(?:$|\/)/)
	if (!match?.[1]) return null
	return Number(match[1])
}

function nextId(items: Array<{ id: number }>): number {
	return items.length > 0 ? Math.max(...items.map(item => item.id)) + 1 : 1
}

function buildStudentFixtures(state: MockState): MockStudentFixtures {
	const students: MockStudent[] = [
		{
			id: 101,
			studentNumber: 'S100101',
			firstName: 'Maya',
			lastName: 'Patel',
			email: 'maya.patel@students.campus.edu',
			department: 'Computer Science',
			yearLevel: 3,
			targetCourseLoad: 4,
			preferredCourseIds: state.courses.slice(0, 2).map(course => course.id),
		},
		{
			id: 102,
			studentNumber: 'S100102',
			firstName: 'Jonah',
			lastName: 'Lee',
			email: 'jonah.lee@students.campus.edu',
			department: 'Computer Science',
			yearLevel: 2,
			targetCourseLoad: 4,
			preferredCourseIds: state.courses.slice(1, 3).map(course => course.id),
		},
		{
			id: 103,
			studentNumber: 'S100103',
			firstName: 'Priya',
			lastName: 'Singh',
			email: 'priya.singh@students.campus.edu',
			department: 'Mathematics',
			yearLevel: 4,
			targetCourseLoad: 3,
			preferredCourseIds: state.courses.slice(0, 1).map(course => course.id),
		},
	]

	const semester = state.schedules[0]?.semester ?? 'Fall 2026'
	const owner = students[0] as MockStudent
	const invited = students[1] as MockStudent
	const bookingRoom = state.rooms.find(room => room.id !== state.schedules[0]?.room.id) ?? state.rooms[0]
	const bookingTimeSlot = state.timeslots[2] ?? state.timeslots[0]
	const roomBookings: MockRoomBooking[] = []

	if (state.schedules.length > 0 && bookingRoom && bookingTimeSlot) {
		roomBookings.push({
			id: 9001,
			room: bookingRoom,
			timeSlot: bookingTimeSlot,
			semester,
			bookingDate: '2026-03-30',
			createdAt: '2026-03-30T11:00:00Z',
			participantCount: 2,
			viewerCanSeeStudentDetails: true,
			viewerIsOwner: true,
			viewerIsParticipant: true,
			bookedBy: {
				id: owner.id,
				fullName: `${owner.firstName} ${owner.lastName}`,
				email: owner.email,
			},
			participants: [
				{
					id: invited.id,
					fullName: `${invited.firstName} ${invited.lastName}`,
					email: invited.email,
				},
			],
		})
	}

	return {
		students,
		semester,
		roomBookings,
		plans: {
			101: {
				enrolledScheduleIds: state.schedules[0] ? [state.schedules[0].id] : [],
				waitlistedScheduleIds: state.schedules[1] ? [state.schedules[1].id] : [],
			},
			102: {
				enrolledScheduleIds: state.schedules[1] ? [state.schedules[1].id] : [],
				waitlistedScheduleIds: [],
			},
			103: {
				enrolledScheduleIds: state.schedules[2] ? [state.schedules[2].id] : [],
				waitlistedScheduleIds: [],
			},
		},
	}
}

function buildStudentScheduleResponse(
	state: MockState,
	fixtures: MockStudentFixtures,
	studentId: number,
	semester: string
) {
	const plan = fixtures.plans[studentId] ?? { enrolledScheduleIds: [], waitlistedScheduleIds: [] }
	const scheduleById = new Map(state.schedules.map(schedule => [schedule.id, schedule]))
	const toEnrollment = (scheduleId: number, status: 'ENROLLED' | 'WAITLISTED', index: number) => ({
		id: index + 1,
		semester,
		status,
		student: null,
		schedule: scheduleById.get(scheduleId) ?? null,
	})

	return {
		studentId,
		semester,
		enrolled: plan.enrolledScheduleIds
			.map((scheduleId, index) => toEnrollment(scheduleId, 'ENROLLED', index))
			.filter(item => item.schedule !== null),
		waitlisted: plan.waitlistedScheduleIds
			.map((scheduleId, index) => toEnrollment(scheduleId, 'WAITLISTED', index + plan.enrolledScheduleIds.length))
			.filter(item => item.schedule !== null),
	}
}

function buildStudentEnrollmentSummaries(fixtures: MockStudentFixtures, studentId: number) {
	const plan = fixtures.plans[studentId] ?? { enrolledScheduleIds: [], waitlistedScheduleIds: [] }
	return [
		...plan.enrolledScheduleIds.map((_, index) => ({
			id: index + 1,
			semester: fixtures.semester,
			status: 'ENROLLED' as const,
			student: null,
			schedule: null,
		})),
		...plan.waitlistedScheduleIds.map((_, index) => ({
			id: index + 1 + plan.enrolledScheduleIds.length,
			semester: fixtures.semester,
			status: 'WAITLISTED' as const,
			student: null,
			schedule: null,
		})),
	]
}

function filterSchedulesByQuery(
	schedules: MockState['schedules'],
	searchParams: URLSearchParams
): MockState['schedules'] {
	let output = schedules

	const roomId = Number(searchParams.get('roomId') || 0)
	const courseId = Number(searchParams.get('courseId') || 0)
	const instructorId = Number(searchParams.get('instructorId') || 0)
	const semester = searchParams.get('semester')

	if (roomId) output = output.filter(item => item.room.id === roomId)
	if (courseId) output = output.filter(item => item.course.id === courseId)
	if (instructorId) output = output.filter(item => item.course.instructor?.id === instructorId)
	if (semester) output = output.filter(item => item.semester === semester)

	return output
}

function filterChangeRequestsByQuery(
	requests: MockState['changeRequests'],
	searchParams: URLSearchParams
): MockState['changeRequests'] {
	let output = requests

	const status = searchParams.get('status')
	const instructorId = Number(searchParams.get('instructorId') || 0)
	const semester = searchParams.get('semester')
	const scheduleId = Number(searchParams.get('scheduleId') || 0)

	if (status) output = output.filter(item => item.status === status)
	if (instructorId) output = output.filter(item => item.requestedByInstructor.id === instructorId)
	if (semester) output = output.filter(item => item.schedule.semester === semester)
	if (scheduleId) output = output.filter(item => item.schedule.id === scheduleId)

	return output
}

function buildAnalytics(state: MockState) {
	const rooms = state.rooms.map((room, index) => ({
		roomId: room.id,
		roomNumber: room.roomNumber,
		buildingName: room.buildingName || 'Unknown',
		buildingCode: room.buildingCode || 'UNK',
		capacity: room.capacity,
		scheduledSlots: index + 3,
		totalSlots: 20,
		utilizationPercentage: Math.round(((index + 3) / 20) * 1000) / 10,
	}))

	const buildings = state.buildings.map((building, index) => ({
		buildingId: building.id,
		buildingName: building.name,
		buildingCode: building.code,
		roomCount: state.rooms.filter(room => room.buildingId === building.id).length,
		scheduledSlots: (index + 3) * 4,
		totalSlots: 40,
		utilizationPercentage: Math.round((((index + 3) * 4) / 40) * 1000) / 10,
	}))

	const peakHours = state.timeslots.map((slot, index) => ({
		timeSlotId: slot.id,
		dayOfWeek: slot.dayOfWeek,
		startTime: slot.startTime,
		endTime: slot.endTime,
		label: slot.label || `Slot ${slot.id}`,
		bookingCount: index + 2,
	}))

	const summary = {
		semester: 'Fall 2026',
		totalRooms: state.rooms.length,
		totalBuildings: state.buildings.length,
		totalScheduledSlots: 15,
		totalAvailableSlots: 40,
		overallUtilizationPercentage: 37.5,
		topUtilizedRooms: rooms.slice(0, 2),
		leastUtilizedRooms: [...rooms].reverse().slice(0, 2),
	}

	return {
		rooms,
		buildings,
		peakHours,
		summary,
	}
}

type MockFrictionType =
	| 'LARGE_GAP'
	| 'TIGHT_BUILDING_HOP'
	| 'OUTSIDE_PREFERRED_WINDOW'
	| 'ROOM_FEATURE_MISMATCH'
	| 'NON_PREFERRED_BUILDING'

type MockFrictionSeverity = 'LOW' | 'MEDIUM' | 'HIGH'

type MockRecommendedIssue =
	| 'GAP_TOO_LARGE_BEFORE'
	| 'GAP_TOO_LARGE_AFTER'
	| 'TIME_OF_DAY_PREFERENCE'
	| 'BACK_TO_BACK_TRAVEL'
	| 'ROOM_EQUIPMENT_MISMATCH'
	| 'OTHER'

interface MockFrictionIssue {
	id: string
	type: MockFrictionType
	severity: MockFrictionSeverity
	scheduleId: number
	message: string
	recommendedIssue: MockRecommendedIssue
}

function buildFrictionIssues(state: MockState, scenario: A11yScenario): MockFrictionIssue[] {
	if (scenario === 'empty' || state.schedules.length === 0) {
		return []
	}

	const scheduleAt = (index: number) => state.schedules[index] ?? state.schedules[0]

	const issues: MockFrictionIssue[] = [
		{
			id: 'friction-low-gap',
			type: 'LARGE_GAP',
			severity: 'LOW',
			scheduleId: scheduleAt(0).id,
			message: 'There is a large gap before this class compared to your preferred pacing.',
			recommendedIssue: 'GAP_TOO_LARGE_BEFORE',
		},
		{
			id: 'friction-medium-hop',
			type: 'TIGHT_BUILDING_HOP',
			severity: 'MEDIUM',
			scheduleId: scheduleAt(1).id,
			message: 'Travel time between adjacent classes is tight for this room transition.',
			recommendedIssue: 'BACK_TO_BACK_TRAVEL',
		},
	]

	if (scenario !== 'dense') {
		return issues
	}

	issues.push(
		{
			id: 'friction-medium-window',
			type: 'OUTSIDE_PREFERRED_WINDOW',
			severity: 'MEDIUM',
			scheduleId: scheduleAt(2).id,
			message: 'Class starts outside your preferred teaching window.',
			recommendedIssue: 'TIME_OF_DAY_PREFERENCE',
		},
		{
			id: 'friction-low-building',
			type: 'NON_PREFERRED_BUILDING',
			severity: 'LOW',
			scheduleId: scheduleAt(3).id,
			message: 'Assigned building is outside your preferred building list.',
			recommendedIssue: 'OTHER',
		},
		{
			id: 'friction-high-room',
			type: 'ROOM_FEATURE_MISMATCH',
			severity: 'HIGH',
			scheduleId: scheduleAt(4).id,
			message: 'Assigned room is missing one or more required instructional features.',
			recommendedIssue: 'ROOM_EQUIPMENT_MISMATCH',
		}
	)

	return issues
}

function getErrorScenarioResponse(
	scenario: A11yScenario,
	pathname: string,
	method: string
): { status: number; payload: unknown } | null {
	if (scenario !== 'error') {
		return null
	}

	if (pathname === '/instructor-insights/frictions' && method === 'GET') {
		return {
			status: 500,
			payload: {
				error: 'INSIGHTS_UNAVAILABLE',
				message: 'Instructor insights are temporarily unavailable in error scenario mode.',
			},
		}
	}

	if (pathname === '/analytics/summary' && method === 'GET') {
		return {
			status: 500,
			payload: {
				error: 'ANALYTICS_SUMMARY_FAILURE',
				message: 'Analytics summary failed in error scenario mode.',
			},
		}
	}

	if (pathname === '/change-requests/validate' && method === 'POST') {
		return {
			status: 422,
			payload: {
				error: 'VALIDATION_FAILED',
				message: 'Unable to validate this change request in error scenario mode.',
				fieldErrors: {
					proposedTimeSlotId: 'Selected time slot is unavailable.',
				},
			},
		}
	}

	return null
}

function recordGap(
	mockGaps: A11yMockGap[],
	options: MockInstallOptions,
	method: string,
	url: string,
	reason: string
): void {
	mockGaps.push({
		method,
		url,
		reason,
		route: options.route,
		role: options.role,
		theme: options.theme,
		scenario: options.scenario,
	})
}

export async function installA11yMockApi(
	page: Page,
	options: MockInstallOptions
): Promise<{ mockGaps: A11yMockGap[] }> {
	const state = createState(options.scenario)
	const studentFixtures = buildStudentFixtures(state)
	const mockGaps: A11yMockGap[] = []

	await page.route('**/ws/info**', async route => {
		await asJson(route, {
			websocket: false,
			cookie_needed: false,
			origins: ['*:*'],
			entropy: 1,
		})
	})

	await page.route('**/api/**', async route => {
		const request = route.request()
		const url = new URL(request.url())
		const method = request.method().toUpperCase()
		const pathname = url.pathname.replace(/^\/api/, '') || '/'
		const analytics = buildAnalytics(state)
		const scenarioError = getErrorScenarioResponse(options.scenario, pathname, method)

		if (scenarioError) {
			await asJson(route, scenarioError.payload, scenarioError.status)
			return
		}

		if (pathname === '/buildings' && method === 'GET') {
			await asJson(route, state.buildings)
			return
		}

		if (pathname === '/buildings' && method === 'POST') {
			const payload = request.postDataJSON() as Partial<MockState['buildings'][number]> | null
			const next = {
				id: nextId(state.buildings),
				code: payload?.code || 'NEW',
				name: payload?.name || 'New Building',
				address: payload?.address || null,
			}
			state.buildings.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/buildings\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const building = state.buildings.find(item => item.id === id)
			if (!building) {
				await asJson(route, { message: 'Building not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, building)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(building, payload)
				await asJson(route, building)
				return
			}

			if (method === 'DELETE') {
				state.buildings = state.buildings.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/rooms' && method === 'GET') {
			const buildingId = Number(url.searchParams.get('buildingId') || 0)
			const rooms = buildingId ? state.rooms.filter(room => room.buildingId === buildingId) : state.rooms
			await asJson(route, rooms)
			return
		}

		if (/^\/rooms\/building\/\d+$/.test(pathname) && method === 'POST') {
			const buildingId = getNumericPathId(pathname)
			const building = state.buildings.find(item => item.id === buildingId)
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: nextId(state.rooms),
				roomNumber: String(payload.roomNumber || '999'),
				capacity: Number(payload.capacity || 10),
				type: String(payload.type || 'CLASSROOM'),
				features: (payload.features as string | undefined) ?? null,
				buildingId: building?.id ?? null,
				buildingCode: building?.code ?? null,
				buildingName: building?.name ?? null,
			}
			state.rooms.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/rooms\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const room = state.rooms.find(item => item.id === id)
			if (!room) {
				await asJson(route, { message: 'Room not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, room)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(room, payload)
				await asJson(route, room)
				return
			}

			if (method === 'DELETE') {
				state.rooms = state.rooms.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/instructors' && method === 'GET') {
			const department = url.searchParams.get('department')
			const instructors = department
				? state.instructors.filter(item => item.department === department)
				: state.instructors
			await asJson(route, instructors)
			return
		}

		if (pathname === '/instructors' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: nextId(state.instructors),
				firstName: String(payload.firstName || 'New'),
				lastName: String(payload.lastName || 'Instructor'),
				email: String(payload.email || 'new@campus.edu'),
				department: (payload.department as string | undefined) ?? null,
				officeNumber: (payload.officeNumber as string | undefined) ?? null,
			}
			state.instructors.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/instructors\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const instructor = state.instructors.find(item => item.id === id) ?? state.instructors[0]
			if (!instructor) {
				await asJson(route, { message: 'No instructors available' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, instructor)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(instructor, payload)
				await asJson(route, instructor)
				return
			}

			if (method === 'DELETE') {
				state.instructors = state.instructors.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/courses' && method === 'GET') {
			const department = url.searchParams.get('department')
			const instructorId = Number(url.searchParams.get('instructorId') || 0)

			const courses = state.courses.filter((course) => {
				if (department && course.department !== department) return false
				if (instructorId && course.instructor?.id !== instructorId) return false
				return true
			})

			await asJson(route, courses)
			return
		}

		if (pathname === '/courses' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: nextId(state.courses),
				code: String(payload.code || 'NEW101'),
				name: String(payload.name || 'New Course'),
				description: (payload.description as string | undefined) ?? null,
				credits: Number(payload.credits || 3),
				enrollmentCapacity: Number(payload.enrollmentCapacity || 20),
				department: (payload.department as string | undefined) ?? null,
				instructor: null,
			}
			state.courses.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/courses\/code\/.+/.test(pathname) && method === 'GET') {
			const code = decodeURIComponent(pathname.split('/').pop() || '')
			const course = state.courses.find(item => item.code === code)
			if (!course) {
				await asJson(route, { message: 'Course not found' }, 404)
				return
			}
			await asJson(route, course)
			return
		}

		if (/^\/courses\/instructor\/\d+$/.test(pathname) && method === 'GET') {
			const instructorId = getNumericPathId(pathname)
			const courses = state.courses.filter(item => item.instructor?.id === instructorId)
			await asJson(route, courses)
			return
		}

		if (/^\/courses\/\d+\/instructor\/\d+$/.test(pathname) && method === 'PUT') {
			const ids = pathname.match(/^\/courses\/(\d+)\/instructor\/(\d+)$/)
			const courseId = Number(ids?.[1] || 0)
			const instructorId = Number(ids?.[2] || 0)
			const course = state.courses.find(item => item.id === courseId)
			const instructor = state.instructors.find(item => item.id === instructorId)
			if (!course || !instructor) {
				await asJson(route, { message: 'Not found' }, 404)
				return
			}
			course.instructor = {
				id: instructor.id,
				firstName: instructor.firstName,
				lastName: instructor.lastName,
				email: instructor.email,
			}
			await asJson(route, course)
			return
		}

		if (/^\/courses\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const course = state.courses.find(item => item.id === id) ?? state.courses[0]
			if (!course) {
				await asJson(route, { message: 'No courses available' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, course)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(course, payload)
				await asJson(route, course)
				return
			}

			if (method === 'DELETE') {
				state.courses = state.courses.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/timeslots' && method === 'GET') {
			const dayOfWeek = url.searchParams.get('dayOfWeek')
			const timeSlots = dayOfWeek ? state.timeslots.filter(item => item.dayOfWeek === dayOfWeek) : state.timeslots
			await asJson(route, timeSlots)
			return
		}

		if (pathname === '/timeslots' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: nextId(state.timeslots),
				dayOfWeek: String(payload.dayOfWeek || 'MONDAY'),
				startTime: String(payload.startTime || '08:00'),
				endTime: String(payload.endTime || '09:00'),
				label: (payload.label as string | undefined) ?? null,
			}
			state.timeslots.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/timeslots\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const slot = state.timeslots.find(item => item.id === id)
			if (!slot) {
				await asJson(route, { message: 'Time slot not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, slot)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(slot, payload)
				await asJson(route, slot)
				return
			}

			if (method === 'DELETE') {
				state.timeslots = state.timeslots.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/students' && method === 'GET') {
			await asJson(route, studentFixtures.students)
			return
		}

		if (/^\/students\/\d+\/schedule$/.test(pathname) && method === 'GET') {
			const studentId = getNumericPathId(pathname) ?? 101
			const semester = url.searchParams.get('semester') || studentFixtures.semester
			await asJson(route, buildStudentScheduleResponse(state, studentFixtures, studentId, semester))
			return
		}

		if (/^\/students\/\d+$/.test(pathname) && method === 'GET') {
			const studentId = getNumericPathId(pathname) ?? 101
			const student = studentFixtures.students.find(item => item.id === studentId) ?? studentFixtures.students[0]
			await asJson(route, student)
			return
		}

		if (pathname === '/enrollments' && method === 'GET') {
			const studentId = Number(url.searchParams.get('studentId') || 101)
			await asJson(route, buildStudentEnrollmentSummaries(studentFixtures, studentId))
			return
		}

		if (pathname === '/schedules' && method === 'GET') {
			await asJson(route, filterSchedulesByQuery(state.schedules, url.searchParams))
			return
		}

		if (pathname === '/room-bookings/student-search' && method === 'GET') {
			const query = (url.searchParams.get('query') || '').trim().toLowerCase()
			const timeSlotId = Number(url.searchParams.get('timeSlotId') || 0)
			const excludeStudentIds = new Set(
				url.searchParams.getAll('excludeStudentId').map(value => Number(value)).filter(Number.isFinite)
			)
			const results = studentFixtures.students
				.filter(student => !excludeStudentIds.has(student.id))
				.filter(student => {
					if (!query) return true
					return `${student.firstName} ${student.lastName}`.toLowerCase().includes(query)
						|| student.email.toLowerCase().includes(query)
				})
				.map(student => {
					const scheduleResponse = buildStudentScheduleResponse(state, studentFixtures, student.id, studentFixtures.semester)
					const hasClassDuringPeriod = [...scheduleResponse.enrolled, ...scheduleResponse.waitlisted]
						.some(item => item.schedule?.timeSlot.id === timeSlotId)

					return {
						id: student.id,
						email: student.email,
						fullName: `${student.firstName} ${student.lastName}`,
						hasClassDuringPeriod,
					}
				})
			await asJson(route, results)
			return
		}

		if (pathname === '/room-bookings' && method === 'GET') {
			const semester = url.searchParams.get('semester')
			const bookings = semester
				? studentFixtures.roomBookings.filter(booking => booking.semester === semester)
				: studentFixtures.roomBookings
			await asJson(route, bookings)
			return
		}

		if (pathname === '/room-bookings' && method === 'POST') {
			const payload = request.postDataJSON() as {
				studentId?: number
				roomId?: number
				timeSlotId?: number
				semester?: string
				bookingDate?: string
				participantEmails?: string[]
			} | null
			const student = studentFixtures.students.find(item => item.id === Number(payload?.studentId || 0)) ?? studentFixtures.students[0]
			const room = state.rooms.find(item => item.id === Number(payload?.roomId || 0)) ?? state.rooms[0]
			const timeSlot = state.timeslots.find(item => item.id === Number(payload?.timeSlotId || 0)) ?? state.timeslots[0]
			const participants = studentFixtures.students
				.filter(item => payload?.participantEmails?.includes(item.email))
				.map(item => ({
					id: item.id,
					fullName: `${item.firstName} ${item.lastName}`,
					email: item.email,
				}))

			const booking: MockRoomBooking = {
				id: nextId(studentFixtures.roomBookings),
				room,
				timeSlot,
				semester: payload?.semester || studentFixtures.semester,
				bookingDate: payload?.bookingDate || '2026-03-30',
				createdAt: new Date().toISOString(),
				participantCount: participants.length + 1,
				viewerCanSeeStudentDetails: true,
				viewerIsOwner: true,
				viewerIsParticipant: true,
				bookedBy: {
					id: student.id,
					fullName: `${student.firstName} ${student.lastName}`,
					email: student.email,
				},
				participants,
			}
			studentFixtures.roomBookings.push(booking)
			await asJson(route, booking, 201)
			return
		}

		if (pathname === '/schedules' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const course = state.courses.find(item => item.id === Number(payload.courseId || 0)) ?? state.courses[0]
			const room = state.rooms.find(item => item.id === Number(payload.roomId || 0)) ?? state.rooms[0]
			const timeSlot = state.timeslots.find(item => item.id === Number(payload.timeSlotId || 0)) ?? state.timeslots[0]
			const next = {
				id: nextId(state.schedules),
				course,
				room,
				timeSlot,
				semester: String(payload.semester || 'Fall 2026'),
			}
			state.schedules.push(next)
			await asJson(route, next, 201)
			return
		}

		if (pathname.startsWith('/schedules/conflicts') && method === 'GET') {
			await asJson(route, { hasConflict: false })
			return
		}

		if (/^\/schedules\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const schedule = state.schedules.find(item => item.id === id)
			if (!schedule) {
				await asJson(route, { message: 'Schedule not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, schedule)
				return
			}

			if (method === 'DELETE') {
				state.schedules = state.schedules.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/change-requests' && method === 'GET') {
			await asJson(route, filterChangeRequestsByQuery(state.changeRequests, url.searchParams))
			return
		}

		if (pathname === '/change-requests' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const schedule = state.schedules.find(item => item.id === Number(payload.scheduleId || 0)) ?? state.schedules[0]
			const instructor =
				state.instructors.find(item => item.id === Number(payload.requestedByInstructorId || 0)) ?? state.instructors[0]
			const proposedRoom = state.rooms.find(item => item.id === Number(payload.proposedRoomId || 0)) ?? null
			const proposedTimeSlot = state.timeslots.find(item => item.id === Number(payload.proposedTimeSlotId || 0)) ?? null

			const next = {
				id: nextId(state.changeRequests),
				schedule,
				requestedByInstructor: instructor,
				requestedByRole: String(payload.requestedByRole || 'INSTRUCTOR') as 'INSTRUCTOR' | 'ADMIN',
				status: 'PENDING' as const,
				reasonCategory: String(payload.reasonCategory || 'OTHER') as
					| 'MEDICAL'
					| 'EQUIPMENT_FAILURE'
					| 'PEDAGOGICAL_CONFLICT'
					| 'OTHER',
				reasonDetails: (payload.reasonDetails as string | undefined) ?? null,
				proposedRoom,
				proposedTimeSlot,
				originalRoomId: schedule.room.id,
				originalTimeSlotId: schedule.timeSlot.id,
				originalSemester: schedule.semester,
				decisionNote: null,
				createdAt: new Date().toISOString(),
				reviewedAt: null,
				appliedAt: null,
			}

			state.changeRequests.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/change-requests\/\d+\/(approve|reject)$/.test(pathname) && method === 'POST') {
			const ids = pathname.match(/^\/change-requests\/(\d+)\/(approve|reject)$/)
			const requestId = Number(ids?.[1] || 0)
			const action = ids?.[2] || 'approve'
			const requestItem = state.changeRequests.find(item => item.id === requestId)
			if (!requestItem) {
				await asJson(route, { message: 'Change request not found' }, 404)
				return
			}

			requestItem.status = action === 'approve' ? 'APPROVED' : 'REJECTED'
			requestItem.reviewedAt = new Date().toISOString()
			const payload = request.postDataJSON() as Record<string, unknown>
			requestItem.decisionNote = (payload.decisionNote as string | undefined) ?? null

			await asJson(route, requestItem)
			return
		}

		if (pathname === '/change-requests/validate' && method === 'POST') {
			await asJson(route, {
				green: true,
				hardConflicts: [],
				softWarnings: [],
			})
			return
		}

		if (pathname === '/instructor-preferences/room-feature-options' && method === 'GET') {
			await asJson(route, [
				{ value: 'projector', label: 'Projector', category: 'Presentation and AV', matchKeywords: ['projector'] },
				{ value: 'microphone', label: 'Microphone', category: 'Presentation and AV', matchKeywords: ['microphone', 'mic'] },
				{ value: 'whiteboard', label: 'Whiteboard', category: 'Teaching setup', matchKeywords: ['whiteboard'] },
			])
			return
		}

		if (/^\/instructor-preferences\/\d+$/.test(pathname)) {
			const instructorId = getNumericPathId(pathname)
			if (!instructorId) {
				await asJson(route, { message: 'Instructor not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, {
					instructorId,
					preferredStartTime: '08:00',
					preferredEndTime: '18:00',
					maxGapMinutes: 120,
					minTravelBufferMinutes: 15,
					avoidBuildingHops: true,
					preferredBuildingIds: [],
					requiredRoomFeatures: [],
					updatedAt: new Date().toISOString(),
				})
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				await asJson(route, {
					instructorId,
					preferredStartTime: payload.preferredStartTime ?? '08:00',
					preferredEndTime: payload.preferredEndTime ?? '18:00',
					maxGapMinutes: payload.maxGapMinutes ?? 120,
					minTravelBufferMinutes: payload.minTravelBufferMinutes ?? 15,
					avoidBuildingHops: payload.avoidBuildingHops ?? true,
					preferredBuildingIds: payload.preferredBuildingIds ?? [],
					requiredRoomFeatures: payload.requiredRoomFeatures ?? [],
					updatedAt: new Date().toISOString(),
				})
				return
			}
		}

		if (pathname === '/instructor-insights/frictions' && method === 'GET') {
			await asJson(route, buildFrictionIssues(state, options.scenario))
			return
		}

		if (pathname === '/analytics/rooms' && method === 'GET') {
			await asJson(route, analytics.rooms)
			return
		}

		if (/^\/analytics\/rooms\/\d+$/.test(pathname) && method === 'GET') {
			const id = getNumericPathId(pathname)
			const room = analytics.rooms.find(item => item.roomId === id)
			await asJson(route, room ?? analytics.rooms[0])
			return
		}

		if (pathname === '/analytics/buildings' && method === 'GET') {
			await asJson(route, analytics.buildings)
			return
		}

		if (/^\/analytics\/buildings\/\d+$/.test(pathname) && method === 'GET') {
			const id = getNumericPathId(pathname)
			const building = analytics.buildings.find(item => item.buildingId === id)
			await asJson(route, building ?? analytics.buildings[0])
			return
		}

		if (pathname === '/analytics/peak-hours' && method === 'GET') {
			await asJson(route, analytics.peakHours)
			return
		}

		if (pathname === '/analytics/underused' && method === 'GET') {
			const threshold = Number(url.searchParams.get('threshold') || 30)
			await asJson(route, analytics.rooms.filter(item => item.utilizationPercentage < threshold))
			return
		}

		if (pathname === '/analytics/summary' && method === 'GET') {
			await asJson(route, analytics.summary)
			return
		}

		if (pathname === '/semesters' && method === 'GET') {
			await asJson(route, semesterDefinitions)
			return
		}

		if (pathname === '/generator/stats' && method === 'GET') {
			await asJson(route, {
				buildings: state.buildings.length,
				rooms: state.rooms.length,
				instructors: state.instructors.length,
				courses: state.courses.length,
				schedules: state.schedules.length,
			})
			return
		}

		if (pathname === '/generator/archetypes' && method === 'GET') {
			await asJson(route, [
				{
					id: 'METROPOLIS',
					displayName: 'Metropolis',
					description: 'Dense urban campus',
					studentsPerBuilding: 1000,
					coursesPerBuilding: 60,
					studentsPerCourse: 50,
					minStudents: 10000,
					maxStudents: 60000,
					academicBuildingRatio: 0.7,
					exampleUniversities: ['Metro State'],
				},
				{
					id: 'CAMPUS_SPRAWL',
					displayName: 'Campus Sprawl',
					description: 'Large suburban campus',
					studentsPerBuilding: 700,
					coursesPerBuilding: 45,
					studentsPerCourse: 40,
					minStudents: 5000,
					maxStudents: 30000,
					academicBuildingRatio: 0.6,
					exampleUniversities: ['Suburban Tech'],
				},
				{
					id: 'COMMUNITY',
					displayName: 'Community',
					description: 'Compact campus',
					studentsPerBuilding: 450,
					coursesPerBuilding: 35,
					studentsPerCourse: 35,
					minStudents: 1000,
					maxStudents: 12000,
					academicBuildingRatio: 0.5,
					exampleUniversities: ['Community College'],
				},
			])
			return
		}

		if (pathname === '/generator/preview' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const studentPopulation = Number(payload.studentPopulation || 8000)
			await asJson(route, {
				archetype: String(payload.archetype || 'COMMUNITY'),
				archetypeDisplayName: 'Community',
				studentPopulation,
				totalBuildings: 12,
				academicBuildings: 7,
				roomsPerBuilding: 20,
				instructors: 120,
				courses: 220,
				totalRooms: 240,
				ratioInfo: 'Mock preview data',
			})
			return
		}

		if (pathname.startsWith('/generator/university') && method === 'POST') {
			await asJson(route, {
				buildings: state.buildings.length,
				rooms: state.rooms.length,
				instructors: state.instructors.length,
				courses: state.courses.length,
				timeSlots: state.timeslots.length,
				archetype: 'COMMUNITY',
				studentPopulation: 8000,
			})
			return
		}

		if (pathname === '/generator/reset' && method === 'DELETE') {
			await route.fulfill({ status: 204 })
			return
		}

		if (pathname.startsWith('/solver/start') && method === 'POST') {
			await asJson(route, {
				problemId: 1,
				message: 'Solver started',
			})
			return
		}

		if (pathname === '/solver/stop' && method === 'POST') {
			await asJson(route, 'Solver stopped')
			return
		}

		if (pathname === '/solver/status' && method === 'GET') {
			await asJson(route, {
				status: 'NOT_SOLVING',
				score: '0hard/-50soft',
				assignedCourses: 1,
				totalCourses: 2,
				hardViolations: 0,
				softScore: -50,
			})
			return
		}

		if (pathname === '/solver/save' && method === 'POST') {
			await asJson(route, {
				savedCount: state.schedules.length,
				message: 'Saved',
			})
			return
		}

		if (pathname === '/solver/analytics' && method === 'GET') {
			await asJson(route, {
				semester: 'Fall 2026',
				totalRooms: state.rooms.length,
				totalBuildings: state.buildings.length,
				totalScheduledSlots: 15,
				totalAvailableSlots: 40,
				overallUtilizationPercentage: 37.5,
				topUtilizedRooms: analytics.rooms.slice(0, 2),
				leastUtilizedRooms: [...analytics.rooms].reverse().slice(0, 2),
				rooms: analytics.rooms,
				buildings: analytics.buildings,
				peakHours: analytics.peakHours,
			})
			return
		}

		if (pathname === '/solver/impact' && method === 'POST') {
			await asJson(route, {
				status: 'SOLVED',
				score: '0hard/-20soft',
				scoreSummary: 'Mock impact analysis',
				moves: [],
				constraintSummaries: [],
			})
			return
		}

		recordGap(mockGaps, options, method, url.toString(), 'No mock handler matched this request')
		const fallback = method === 'GET' ? [] : { ok: true }
		await asJson(route, fallback)
	})

	page.on('pageerror', error => {
		recordGap(mockGaps, options, 'PAGE', page.url(), `Page error: ${error.message}`)
	})

	return { mockGaps }
}
