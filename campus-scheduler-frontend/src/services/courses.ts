import api from './api'

export interface Course {
    id: number
    code: string
    name: string
    description: string | null
    credits: number
    enrollmentCapacity: number
    department: string | null
    instructor: {
        id: number
        firstName: string
        lastName: string
        email: string
    } | null
}

export interface CreateCourseRequest {
    code: string
    name: string
    description?: string
    credits: number
    enrollmentCapacity: number
    department?: string
}

export interface UpdateCourseRequest {
    code?: string
    name?: string
    description?: string
    credits?: number
    enrollmentCapacity?: number
    department?: string
}

export const coursesService = {
    async getAll(params?: { department?: string; instructorId?: number }): Promise<Course[]> {
        const queryParams = new URLSearchParams()
        if (params?.department) queryParams.set('department', params.department)
        if (params?.instructorId) queryParams.set('instructorId', String(params.instructorId))
        const query = queryParams.toString()
        const response = await api.get<Course[]>(`/courses${query ? `?${query}` : ''}`)
        return response.data
    },

    async getById(id: number): Promise<Course> {
        const response = await api.get<Course>(`/courses/${id}`)
        return response.data
    },

    async getByCode(code: string): Promise<Course> {
        const response = await api.get<Course>(`/courses/code/${encodeURIComponent(code)}`)
        return response.data
    },

    async getByInstructor(instructorId: number): Promise<Course[]> {
        const response = await api.get<Course[]>(`/courses/instructor/${instructorId}`)
        return response.data
    },

    async create(course: CreateCourseRequest): Promise<Course> {
        const response = await api.post<Course>('/courses', course)
        return response.data
    },

    async createWithInstructor(instructorId: number, course: CreateCourseRequest): Promise<Course> {
        const response = await api.post<Course>(`/courses/instructor/${instructorId}`, course)
        return response.data
    },

    async update(id: number, course: UpdateCourseRequest): Promise<Course> {
        const response = await api.put<Course>(`/courses/${id}`, course)
        return response.data
    },

    async assignInstructor(courseId: number, instructorId: number): Promise<Course> {
        const response = await api.put<Course>(`/courses/${courseId}/instructor/${instructorId}`)
        return response.data
    },

    async delete(id: number): Promise<void> {
        await api.delete(`/courses/${id}`)
    },
}
