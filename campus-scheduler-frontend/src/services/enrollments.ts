import api from './api'
import type { Schedule } from './schedules'

export type EnrollmentStatus = 'ENROLLED' | 'WAITLISTED'

export interface EnrollmentStudentSummary {
	id: number
	studentNumber: string
	firstName: string
	lastName: string
	email: string
	department: string | null
	yearLevel: number | null
	targetCourseLoad: number | null
}

export interface Enrollment {
	id: number
	semester: string
	status: EnrollmentStatus
	student: EnrollmentStudentSummary | null
	schedule: Schedule | null
}

export interface EnrollmentFilters {
	studentId?: number
	courseId?: number
	scheduleId?: number
	semester?: string
}

export const enrollmentsService = {
	async getAll(filters?: EnrollmentFilters): Promise<Enrollment[]> {
		const queryParams = new URLSearchParams()
		if (filters?.studentId) queryParams.set('studentId', String(filters.studentId))
		if (filters?.courseId) queryParams.set('courseId', String(filters.courseId))
		if (filters?.scheduleId) queryParams.set('scheduleId', String(filters.scheduleId))
		if (filters?.semester) queryParams.set('semester', filters.semester)
		const query = queryParams.toString()
		const response = await api.get<Enrollment[]>(`/enrollments${query ? `?${query}` : ''}`)
		return response.data
	},
}
