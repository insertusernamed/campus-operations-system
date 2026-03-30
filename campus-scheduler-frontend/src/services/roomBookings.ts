import api from './api'
import type { Room } from './rooms'
import type { TimeSlot } from './timeslots'

export interface RoomBookingParticipant {
	id: number
	fullName: string
	email: string
}

export interface RoomBooking {
	id: number
	room: Room
	timeSlot: TimeSlot
	semester: string
	createdAt: string
	participantCount: number
	viewerCanSeeStudentDetails: boolean
	viewerIsOwner: boolean
	viewerIsParticipant: boolean
	bookedBy: RoomBookingParticipant | null
	participants: RoomBookingParticipant[]
}

export interface CreateRoomBookingRequest {
	studentId: number
	roomId: number
	timeSlotId: number
	semester: string
	participantEmails: string[]
}

export interface RoomBookingStudentLookupResponse {
	id: number
	email: string
	fullName: string
	hasClassDuringPeriod: boolean
}

export const roomBookingsService = {
	async getAll(params?: { semester?: string; studentId?: number }): Promise<RoomBooking[]> {
		const queryParams = new URLSearchParams()
		if (params?.semester) queryParams.set('semester', params.semester)
		if (params?.studentId) queryParams.set('studentId', String(params.studentId))
		const query = queryParams.toString()
		const response = await api.get<RoomBooking[]>(`/room-bookings${query ? `?${query}` : ''}`)
		return response.data
	},

	async create(request: CreateRoomBookingRequest): Promise<RoomBooking> {
		const response = await api.post<RoomBooking>('/room-bookings', request)
		return response.data
	},

	async searchStudents(
		query: string,
		semester: string,
		timeSlotId: number,
		excludeStudentIds: number[] = []
	): Promise<RoomBookingStudentLookupResponse[]> {
		const queryParams = new URLSearchParams({
			query,
			semester,
			timeSlotId: String(timeSlotId),
		})
		for (const studentId of excludeStudentIds) {
			queryParams.append('excludeStudentId', String(studentId))
		}
		const response = await api.get<RoomBookingStudentLookupResponse[]>(
			`/room-bookings/student-search?${queryParams.toString()}`
		)
		return response.data
	},
}
