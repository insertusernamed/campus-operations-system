import api from './api'

export interface Instructor {
    id: number
    firstName: string
    lastName: string
    email: string
    department: string | null
    officeNumber: string | null
}

export interface CreateInstructorRequest {
    firstName: string
    lastName: string
    email: string
    department?: string
    officeNumber?: string
}

export interface UpdateInstructorRequest {
    firstName?: string
    lastName?: string
    email?: string
    department?: string
    officeNumber?: string
}

export const instructorsService = {
    async getAll(department?: string): Promise<Instructor[]> {
        const params = department ? `?department=${encodeURIComponent(department)}` : ''
        const response = await api.get<Instructor[]>(`/instructors${params}`)
        return response.data
    },

    async getById(id: number): Promise<Instructor> {
        const response = await api.get<Instructor>(`/instructors/${id}`)
        return response.data
    },

    async create(instructor: CreateInstructorRequest): Promise<Instructor> {
        const response = await api.post<Instructor>('/instructors', instructor)
        return response.data
    },

    async update(id: number, instructor: UpdateInstructorRequest): Promise<Instructor> {
        const response = await api.put<Instructor>(`/instructors/${id}`, instructor)
        return response.data
    },

    async delete(id: number): Promise<void> {
        await api.delete(`/instructors/${id}`)
    },
}
