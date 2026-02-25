import api from './api'

export interface InstructorPreference {
	instructorId: number
	preferredStartTime: string | null
	preferredEndTime: string | null
	maxGapMinutes: number
	minTravelBufferMinutes: number
	avoidBuildingHops: boolean
	preferredBuildingIds: number[]
	requiredRoomFeatures: string[]
	updatedAt: string
}

export interface InstructorPreferenceUpdateRequest {
	preferredStartTime: string | null
	preferredEndTime: string | null
	maxGapMinutes: number
	minTravelBufferMinutes: number
	avoidBuildingHops: boolean
	preferredBuildingIds: number[]
	requiredRoomFeatures: string[]
}

export interface RoomFeatureOption {
	value: string
	label: string
	category: string
}

export const instructorPreferencesService = {
	async getByInstructorId(instructorId: number): Promise<InstructorPreference> {
		const response = await api.get<InstructorPreference>(`/instructor-preferences/${instructorId}`)
		return response.data
	},

	async getRoomFeatureOptions(): Promise<RoomFeatureOption[]> {
		const response = await api.get<RoomFeatureOption[]>('/instructor-preferences/room-feature-options')
		return response.data
	},

	async upsert(
		instructorId: number,
		request: InstructorPreferenceUpdateRequest,
	): Promise<InstructorPreference> {
		const response = await api.put<InstructorPreference>(`/instructor-preferences/${instructorId}`, request)
		return response.data
	},
}
