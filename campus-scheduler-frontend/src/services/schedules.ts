import api from './api'
import type { Course } from './courses'
import type { Room } from './rooms'
import type { TimeSlot } from './timeslots'

export interface Schedule {
	id: number
	course: Course
	room: Room
	timeSlot: TimeSlot
	semester: string
	filledSeats?: number | null
	seatLimit?: number | null
	remainingSeats?: number | null
	waitlistCount?: number | null
}

export interface ScheduleCreateRequest {
	courseId: number
	roomId: number
	timeSlotId: number
	semester: string
}

export interface ConflictCheckResult {
	hasConflict: boolean
}

export const schedulesService = {
	async getAll(params?: { roomId?: number; courseId?: number; instructorId?: number; semester?: string }): Promise<Schedule[]> {
		const queryParams = new URLSearchParams()
		if (params?.roomId) queryParams.set('roomId', String(params.roomId))
		if (params?.courseId) queryParams.set('courseId', String(params.courseId))
		if (params?.instructorId) queryParams.set('instructorId', String(params.instructorId))
		if (params?.semester) queryParams.set('semester', params.semester)
		const query = queryParams.toString()
		const response = await api.get<Schedule[]>(`/schedules${query ? `?${query}` : ''}`)
		return response.data
	},

	async getById(id: number): Promise<Schedule> {
		const response = await api.get<Schedule>(`/schedules/${id}`)
		return response.data
	},

	async create(request: ScheduleCreateRequest): Promise<Schedule> {
		const response = await api.post<Schedule>('/schedules', request)
		return response.data
	},

	async delete(id: number): Promise<void> {
		await api.delete(`/schedules/${id}`)
	},

	async checkConflicts(roomId: number, timeSlotId: number, semester?: string): Promise<ConflictCheckResult> {
		const params = new URLSearchParams({
			roomId: String(roomId),
			timeSlotId: String(timeSlotId),
		})
		if (semester) params.set('semester', semester)
		const response = await api.get<ConflictCheckResult>(`/schedules/conflicts?${params}`)
		return response.data
	},
}
