import api from './api'

export type SemesterTerm = 'WINTER' | 'SPRING' | 'SUMMER' | 'FALL'

export interface SemesterDefinition {
	term: SemesterTerm
	displayName: string
	startMonth: number
	startDay: number
	endMonth: number
	endDay: number
	startYearOffset: number
	endYearOffset: number
}

export const DEFAULT_SEMESTER_DEFINITIONS: SemesterDefinition[] = [
	{
		term: 'WINTER',
		displayName: 'Winter',
		startMonth: 12,
		startDay: 21,
		endMonth: 3,
		endDay: 20,
		startYearOffset: -1,
		endYearOffset: 0,
	},
	{
		term: 'SPRING',
		displayName: 'Spring',
		startMonth: 3,
		startDay: 21,
		endMonth: 6,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
	{
		term: 'SUMMER',
		displayName: 'Summer',
		startMonth: 6,
		startDay: 21,
		endMonth: 9,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
	{
		term: 'FALL',
		displayName: 'Fall',
		startMonth: 9,
		startDay: 21,
		endMonth: 12,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
]

let definitionsCache: SemesterDefinition[] | null = null

function isSemesterTerm(value: unknown): value is SemesterTerm {
	return value === 'WINTER' || value === 'SPRING' || value === 'SUMMER' || value === 'FALL'
}

function isSemesterDefinition(value: unknown): value is SemesterDefinition {
	if (!value || typeof value !== 'object') {
		return false
	}

	const candidate = value as Record<string, unknown>
	return (
		isSemesterTerm(candidate.term) &&
		typeof candidate.displayName === 'string' &&
		typeof candidate.startMonth === 'number' &&
		typeof candidate.startDay === 'number' &&
		typeof candidate.endMonth === 'number' &&
		typeof candidate.endDay === 'number' &&
		typeof candidate.startYearOffset === 'number' &&
		typeof candidate.endYearOffset === 'number'
	)
}

export const semestersService = {
	async getDefinitions(): Promise<SemesterDefinition[]> {
		if (definitionsCache) {
			return definitionsCache
		}

		try {
			const response = await api.get<SemesterDefinition[]>('/semesters')
			if (Array.isArray(response.data) && response.data.every(isSemesterDefinition)) {
				definitionsCache = response.data
				return definitionsCache
			}
			definitionsCache = DEFAULT_SEMESTER_DEFINITIONS
			return definitionsCache
		} catch (error) {
			console.error('Failed to fetch semester definitions', error)
			definitionsCache = DEFAULT_SEMESTER_DEFINITIONS
			return definitionsCache
		}
	},
}
