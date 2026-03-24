import api from './api'
import type { Enrollment } from './enrollments'

export interface Student {
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

export interface StudentSemesterSchedule {
	studentId: number
	semester: string
	enrolled: Enrollment[]
	waitlisted: Enrollment[]
}

export const studentsService = {
	async getAll(): Promise<Student[]> {
		const response = await api.get<Student[]>('/students')
		return response.data
	},

	async getById(id: number): Promise<Student> {
		const response = await api.get<Student>(`/students/${id}`)
		return response.data
	},

	async getSchedule(id: number, semester: string): Promise<StudentSemesterSchedule> {
		const params = new URLSearchParams({ semester })
		const response = await api.get<StudentSemesterSchedule>(`/students/${id}/schedule?${params}`)
		return response.data
	},
}
