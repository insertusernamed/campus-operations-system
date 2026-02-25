import api from './api'

export type FrictionType =
	| 'LARGE_GAP'
	| 'TIGHT_BUILDING_HOP'
	| 'OUTSIDE_PREFERRED_WINDOW'
	| 'ROOM_FEATURE_MISMATCH'
	| 'NON_PREFERRED_BUILDING'

export type FrictionSeverity = 'LOW' | 'MEDIUM' | 'HIGH'

export type RecommendedIssue =
	| 'GAP_TOO_LARGE_BEFORE'
	| 'GAP_TOO_LARGE_AFTER'
	| 'TIME_OF_DAY_PREFERENCE'
	| 'BACK_TO_BACK_TRAVEL'
	| 'ROOM_EQUIPMENT_MISMATCH'
	| 'OTHER'

export interface InstructorFrictionIssue {
	id: string
	type: FrictionType
	severity: FrictionSeverity
	scheduleId: number
	message: string
	recommendedIssue: RecommendedIssue
}

export const instructorInsightsService = {
	async getFrictions(instructorId: number, semester: string): Promise<InstructorFrictionIssue[]> {
		const response = await api.get<InstructorFrictionIssue[]>('/instructor-insights/frictions', {
			params: { instructorId, semester },
		})
		return response.data
	},
}
