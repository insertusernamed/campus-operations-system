import type { Room } from '@/services/rooms'
import type { RoomBooking } from '@/services/roomBookings'
import type { Schedule } from '@/services/schedules'
import type { TimeSlot } from '@/services/timeslots'

export type CalendarEntryKind = 'schedule' | 'roomBooking'

export interface CalendarSelection {
	kind: CalendarEntryKind
	sourceId: number
}

interface BaseCalendarEntry {
	kind: CalendarEntryKind
	sourceId: number
	semester: string
	timeSlot: TimeSlot
	room: Room
	title: string
	secondaryText: string
	colorKey: string
}

export interface ScheduleCalendarEntry extends BaseCalendarEntry {
	kind: 'schedule'
	schedule: Schedule
}

export interface RoomBookingCalendarEntry extends BaseCalendarEntry {
	kind: 'roomBooking'
	bookingDate: string | null
	roomBooking: RoomBooking
}

export type CalendarEntry = ScheduleCalendarEntry | RoomBookingCalendarEntry

function formatRoomLabel(room: Room): string {
	const label = [room.buildingCode, room.roomNumber].filter(Boolean).join(' ').trim()
	return label || 'Room unavailable'
}

export function toScheduleCalendarEntry(schedule: Schedule): ScheduleCalendarEntry {
	return {
		kind: 'schedule',
		sourceId: schedule.id,
		semester: schedule.semester,
		timeSlot: schedule.timeSlot,
		room: schedule.room,
		title: schedule.course.code,
		secondaryText: schedule.course.name,
		colorKey: schedule.course.department || 'General',
		schedule,
	}
}

export function toRoomBookingCalendarEntry(roomBooking: RoomBooking): RoomBookingCalendarEntry {
	return {
		kind: 'roomBooking',
		sourceId: roomBooking.id,
		semester: roomBooking.semester,
		timeSlot: roomBooking.timeSlot,
		room: roomBooking.room,
		title: 'Room Booking',
		secondaryText: formatRoomLabel(roomBooking.room),
		colorKey: 'Room Booking',
		bookingDate: roomBooking.bookingDate,
		roomBooking,
	}
}

export function getCalendarSelection(entry: CalendarEntry): CalendarSelection {
	return {
		kind: entry.kind,
		sourceId: entry.sourceId,
	}
}
