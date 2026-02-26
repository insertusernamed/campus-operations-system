import type { FrictionSeverity, FrictionType } from '@/services/instructorInsights'

export function frictionSeverityClass(value: FrictionSeverity): string {
	switch (value) {
		case 'HIGH':
			return 'bg-red-100 text-red-700 slate:bg-red-200 slate:text-gray-900'
		case 'MEDIUM':
			return 'bg-amber-100 text-amber-700 slate:bg-amber-200 slate:text-gray-900'
		default:
			return 'bg-blue-100 text-blue-700 slate:bg-blue-200 slate:text-gray-900'
	}
}

export function formatFrictionType(value: FrictionType): string {
	const labels: Record<FrictionType, string> = {
		LARGE_GAP: 'Long break',
		TIGHT_BUILDING_HOP: 'Short travel time',
		OUTSIDE_PREFERRED_WINDOW: 'Outside your preferred hours',
		ROOM_FEATURE_MISMATCH: 'Missing room setup',
		NON_PREFERRED_BUILDING: 'Different building',
	}
	return labels[value]
}
