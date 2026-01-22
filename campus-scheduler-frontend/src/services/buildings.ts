import api from './api'

export interface Building {
    id: number
    name: string
    code: string
    address: string | null
}

export interface CreateBuildingRequest {
    name: string
    code: string
    address?: string
}

export interface UpdateBuildingRequest {
    name?: string
    code?: string
    address?: string
}

export const buildingsService = {
    async getAll(): Promise<Building[]> {
        const response = await api.get<Building[]>('/buildings')
        return response.data
    },

    async getById(id: number): Promise<Building> {
        const response = await api.get<Building>(`/buildings/${id}`)
        return response.data
    },

    async create(building: CreateBuildingRequest): Promise<Building> {
        const response = await api.post<Building>('/buildings', building)
        return response.data
    },

    async update(id: number, building: UpdateBuildingRequest): Promise<Building> {
        const response = await api.put<Building>(`/buildings/${id}`, building)
        return response.data
    },

    async delete(id: number): Promise<void> {
        await api.delete(`/buildings/${id}`)
    },
}
