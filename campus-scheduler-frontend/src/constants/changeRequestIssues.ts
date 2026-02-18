import type { ChangeRequestReason } from '@/services/changeRequests'

export type ChangeRequestIssue =
	| 'GAP_TOO_LARGE_BEFORE'
	| 'GAP_TOO_LARGE_AFTER'
	| 'TIME_OF_DAY_PREFERENCE'
	| 'ROOM_EQUIPMENT_MISMATCH'
	| 'BACK_TO_BACK_TRAVEL'
	| 'OTHER'

export interface ChangeRequestIssueOption {
	value: ChangeRequestIssue
	label: string
	category: ChangeRequestReason
}

export const changeRequestIssueOptions: ChangeRequestIssueOption[] = [
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
		value: 'TIME_OF_DAY_PREFERENCE',
		label: 'Class time is too early or too late',
		category: 'PEDAGOGICAL_CONFLICT',
	},
	{
		value: 'ROOM_EQUIPMENT_MISMATCH',
		label: 'Room lacks required equipment',
		category: 'EQUIPMENT_FAILURE',
	},
	{
		value: 'BACK_TO_BACK_TRAVEL',
		label: 'Back-to-back travel time is too tight',
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
