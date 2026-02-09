import type { SemesterDefinition, SemesterTerm } from '@/services/semesters'

const TERM_ORDER: SemesterTerm[] = ['WINTER', 'SPRING', 'SUMMER', 'FALL']

const TERM_ALIASES: Record<SemesterTerm, string[]> = {
	WINTER: ['WINTER'],
	SPRING: ['SPRING'],
	SUMMER: ['SUMMER'],
	FALL: ['FALL', 'AUTUMN'],
}

export interface SemesterDateParts {
	year: number
	month: number
	day: number
}

export interface SemesterDateRange {
	start: SemesterDateParts
	end: SemesterDateParts
}

interface CandidateSemester {
	definition: SemesterDefinition
	year: number
	start: Date
	end: Date
}

export interface ParsedSemesterLabel {
	definition: SemesterDefinition
	year: number
}

function toUtcDate(year: number, month: number, day: number): Date {
	return new Date(Date.UTC(year, month - 1, day))
}

function normalizeDateToUtc(date: Date): Date {
	return toUtcDate(date.getFullYear(), date.getMonth() + 1, date.getDate())
}

function getDefinitionByToken(
	normalizedLabel: string,
	definitions: SemesterDefinition[]
): SemesterDefinition | null {
	return (
		definitions.find((definition) =>
			TERM_ALIASES[definition.term].some((alias) => normalizedLabel.includes(alias))
		) ?? null
	)
}

function buildCandidateSemesters(
	definitions: SemesterDefinition[],
	startYear: number,
	endYear: number
): CandidateSemester[] {
	const candidates: CandidateSemester[] = []
	for (let year = startYear; year <= endYear; year++) {
		for (const definition of definitions) {
			const range = resolveSemesterDateRange(definition, year)
			candidates.push({
				definition,
				year,
				start: toUtcDate(range.start.year, range.start.month, range.start.day),
				end: toUtcDate(range.end.year, range.end.month, range.end.day),
			})
		}
	}
	return candidates.sort((a, b) => a.start.getTime() - b.start.getTime())
}

function advanceSemester(
	orderedDefinitions: SemesterDefinition[],
	term: SemesterTerm,
	year: number
): { definition: SemesterDefinition; year: number } | null {
	if (orderedDefinitions.length === 0) {
		return null
	}

	const index = orderedDefinitions.findIndex((definition) => definition.term === term)
	if (index < 0) {
		return null
	}

	const nextIndex = (index + 1) % orderedDefinitions.length
	const nextYear = nextIndex === 0 ? year + 1 : year
	const nextDefinition = orderedDefinitions[nextIndex]
	if (!nextDefinition) {
		return null
	}
	return { definition: nextDefinition, year: nextYear }
}

export function getOrderedSemesterDefinitions(definitions: SemesterDefinition[]): SemesterDefinition[] {
	const map = new Map(definitions.map((definition) => [definition.term, definition]))
	const ordered = TERM_ORDER
		.map((term) => map.get(term))
		.filter((definition): definition is SemesterDefinition => definition !== undefined)

	if (ordered.length === definitions.length) {
		return ordered
	}
	return definitions
}

export function resolveSemesterDateRange(
	definition: SemesterDefinition,
	year: number
): SemesterDateRange {
	return {
		start: {
			year: year + definition.startYearOffset,
			month: definition.startMonth,
			day: definition.startDay,
		},
		end: {
			year: year + definition.endYearOffset,
			month: definition.endMonth,
			day: definition.endDay,
		},
	}
}

export function parseSemesterLabel(
	semesterLabel: string,
	definitions: SemesterDefinition[]
): ParsedSemesterLabel | null {
	const normalizedLabel = semesterLabel.trim().toUpperCase()
	const definition = getDefinitionByToken(normalizedLabel, definitions)
	if (!definition) {
		return null
	}

	const yearMatch = normalizedLabel.match(/\b(20\d{2})\b/)
	if (!yearMatch) {
		return null
	}
	const year = Number(yearMatch[1])
	if (Number.isNaN(year)) {
		return null
	}

	return { definition, year }
}

export function getCurrentSemester(
	definitions: SemesterDefinition[],
	referenceDate: Date = new Date()
): ParsedSemesterLabel | null {
	if (definitions.length === 0) {
		return null
	}

	const orderedDefinitions = getOrderedSemesterDefinitions(definitions)
	const normalizedReference = normalizeDateToUtc(referenceDate)
	const refYear = normalizedReference.getUTCFullYear()
	const candidates = buildCandidateSemesters(orderedDefinitions, refYear - 2, refYear + 2)

	const active = candidates.find(
		(candidate) =>
			normalizedReference.getTime() >= candidate.start.getTime() &&
			normalizedReference.getTime() <= candidate.end.getTime()
	)
	if (active) {
		return { definition: active.definition, year: active.year }
	}

	const next = candidates.find(
		(candidate) => normalizedReference.getTime() < candidate.start.getTime()
	)
	if (next) {
		return { definition: next.definition, year: next.year }
	}

	const fallback = candidates[candidates.length - 1]
	if (!fallback) {
		return null
	}
	return { definition: fallback.definition, year: fallback.year }
}

export function formatSemesterLabel(definition: SemesterDefinition, year: number): string {
	return `${definition.displayName} ${year}`
}

export function getDynamicSemesterOptions(
	definitions: SemesterDefinition[],
	referenceDate: Date = new Date(),
	count: number = 4
): string[] {
	if (definitions.length === 0 || count <= 0) {
		return []
	}

	const orderedDefinitions = getOrderedSemesterDefinitions(definitions)
	const currentSemester = getCurrentSemester(orderedDefinitions, referenceDate)
	if (!currentSemester) {
		return []
	}

	const options: string[] = []
	let activeDefinition = currentSemester.definition
	let activeYear = currentSemester.year

	for (let i = 0; i < count; i++) {
		options.push(formatSemesterLabel(activeDefinition, activeYear))
		const next = advanceSemester(orderedDefinitions, activeDefinition.term, activeYear)
		if (!next) {
			break
		}
		activeDefinition = next.definition
		activeYear = next.year
	}

	return options
}
