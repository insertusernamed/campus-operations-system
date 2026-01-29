import api from './api'

export interface GeneratorConfig {
	buildings?: number
	roomsPerBuilding?: number
	instructors?: number
	courses?: number
}

export interface ResearchGeneratorConfig {
	archetype: 'METROPOLIS' | 'CAMPUS_SPRAWL' | 'COMMUNITY'
	studentPopulation: number
}

export interface GeneratorResponse {
	buildings: number
	rooms: number
	instructors: number
	courses: number
	timeSlots: number
	archetype?: string
	studentPopulation?: number
}

export interface ArchetypeInfo {
	id: string
	displayName: string
	description: string
	studentsPerBuilding: number
	coursesPerBuilding: number
	studentsPerCourse: number // S/C ratio - higher = less course variety
	minStudents: number
	maxStudents: number
	academicBuildingRatio: number
	exampleUniversities: string[]
}

export interface GenerationPreview {
	archetype: string
	archetypeDisplayName: string
	studentPopulation: number
	totalBuildings: number
	academicBuildings: number
	roomsPerBuilding: number
	instructors: number
	courses: number
	totalRooms: number
	ratioInfo: string
}

export const generatorService = {
	async generateUniversity(config: GeneratorConfig = {}): Promise<GeneratorResponse> {
		const response = await api.post<GeneratorResponse>('/generator/university', config)
		return response.data
	},

	async generateWithArchetype(config: ResearchGeneratorConfig): Promise<GeneratorResponse> {
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

	async generateResearch(): Promise<GeneratorResponse> {
		const response = await api.post<GeneratorResponse>('/generator/university/research')
		return response.data
	},

	async getArchetypes(): Promise<ArchetypeInfo[]> {
		const response = await api.get<ArchetypeInfo[]>('/generator/archetypes')
		return response.data
	},

	async previewGeneration(config: ResearchGeneratorConfig): Promise<GenerationPreview> {
		const response = await api.post<GenerationPreview>('/generator/preview', config)
		return response.data
	},

	async reset(): Promise<void> {
		await api.delete('/generator/reset')
	},

	async getStats(): Promise<UniversityStats> {
		const response = await api.get<UniversityStats>('/generator/stats')
		return response.data
	},
}

export interface UniversityStats {
	buildings: number
	rooms: number
	instructors: number
	courses: number
	schedules: number
}
