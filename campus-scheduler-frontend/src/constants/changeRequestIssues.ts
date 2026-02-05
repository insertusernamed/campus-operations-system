import type { ChangeRequestReason } from '@/services/changeRequests'

export type ChangeRequestIssue = 'GAP_TOO_LARGE_BEFORE' | 'GAP_TOO_LARGE_AFTER' | 'OTHER'

export interface ChangeRequestIssueOption {
	value: ChangeRequestIssue
	label: string
	category: ChangeRequestReason
}

export const changeRequestIssueOptions: ChangeRequestIssueOption[] = [
	// TODO: Expand issue options (room/equipment, time-of-day, adjacency, etc.).
	{
		value: 'GAP_TOO_LARGE_BEFORE',
		label: 'Gap too large before this class',
		category: 'PEDAGOGICAL_CONFLICT',
	},
	{
		value: 'GAP_TOO_LARGE_AFTER',
		label: 'Gap too large after this class',
		category: 'PEDAGOGICAL_CONFLICT',
	},
	{
		value: 'OTHER',
		label: 'Other',
		category: 'OTHER',
	},
]

export function resolveIssueOption(value: string): ChangeRequestIssueOption | undefined {
	return changeRequestIssueOptions.find(option => option.value === value)
}

export function buildReasonDetails(issueValue: string, notes: string): string | undefined {
	const option = resolveIssueOption(issueValue)
	const trimmedNotes = notes.trim()

	if (!option) {
		return trimmedNotes || undefined
	}

	if (!trimmedNotes) {
		return `Issue: ${option.label}`
	}

	return `Issue: ${option.label}. Notes: ${trimmedNotes}`
}
