import ical, { ICalEventRepeatingFreq } from 'ical-generator'
import type { Schedule } from '@/services/schedules'
import type { DayOfWeek } from '@/services/timeslots'

// Maps our DayOfWeek enum to JS Date.getDay() values
const DAY_INDEX: Record<DayOfWeek, number> = {
	SUNDAY: 0,
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
}

/**
 * Approximate semester start/end dates based on term + year string.
 * e.g. "Fall 2025" -> { start: Sep 2 2025, end: Dec 20 2025 }
 */
function getSemesterDates(semester: string): { start: Date; end: Date } {
	const match = semester.match(/([A-Za-z]+)\s+(\d{4})/)
	const term = match?.[1]?.toUpperCase() ?? 'FALL'
	const year = Number(match?.[2] ?? new Date().getFullYear())

	const termBounds: Record<string, [number, number, number, number]> = {
		// [startMonth (1-based), startDay, endMonth (1-based), endDay]
		SPRING: [1, 15, 5, 15],
		SUMMER: [6, 1, 8, 15],
		FALL: [9, 2, 12, 20],
		WINTER: [1, 2, 3, 15],
	}

	const [sm, sd, em, ed] = termBounds[term] ?? termBounds['FALL']!
	return {
		start: new Date(year, sm - 1, sd),
		end: new Date(year, em - 1, ed),
	}
}

/** Returns the next calendar date that falls on the given day of the week (today if it matches). */
function getNextOccurrence(dayOfWeek: DayOfWeek, from?: Date): Date {
	const base = from ? new Date(from) : new Date()
	base.setHours(0, 0, 0, 0)
	const targetDay = DAY_INDEX[dayOfWeek]
	const diff = (targetDay - base.getDay() + 7) % 7
	const result = new Date(base)
	result.setDate(base.getDate() + diff)
	return result
}

/** Applies HH:mm time string to a Date, returning a new Date. */
function withTime(date: Date, time: string): Date {
	const [h = '0', m = '0'] = time.split(':')
	const result = new Date(date)
	result.setHours(Number(h), Number(m), 0, 0)
	return result
}

/** Builds a human-readable event description from a Schedule. */
function buildDescription(schedule: Schedule): string {
	const parts: string[] = [`Semester: ${schedule.semester}`]
	const inst = schedule.course.instructor
	if (inst) {
		parts.push(`Instructor: ${inst.firstName} ${inst.lastName}`)
	}
	if (schedule.course.department) {
		parts.push(`Department: ${schedule.course.department}`)
	}
	return parts.join('\n')
}

/** Triggers a browser download of an .ics file. */
function triggerDownload(content: string, filename: string): void {
	const blob = new Blob([content], { type: 'text/calendar;charset=utf-8' })
	const url = URL.createObjectURL(blob)
	const anchor = document.createElement('a')
	anchor.href = url
	anchor.download = filename
	document.body.appendChild(anchor)
	anchor.click()
	document.body.removeChild(anchor)
	URL.revokeObjectURL(url)
}

/**
 * Exports a single one-time event for the next upcoming occurrence of a class.
 */
export function exportSingleClass(schedule: Schedule): void {
	const cal = ical({ name: 'Class Event' })

	const nextDate = getNextOccurrence(schedule.timeSlot.dayOfWeek)

	cal.createEvent({
		start: withTime(nextDate, schedule.timeSlot.startTime),
		end: withTime(nextDate, schedule.timeSlot.endTime),
		summary: `${schedule.course.code} – ${schedule.course.name}`,
		location: `${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
		description: buildDescription(schedule),
	})

	triggerDownload(
		cal.toString(),
		`${schedule.course.code.replace(/\s+/g, '-')}-next-class.ics`,
	)
}

/**
 * Exports weekly recurring events for a single class from today (or semester
 * start, whichever is later) through the end of the semester.
 */
export function exportClassForSemester(schedule: Schedule): void {
	const cal = ical({ name: `${schedule.course.code} – ${schedule.semester}` })
	const { start: semStart, end: semEnd } = getSemesterDates(schedule.semester)

	const today = new Date()
	today.setHours(0, 0, 0, 0)

	// Start from the first occurrence on-or-after the later of (semStart, today)
	const baseDate = semStart >= today ? semStart : today
	const firstDate = getNextOccurrence(schedule.timeSlot.dayOfWeek, baseDate)

	cal.createEvent({
		start: withTime(firstDate, schedule.timeSlot.startTime),
		end: withTime(firstDate, schedule.timeSlot.endTime),
		summary: `${schedule.course.code} – ${schedule.course.name}`,
		location: `${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
		description: buildDescription(schedule),
		repeating: {
			freq: ICalEventRepeatingFreq.WEEKLY,
			until: semEnd,
		},
	})

	triggerDownload(
		cal.toString(),
		`${schedule.course.code.replace(/\s+/g, '-')}-${schedule.semester.replace(/\s+/g, '-')}.ics`,
	)
}

/**
 * Exports the full semester schedule (all assigned classes) as a calendar file
 * with weekly recurring events for each class.
 */
export function exportFullSemester(schedules: Schedule[]): void {
	if (schedules.length === 0) return

	// Group schedules by semester for calendar name
	const semesters = [...new Set(schedules.map(s => s.semester))]
	const calName =
		semesters.length === 1
			? `My Schedule – ${semesters[0]}`
			: 'My Class Schedule'

	const cal = ical({ name: calName })

	const today = new Date()
	today.setHours(0, 0, 0, 0)

	for (const schedule of schedules) {
		const { start: semStart, end: semEnd } = getSemesterDates(schedule.semester)
		const baseDate = semStart >= today ? semStart : today
		const firstDate = getNextOccurrence(schedule.timeSlot.dayOfWeek, baseDate)

		cal.createEvent({
			start: withTime(firstDate, schedule.timeSlot.startTime),
			end: withTime(firstDate, schedule.timeSlot.endTime),
			summary: `${schedule.course.code} – ${schedule.course.name}`,
			location: `${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
			description: buildDescription(schedule),
			repeating: {
				freq: ICalEventRepeatingFreq.WEEKLY,
				until: semEnd,
			},
		})
	}

	const filenameSuffix =
		semesters.length === 1
			? semesters[0]!.replace(/\s+/g, '-')
			: 'all-semesters'

	triggerDownload(cal.toString(), `my-schedule-${filenameSuffix}.ics`)
}
