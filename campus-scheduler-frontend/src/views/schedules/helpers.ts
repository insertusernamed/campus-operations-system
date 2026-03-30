import type { EnrollmentStatus } from '@/services/enrollments'
import { DEFAULT_SEMESTER_DEFINITIONS } from '@/services/semesters'
import type { RoomBooking } from '@/services/roomBookings'
import type { Room } from '@/services/rooms'
import type { Schedule } from '@/services/schedules'
import type { DayOfWeek, TimeSlot } from '@/services/timeslots'
import { parseSemesterLabel, resolveSemesterDateRange } from '@/utils/semester'

const DAY_ORDER: Record<DayOfWeek, number> = {
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
	SUNDAY: 7,
}

const JAVASCRIPT_DAY_ORDER: Record<DayOfWeek, number> = {
	SUNDAY: 0,
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
}

const BOOKING_WINDOW_DAYS = 21

export interface BookingOccurrenceOption {
	key: string
	bookingDate: string
	timeSlot: TimeSlot
}

export function formatRoom(room: Room): string {
	const parts = [room.buildingCode, room.roomNumber].filter(Boolean)
	return parts.join(' ') || 'Room unavailable'
}

function parseSemesterForSort(semester: string): { year: number; termRank: number } {
	const match = semester.match(/([A-Za-z]+)\s+(\d{4})/)
	if (!match) {
		return { year: 0, termRank: 0 }
	}

	const term = match[1]?.toUpperCase() ?? ''
	const year = Number(match[2] ?? 0)
	const termOrder: Record<string, number> = {
		WINTER: 1,
		SPRING: 2,
		SUMMER: 3,
		FALL: 4,
	}

	return {
		year: Number.isNaN(year) ? 0 : year,
		termRank: termOrder[term] ?? 0,
	}
}

export function sortSemesters(semesters: string[]): string[] {
	return semesters
		.filter(semester => semester.trim().length > 0)
		.sort((a, b) => {
			const parsedA = parseSemesterForSort(a)
			const parsedB = parseSemesterForSort(b)
			if (parsedA.year !== parsedB.year) {
				return parsedB.year - parsedA.year
			}
			if (parsedA.termRank !== parsedB.termRank) {
				return parsedB.termRank - parsedA.termRank
			}
			return a.localeCompare(b)
		})
}

export function compareByTimeSlot(a: { timeSlot: TimeSlot }, b: { timeSlot: TimeSlot }): number {
	const dayA = DAY_ORDER[a.timeSlot.dayOfWeek] ?? Number.MAX_SAFE_INTEGER
	const dayB = DAY_ORDER[b.timeSlot.dayOfWeek] ?? Number.MAX_SAFE_INTEGER
	if (dayA !== dayB) {
		return dayA - dayB
	}
	if (a.timeSlot.startTime !== b.timeSlot.startTime) {
		return a.timeSlot.startTime.localeCompare(b.timeSlot.startTime)
	}
	if (a.timeSlot.endTime !== b.timeSlot.endTime) {
		return a.timeSlot.endTime.localeCompare(b.timeSlot.endTime)
	}
	return 0
}

export function compareTimeSlots(a: TimeSlot, b: TimeSlot): number {
	return compareByTimeSlot({ timeSlot: a }, { timeSlot: b })
}

export function sortSchedulesByTime(schedules: Schedule[]): Schedule[] {
	return [...schedules].sort((a, b) => {
		const timeComparison = compareByTimeSlot(a, b)
		if (timeComparison !== 0) {
			return timeComparison
		}
		return a.course.code.localeCompare(b.course.code)
	})
}

export function sortRoomBookingsByTime(bookings: RoomBooking[]): RoomBooking[] {
	return [...bookings].sort((a, b) => {
		const bookingDateA = a.bookingDate ?? ''
		const bookingDateB = b.bookingDate ?? ''
		if (bookingDateA !== bookingDateB) {
			if (!bookingDateA) return 1
			if (!bookingDateB) return -1
			return bookingDateA.localeCompare(bookingDateB)
		}

		const semesterComparison = a.semester.localeCompare(b.semester)
		if (semesterComparison !== 0) {
			return semesterComparison
		}

		const timeComparison = compareByTimeSlot(a, b)
		if (timeComparison !== 0) {
			return timeComparison
		}

		const buildingA = a.room.buildingCode ?? ''
		const buildingB = b.room.buildingCode ?? ''
		if (buildingA !== buildingB) {
			return buildingA.localeCompare(buildingB)
		}

		if (a.room.roomNumber !== b.room.roomNumber) {
			return a.room.roomNumber.localeCompare(b.room.roomNumber)
		}

		return a.id - b.id
	})
}

export function getInstructorName(schedule: Schedule): string {
	const instructor = schedule.course.instructor
	if (!instructor) {
		return 'Unassigned'
	}
	return `${instructor.firstName} ${instructor.lastName}`
}

