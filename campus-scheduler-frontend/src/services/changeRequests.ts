import api from './api'
import type { Schedule } from './schedules'
import type { Instructor } from './instructors'
import type { Room } from './rooms'
import type { TimeSlot } from './timeslots'

export type ChangeRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ChangeRequestRole = 'INSTRUCTOR' | 'ADMIN'
export type ChangeRequestReason = 'MEDICAL' | 'EQUIPMENT_FAILURE' | 'PEDAGOGICAL_CONFLICT' | 'OTHER'

export interface ScheduleChangeRequest {
    id: number
    schedule: Schedule
    requestedByInstructor: Instructor
    requestedByRole: ChangeRequestRole
    status: ChangeRequestStatus
    reasonCategory: ChangeRequestReason
    reasonDetails: string | null
    proposedRoom: Room | null
    proposedTimeSlot: TimeSlot | null
    originalRoomId: number
    originalTimeSlotId: number
    originalSemester: string
    decisionNote: string | null
    createdAt: string
    reviewedAt: string | null
    appliedAt: string | null
}

export interface ChangeRequestCreateRequest {
    scheduleId: number
    requestedByInstructorId: number
    requestedByRole: ChangeRequestRole
    reasonCategory: ChangeRequestReason
    reasonDetails?: string
    proposedRoomId?: number | null
    proposedTimeSlotId?: number | null
}

export interface ChangeRequestDecisionRequest {
    decisionNote?: string
    proposedRoomId?: number | null
    proposedTimeSlotId?: number | null
}

export interface ChangeRequestValidationRequest {
    scheduleId: number
    proposedRoomId?: number | null
    proposedTimeSlotId?: number | null
}

export interface ChangeRequestValidationResponse {
    green: boolean
    hardConflicts: string[]
    softWarnings: string[]
}

export const changeRequestsService = {
    async getAll(params?: {
        status?: ChangeRequestStatus
        instructorId?: number
        semester?: string
        scheduleId?: number
    }): Promise<ScheduleChangeRequest[]> {
        const queryParams = new URLSearchParams()
        if (params?.status) queryParams.set('status', params.status)
        if (params?.instructorId) queryParams.set('instructorId', String(params.instructorId))
        if (params?.semester) queryParams.set('semester', params.semester)
        if (params?.scheduleId) queryParams.set('scheduleId', String(params.scheduleId))
        const query = queryParams.toString()
        const response = await api.get<ScheduleChangeRequest[]>(`/change-requests${query ? `?${query}` : ''}`)
        return response.data
    },

    async create(request: ChangeRequestCreateRequest): Promise<ScheduleChangeRequest> {
        const response = await api.post<ScheduleChangeRequest>('/change-requests', request)
        return response.data
    },

    async approve(id: number, request: ChangeRequestDecisionRequest): Promise<ScheduleChangeRequest> {
        const response = await api.post<ScheduleChangeRequest>(`/change-requests/${id}/approve`, request)
        return response.data
    },

    async reject(id: number, request: ChangeRequestDecisionRequest): Promise<ScheduleChangeRequest> {
        const response = await api.post<ScheduleChangeRequest>(`/change-requests/${id}/reject`, request)
        return response.data
    },

    async validate(request: ChangeRequestValidationRequest): Promise<ChangeRequestValidationResponse> {
        const response = await api.post<ChangeRequestValidationResponse>('/change-requests/validate', request)
        return response.data
    },
}
