import api from './api'

export interface Room {
	id: number
	roomNumber: string
	capacity: number
	type: string
	features: string | null
	buildingId: number | null
	buildingCode: string | null
	buildingName: string | null
}

export interface CreateRoomRequest {
	roomNumber: string
	capacity: number
	type: string
	features?: string
}

export interface UpdateRoomRequest {
	roomNumber?: string
	capacity?: number
	type?: string
	features?: string
}

export const roomsService = {
	async getAll(): Promise<Room[]> {
		const response = await api.get<Room[]>('/rooms')
		return response.data
	},

	async getByBuildingId(buildingId: number): Promise<Room[]> {
		const response = await api.get<Room[]>(`/rooms?buildingId=${buildingId}`)
		return response.data
	},

	async getById(id: number): Promise<Room> {
		const response = await api.get<Room>(`/rooms/${id}`)
		return response.data
	},

	async create(buildingId: number, room: CreateRoomRequest): Promise<Room> {
		const response = await api.post<Room>(`/rooms/building/${buildingId}`, room)
		return response.data
	},

	async update(id: number, room: UpdateRoomRequest): Promise<Room> {
		const response = await api.put<Room>(`/rooms/${id}`, room)
		return response.data
	},

	async delete(id: number): Promise<void> {
		await api.delete(`/rooms/${id}`)
	},
}
