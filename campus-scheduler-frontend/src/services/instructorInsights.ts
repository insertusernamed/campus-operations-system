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

export type InstructorLoadStatus = 'UNDER' | 'BALANCED' | 'OVER'
export type CoverageRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type InstructorOperationalStatus =
	| 'COVERAGE_RISK'
	| 'OVERLOADED'
	| 'UNDER_UTILIZED'
	| 'PREFERENCE_INCOMPLETE'
	| 'FRICTION_HOTSPOT'
	| 'READY'

export type InstructorQueueFilter =
	| 'all'
	| 'coverage-risk'
	| 'overloaded'
	| 'under-utilized'
	| 'preference-incomplete'
	| 'friction-hotspots'
	| 'ready'

export interface InstructorFrictionIssue {
	id: string
	type: FrictionType
	severity: FrictionSeverity
	scheduleId: number
	message: string
	recommendedIssue: RecommendedIssue
}

export interface InstructorInsightsSummary {
	totalInstructors: number
	noCurrentAssignment: number
	overloadRisk: number
	preferenceSetupIncomplete: number
	frictionHotspots: number
	departmentsWithCoverageRisk: number
}

export interface InstructorQueueRow {
	id: number
	firstName: string
	lastName: string
	fullName: string
	email: string
	department: string
	assignedCoursesCount: number
	assignedCredits: number
	targetCreditsMin: number
	targetCreditsMax: number
	loadStatus: InstructorLoadStatus
	preferenceCompletenessPercent: number
	frictionScore: number
	frictionIssueCount: number
	frictionSeverity: FrictionSeverity
	coverageRiskLevel: CoverageRiskLevel
	status: InstructorOperationalStatus
	overloadCredits: number
	underUtilizedCredits: number
	recommendedActions: string[]
}

export interface InstructorDepartmentLoad {
	department: string
	instructorCount: number
	assignedCredits: number
	targetCreditsMin: number
	targetCreditsMax: number
	unfilledCourseCount: number
	unfilledCredits: number
	coverageRiskLevel: CoverageRiskLevel
}

export interface InstructorLoadDistribution {
	semester: string
	departments: InstructorDepartmentLoad[]
}

export interface InstructorLoadTrend {
	currentCredits: number
	baselineCredits: number
	deltaCredits: number
	direction: 'UP' | 'DOWN' | 'FLAT'
}

export interface InstructorWeeklyDensity {
	dayOfWeek: string
	classCount: number
	totalMinutes: number
}

export interface InstructorAssignedCourseContext {
	courseId: number
	code: string
	name: string
	credits: number
	enrollmentCapacity: number
	scheduled: boolean
	dayOfWeek: string | null
	startTime: string | null
	endTime: string | null
	roomLabel: string | null
	semester: string | null
}

export interface InstructorRecentChange {
	timestamp: string
	label: string
	source: string
}

export interface InstructorFrictionSummary {
	total: number
	high: number
	medium: number
	low: number
}

export interface InstructorWorkbench {
	instructorId: number
	firstName: string
	lastName: string
	email: string
	department: string
	officeNumber: string | null
	semester: string
	assignedCoursesCount: number
	assignedCredits: number
	targetCreditsMin: number
	targetCreditsMax: number
	loadStatus: InstructorLoadStatus
	preferenceCompletenessPercent: number
	frictionScore: number
	frictionSummary: InstructorFrictionSummary
	loadTrend: InstructorLoadTrend
	weeklyDensity: InstructorWeeklyDensity[]
	assignedCourses: InstructorAssignedCourseContext[]
	frictionIssues: InstructorFrictionIssue[]
	recentChanges: InstructorRecentChange[]
	recommendedActions: string[]
}

export const instructorInsightsService = {
	async getSummary(semester: string): Promise<InstructorInsightsSummary> {
		const response = await api.get<InstructorInsightsSummary>('/instructor-insights/summary', {
			params: { semester },
		})
		return response.data
	},

	async getQueue(params: {
		semester: string
		filter?: InstructorQueueFilter
		department?: string
	}): Promise<InstructorQueueRow[]> {
		const response = await api.get<InstructorQueueRow[]>('/instructor-insights/queue', {
			params,
		})
		return response.data
	},

	async getLoadDistribution(semester: string): Promise<InstructorLoadDistribution> {
		const response = await api.get<InstructorLoadDistribution>('/instructor-insights/load-distribution', {
			params: { semester },
		})
		return response.data
	},

	async getWorkbench(instructorId: number, semester: string): Promise<InstructorWorkbench> {
		const response = await api.get<InstructorWorkbench>(`/instructor-insights/${instructorId}/workbench`, {
			params: { semester },
		})
		return response.data
	},

	async getFrictions(instructorId: number, semester: string): Promise<InstructorFrictionIssue[]> {
		const response = await api.get<InstructorFrictionIssue[]>('/instructor-insights/frictions', {
			params: { instructorId, semester },
		})
		return response.data
	},
}
