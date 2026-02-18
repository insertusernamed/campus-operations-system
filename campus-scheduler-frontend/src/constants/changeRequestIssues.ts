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
	template: string
}

export const changeRequestIssueOptions: ChangeRequestIssueOption[] = [
	{
		value: 'GAP_TOO_LARGE_BEFORE',
		label: 'Gap too large before this class',
		category: 'PEDAGOGICAL_CONFLICT',
		template:
			'Current issue: Large gap before class.\n' +
			'Details:\n' +
			'- Previous class end time:\n' +
			'- Current gap length:\n' +
			'- Requested adjustment:',
	},
	{
		value: 'GAP_TOO_LARGE_AFTER',
		label: 'Gap too large after this class',
		category: 'PEDAGOGICAL_CONFLICT',
		template:
			'Current issue: Large gap after class.\n' +
			'Details:\n' +
			'- Next class start time:\n' +
			'- Current gap length:\n' +
			'- Requested adjustment:',
	},
	{
		value: 'TIME_OF_DAY_PREFERENCE',
		label: 'Class time is too early or too late',
		category: 'PEDAGOGICAL_CONFLICT',
		template:
			'Current issue: Class time is too early/late.\n' +
			'Details:\n' +
			'- Current class time:\n' +
			'- Preferred time window:\n' +
			'- Teaching impact:',
	},
	{
		value: 'ROOM_EQUIPMENT_MISMATCH',
		label: 'Room lacks required equipment',
		category: 'EQUIPMENT_FAILURE',
		template:
			'Current issue: Room equipment mismatch.\n' +
			'Details:\n' +
			'- Required equipment:\n' +
			'- Current room limitations:\n' +
			'- Requested room capability:',
	},
	{
		value: 'BACK_TO_BACK_TRAVEL',
		label: 'Back-to-back travel time is too tight',
		category: 'PEDAGOGICAL_CONFLICT',
		template:
			'Current issue: Back-to-back travel is too tight.\n' +
			'Details:\n' +
			'- Previous room location:\n' +
			'- Estimated travel time needed:\n' +
			'- Requested adjustment:',
	},
	{
		value: 'OTHER',
		label: 'Other',
		category: 'OTHER',
		template:
			'Current issue:\n' +
			'Details:\n' +
			'- What is happening:\n' +
			'- Why this change is needed:\n' +
			'- Requested outcome:',
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

export function buildReasonTemplate(issueValue: string): string {
	return resolveIssueOption(issueValue)?.template ?? ''
}
