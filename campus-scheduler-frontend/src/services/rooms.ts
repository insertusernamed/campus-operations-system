import api from './api'

export type RoomAvailabilityStatus = 'AVAILABLE' | 'MAINTENANCE' | 'OUT_OF_SERVICE'

export interface Room {
	id: number
	roomNumber: string
	capacity: number
	type: string
	availabilityStatus: RoomAvailabilityStatus
	features: string | null
	featureSet: string[]
	accessibilityFlags: string[]
	operationalNotes: string | null
	lastInspectionDate: string | null
	buildingId: number | null
	buildingCode: string | null
	buildingName: string | null
}

export interface CreateRoomRequest {
	roomNumber: string
	capacity: number
	type: string
	availabilityStatus?: RoomAvailabilityStatus
	features?: string
	featureSet?: string[]
	accessibilityFlags?: string[]
	operationalNotes?: string
	lastInspectionDate?: string | null
}

export interface UpdateRoomRequest {
	roomNumber?: string
	capacity?: number
	type?: string
	availabilityStatus?: RoomAvailabilityStatus
	features?: string
	featureSet?: string[]
	accessibilityFlags?: string[]
	operationalNotes?: string
	lastInspectionDate?: string | null
}

interface RoomApiResponse {
	id: number
	roomNumber: string
	capacity: number
	type: string
	availabilityStatus?: RoomAvailabilityStatus | null
	features?: string | null
	featureSet?: string[] | null
	accessibilityFlags?: string[] | null
	operationalNotes?: string | null
	lastInspectionDate?: string | null
	buildingId: number | null
	buildingCode: string | null
	buildingName: string | null
}

function normalizeTagList(values: string[] | null | undefined): string[] {
	if (!Array.isArray(values)) return []
	return [...new Set(values
		.map((value) => value.trim().toLowerCase())
		.filter(Boolean))]
}

function deriveFeatureSet(room: RoomApiResponse): string[] {
	const normalizedSet = normalizeTagList(room.featureSet)
	if (normalizedSet.length > 0) return normalizedSet
	if (!room.features) return []
	return [...new Set(room.features
		.split(',')
		.map((token) => token.trim().toLowerCase())
		.filter(Boolean))]
}

function normalizeRoom(room: RoomApiResponse): Room {
	return {
		id: room.id,
		roomNumber: room.roomNumber,
		capacity: room.capacity,
		type: room.type,
		availabilityStatus: room.availabilityStatus ?? 'AVAILABLE',
		features: room.features ?? null,
		featureSet: deriveFeatureSet(room),
		accessibilityFlags: normalizeTagList(room.accessibilityFlags),
		operationalNotes: room.operationalNotes ?? null,
		lastInspectionDate: room.lastInspectionDate ?? null,
		buildingId: room.buildingId ?? null,
		buildingCode: room.buildingCode ?? null,
		buildingName: room.buildingName ?? null,
	}
}

export const roomsService = {
	async getAll(): Promise<Room[]> {
		const response = await api.get<RoomApiResponse[]>('/rooms')
		return response.data.map(normalizeRoom)
	},

	async getByBuildingId(buildingId: number): Promise<Room[]> {
		const response = await api.get<RoomApiResponse[]>(`/rooms?buildingId=${buildingId}`)
		return response.data.map(normalizeRoom)
	},

	async getById(id: number): Promise<Room> {
		const response = await api.get<RoomApiResponse>(`/rooms/${id}`)
		return normalizeRoom(response.data)
	},

	async create(buildingId: number, room: CreateRoomRequest): Promise<Room> {
		const response = await api.post<RoomApiResponse>(`/rooms/building/${buildingId}`, room)
		return normalizeRoom(response.data)
	},

	async update(id: number, room: UpdateRoomRequest): Promise<Room> {
		const response = await api.put<RoomApiResponse>(`/rooms/${id}`, room)
		return normalizeRoom(response.data)
	},

	async delete(id: number): Promise<void> {
		await api.delete(`/rooms/${id}`)
	},
}