export function getSeatUtilization(schedule: Schedule): string {
	if (typeof schedule.filledSeats !== 'number' || typeof schedule.seatLimit !== 'number') {
		return 'Seat data unavailable'
	}
	if (schedule.seatLimit <= 0) {
		return `${schedule.filledSeats} enrolled`
	}
	const percent = Math.round((schedule.filledSeats / schedule.seatLimit) * 100)
	return `${schedule.filledSeats}/${schedule.seatLimit} seats (${percent}%)`
}

export function getSeatPressure(schedule: Schedule): string {
	if (typeof schedule.remainingSeats !== 'number' || typeof schedule.waitlistCount !== 'number') {
		return 'Seat pressure unavailable'
	}
	return `${schedule.remainingSeats} seats left, ${schedule.waitlistCount} waitlisted`
}

export function getWaitlistSummary(schedule: Schedule): string {
	if (typeof schedule.waitlistCount !== 'number') {
		return 'N/A'
	}
	return `${schedule.waitlistCount}`
}

export function getStudentStatusLabel(status: EnrollmentStatus | null): string {
	if (status === 'ENROLLED') return 'Enrolled'
	if (status === 'WAITLISTED') return 'Waitlisted'
	return 'Unknown'
}

export function getParticipantTotalLabel(count: number): string {
	return count === 1 ? '1 student' : `${count} students`
}

function normalizeCalendarDate(date: Date): Date {
	return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 12, 0, 0, 0)
}

function parseIsoBookingDate(bookingDate: string): Date {
	const [rawYear = '0', rawMonth = '1', rawDay = '1'] = bookingDate.split('-')
	return normalizeCalendarDate(
		new Date(Number(rawYear), Number(rawMonth) - 1, Number(rawDay))
	)
}

function toIsoBookingDate(date: Date): string {
	const year = date.getFullYear()
	const month = String(date.getMonth() + 1).padStart(2, '0')
	const day = String(date.getDate()).padStart(2, '0')
	return `${year}-${month}-${day}`
}

function getNextOccurrenceOnOrAfter(baseDate: Date, dayOfWeek: DayOfWeek): Date {
	const normalizedBase = normalizeCalendarDate(baseDate)
	const diff = (JAVASCRIPT_DAY_ORDER[dayOfWeek] - normalizedBase.getDay() + 7) % 7
	const result = new Date(normalizedBase)
	result.setDate(result.getDate() + diff)
	return normalizeCalendarDate(result)
}

export function getUpcomingBookingOccurrenceOptions(
	semester: string,
	timeSlots: TimeSlot[],
	referenceDate: Date = new Date()
): BookingOccurrenceOption[] {
	if (!semester.trim()) {
		return []
	}

	const parsedSemester = parseSemesterLabel(semester, DEFAULT_SEMESTER_DEFINITIONS)
	const today = normalizeCalendarDate(referenceDate)
	const windowEnd = normalizeCalendarDate(referenceDate)
	windowEnd.setDate(windowEnd.getDate() + BOOKING_WINDOW_DAYS)

	let effectiveStart = today
	let effectiveEnd = windowEnd

	if (parsedSemester) {
		const range = resolveSemesterDateRange(parsedSemester.definition, parsedSemester.year)
		const semesterStart = normalizeCalendarDate(
			new Date(range.start.year, range.start.month - 1, range.start.day)
		)
		const semesterEnd = normalizeCalendarDate(
			new Date(range.end.year, range.end.month - 1, range.end.day)
		)
		if (semesterEnd < effectiveStart || semesterStart > effectiveEnd) {
			return []
		}
		effectiveStart = semesterStart > effectiveStart ? semesterStart : effectiveStart
		effectiveEnd = semesterEnd < effectiveEnd ? semesterEnd : effectiveEnd
	}

	const options: BookingOccurrenceOption[] = []

	for (const timeSlot of timeSlots) {
		let nextOccurrence = getNextOccurrenceOnOrAfter(effectiveStart, timeSlot.dayOfWeek)
		while (nextOccurrence <= effectiveEnd) {
			const bookingDate = toIsoBookingDate(nextOccurrence)
			options.push({
				key: `${bookingDate}-${timeSlot.id}`,
				bookingDate,
				timeSlot,
			})
			const followingWeek = new Date(nextOccurrence)
			followingWeek.setDate(followingWeek.getDate() + 7)
			nextOccurrence = normalizeCalendarDate(followingWeek)
		}
	}

	return options.sort((a, b) => {
		if (a.bookingDate !== b.bookingDate) {
			return a.bookingDate.localeCompare(b.bookingDate)
		}
		return compareTimeSlots(a.timeSlot, b.timeSlot)
	})
}

export function formatBookingDate(bookingDate: string | null): string {
	if (!bookingDate) {
		return 'Date unavailable'
	}

	return new Intl.DateTimeFormat(undefined, {
		weekday: 'short',
		month: 'short',
		day: 'numeric',
		year: 'numeric',
	}).format(parseIsoBookingDate(bookingDate))
}

export function getBookingPrivacyMessage(booking: RoomBooking): string | null {
	if (booking.viewerCanSeeStudentDetails || booking.viewerIsOwner) {
		return null
	}
	if (booking.viewerIsParticipant) {
		return 'You are included in this booking. Student details are only visible to the booking owner and admins.'
	}
	return 'Student details are hidden for this booking.'
}
