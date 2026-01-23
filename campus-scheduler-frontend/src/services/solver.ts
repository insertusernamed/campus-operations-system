import api from './api'

export interface SolverStatus {
	status: 'SOLVING_ACTIVE' | 'NOT_SOLVING'
	score: string | null
	assignedCourses: number
	totalCourses: number
	hardViolations: number
	softScore: number
}

export interface StartResponse {
	problemId: number
	message: string
}

export interface SaveResponse {
	savedCount: number
	message: string
}

export const solverService = {
	async start(semester: string = 'Fall 2026'): Promise<StartResponse> {
		const response = await api.post<StartResponse>('/solver/start', null, {
			params: { semester },
		})
		return response.data
	},

	async stop(): Promise<string> {
		const response = await api.post<string>('/solver/stop')
		return response.data
	},

	async getStatus(): Promise<SolverStatus> {
		const response = await api.get<SolverStatus>('/solver/status')
		return response.data
	},

	async save(): Promise<SaveResponse> {
		const response = await api.post<SaveResponse>('/solver/save')
		return response.data
	},
}
