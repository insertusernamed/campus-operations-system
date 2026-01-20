import api from './api'

export interface Room {
	id: number
	roomNumber: string
	capacity: number
	type: string
	features: string | null
	building: {
		id: number
		code: string
		name: string
		address: string | null
	}
}

export interface CreateRoomRequest {
	roomNumber: string
	capacity: number
	type: string
	features?: string
	buildingId: number
}

export interface UpdateRoomRequest {
	roomNumber?: string
	capacity?: number
	type?: string
	features?: string
	buildingId?: number
}

export const roomsService = {
	async getAll(): Promise<Room[]> {
		const response = await api.get<Room[]>('/rooms')
		return response.data
	},

	async getById(id: number): Promise<Room> {
		const response = await api.get<Room>(`/rooms/${id}`)
		return response.data
	},

	async create(room: CreateRoomRequest): Promise<Room> {
		const response = await api.post<Room>('/rooms', room)
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
