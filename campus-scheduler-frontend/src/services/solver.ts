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

export interface ImpactAnalysisMove {
	scheduleId: number
	courseCode: string
	fromRoomId: number
	fromRoomLabel: string
	toRoomId: number
	toRoomLabel: string
	fromTimeSlotId: number
	fromTimeSlotLabel: string
	toTimeSlotId: number
	toTimeSlotLabel: string
}

export interface ImpactConstraintSummary {
	constraintName: string
	constraintId: string
	score: string
}

export interface ImpactAnalysisResponse {
	status: 'SOLVED' | 'NO_SOLUTION'
	score: string | null
	scoreSummary: string | null
	moves: ImpactAnalysisMove[]
	constraintSummaries: ImpactConstraintSummary[]
}

export interface ImpactAnalysisRequest {
	scheduleId: number
	proposedRoomId?: number | null
	proposedTimeSlotId?: number | null
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

	async analyzeImpact(request: ImpactAnalysisRequest): Promise<ImpactAnalysisResponse> {
		const response = await api.post<ImpactAnalysisResponse>('/solver/impact', request)
		return response.data
	},
}
