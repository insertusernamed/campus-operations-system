import api from './api'

export interface GeneratorConfig {
	buildings?: number
	roomsPerBuilding?: number
	instructors?: number
	courses?: number
}

export interface GeneratorResponse {
	buildings: number
	rooms: number
	instructors: number
	courses: number
	timeSlots: number
}

export const generatorService = {
	async generateUniversity(config: GeneratorConfig = {}): Promise<GeneratorResponse> {
		const response = await api.post<GeneratorResponse>('/generator/university', config)
		return response.data
	},

	async generateSmall(): Promise<GeneratorResponse> {
		const response = await api.post<GeneratorResponse>('/generator/university/small')
		return response.data
	},

	async generateLarge(): Promise<GeneratorResponse> {
		const response = await api.post<GeneratorResponse>('/generator/university/large')
		return response.data
	},

	async reset(): Promise<void> {
		await api.delete('/generator/reset')
	},
}
