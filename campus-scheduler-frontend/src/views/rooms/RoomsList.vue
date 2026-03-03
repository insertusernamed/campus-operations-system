<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { AxiosError } from 'axios'
import { RouterLink } from 'vue-router'
import { toast } from 'vue3-toastify'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCrud } from '@/composables/useCrud'
import { useRole } from '@/composables/useRole'
import { coursesService, type Course } from '@/services/courses'
import {
	instructorPreferencesService,
	type InstructorPreference,
} from '@/services/instructorPreferences'
import { instructorsService, type Instructor } from '@/services/instructors'
import {
	roomsService,
	type Room,
	type RoomAvailabilityStatus,
	type UpdateRoomRequest,
} from '@/services/rooms'
import { schedulesService, type Schedule } from '@/services/schedules'

type StatusFilter = 'all' | 'blocking' | 'at-risk' | 'ready'
type CapacityBandFilter = 'all' | 'small' | 'medium' | 'large'
type ReadinessStatus = 'out_of_service' | 'maintenance' | 'feature_gap' | 'capacity_risk' | 'ready'

interface RoomQueueRow extends Room {
	readinessStatus: ReadinessStatus
	topConstraint: string
	upcomingLoad: number
	matchingCoursesCount: number
	unscheduledMatchCount: number
	featureGapCount: number
	capacityRiskCount: number
}

interface MatchSuggestion {
	course: Course
	currentRoomLabel: string
	currentIssues: string[]
	impactTags: string[]
	isScheduled: boolean
	capacitySlack: number
}

interface FeatureDemandRow {
	key: string
	label: string
	demand: number
	supply: number
}

const statusFilterLabels: Record<StatusFilter, string> = {
	all: 'All Rooms',
	blocking: 'Blocking',
	'at-risk': 'At Risk',
	ready: 'Ready',
}

const statusFilter = ref<StatusFilter>('all')
const buildingFilter = ref('all')
const roomTypeFilter = ref('all')
const capacityBandFilter = ref<CapacityBandFilter>('all')
const featureFilter = ref('all')
const unscheduledDemandOnly = ref(false)
const maintenanceOnly = ref(false)
const drawerRoomId = ref<number | null>(null)
const auxiliaryError = ref<string | null>(null)
const courses = ref<Course[]>([])
const schedules = ref<Schedule[]>([])
const instructors = ref<Instructor[]>([])
const instructorPreferencesByInstructor = ref<Record<number, InstructorPreference>>({})
const updatingByRoomId = ref<Record<number, boolean>>({})
const { role } = useRole()
const canManageRooms = computed(() => role.value === 'admin')

const { items, loading, error, fetchAll, handleDelete } = useCrud<Room, never>({
	getAll: roomsService.getAll,
	deleteItem: roomsService.delete,
	listRoute: '/rooms',
	deleteConfirm: 'Are you sure you want to delete this room?',
})

function normalizeTag(value: string | null | undefined): string {
	if (!value) return ''
	return value
		.trim()
		.toLowerCase()
		.replace(/[_/]+/g, '-')
		.replace(/\s+/g, '-')
}

function labelizeTag(tag: string): string {
	if (!tag) return ''
	return tag
		.split(/[-_]/)
		.filter(Boolean)
		.map((token) => token.charAt(0).toUpperCase() + token.slice(1))
		.join(' ')
}

function collectFeatureSet(source: { featureSet?: string[]; features?: string | null }): Set<string> {
	const fromSet = Array.isArray(source.featureSet)
		? source.featureSet.map(normalizeTag).filter(Boolean)
		: []
	const fromString = source.features
		? source.features.split(',').map(normalizeTag).filter(Boolean)
		: []
	return new Set([...fromSet, ...fromString])
}

function inferPreferredRoomType(course: Course): string | null {
	const department = course.department?.trim().toLowerCase() ?? ''
	if (department.includes('chemistry') || department.includes('biology') || department.includes('physics')) {
		return 'LAB'
	}
	if (course.enrollmentCapacity >= 90) {
		return 'LECTURE_HALL'
	}
	return null
}

function matchesCourseTypePreference(course: Course, room: Room): boolean {
	const preferredType = inferPreferredRoomType(course)
	return preferredType == null || room.type === preferredType
}

function isNotFoundError(errorLike: unknown): boolean {
	return (errorLike as AxiosError)?.response?.status === 404
}

function getCapacityBandBySeats(seats: number): Exclude<CapacityBandFilter, 'all'> {
	if (seats <= 25) return 'small'
	if (seats <= 60) return 'medium'
	return 'large'
}

function getCapacityBandLabel(band: Exclude<CapacityBandFilter, 'all'>): string {
	switch (band) {
		case 'small':
			return 'Small (<=25)'
		case 'medium':
			return 'Medium (26-60)'
		default:
			return 'Large Lecture (61+)'
	}
}

function getReadinessLabel(status: ReadinessStatus): string {
	switch (status) {
		case 'out_of_service':
			return 'Out of Service'
		case 'maintenance':
			return 'Maintenance'
		case 'feature_gap':
			return 'Feature Gap'
		case 'capacity_risk':
			return 'Capacity Risk'
		default:
			return 'Ready'
	}
}

function getReadinessClass(status: ReadinessStatus): string {
	switch (status) {
		case 'out_of_service':
		case 'maintenance':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'feature_gap':
			return 'border-amber-200 bg-amber-50 text-amber-900'
		case 'capacity_risk':
			return 'border-yellow-200 bg-yellow-50 text-yellow-900'
		default:
			return 'border-green-200 bg-green-50 text-green-900'
	}
}

function getReadinessRank(status: ReadinessStatus): number {
	switch (status) {
		case 'out_of_service':
			return 0
		case 'maintenance':
			return 1
		case 'feature_gap':
			return 2
		case 'capacity_risk':
			return 3
		default:
			return 4
	}
}

function isRoomUpdating(roomId: number): boolean {
	return Boolean(updatingByRoomId.value[roomId])
}

function setRoomUpdating(roomId: number, nextValue: boolean) {
	updatingByRoomId.value = {
		...updatingByRoomId.value,
		[roomId]: nextValue,
	}
}

function replaceRoom(updatedRoom: Room) {
	const index = items.value.findIndex((room) => room.id === updatedRoom.id)
	if (index >= 0) {
		items.value[index] = updatedRoom
	}
}

function buildRoomUpdatePayload(room: Room, patch: Partial<UpdateRoomRequest>): UpdateRoomRequest {
	const nextFeatureSet = patch.featureSet ?? room.featureSet
	const nextFeaturesString = patch.features ?? nextFeatureSet.join(', ')
	return {
		roomNumber: patch.roomNumber ?? room.roomNumber,
		capacity: patch.capacity ?? room.capacity,
		type: patch.type ?? room.type,
		availabilityStatus: patch.availabilityStatus ?? room.availabilityStatus,
		featureSet: nextFeatureSet,
		features: nextFeaturesString,
		accessibilityFlags: patch.accessibilityFlags ?? room.accessibilityFlags,
		operationalNotes: patch.operationalNotes ?? (room.operationalNotes ?? ''),
		lastInspectionDate: patch.lastInspectionDate === undefined ? room.lastInspectionDate : patch.lastInspectionDate,
	}
}

function appendAuditNote(previous: string | null, action: string): string {
	const timestamp = new Date().toISOString()
	const note = `[${timestamp}] ${action}`
	return [previous?.trim(), note].filter(Boolean).join('\n')
}

async function updateRoomOperational(room: Room, patch: Partial<UpdateRoomRequest>, successMessage: string, auditAction: string) {
	setRoomUpdating(room.id, true)
	try {
		const payload = buildRoomUpdatePayload(room, {
			...patch,
			operationalNotes: appendAuditNote(room.operationalNotes, auditAction),
		})
		const updated = await roomsService.update(room.id, payload)
		replaceRoom(updated)
		toast.success(successMessage)
	} catch (updateError) {
		const message = (updateError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Failed to update room'
		toast.error(message)
	} finally {
		setRoomUpdating(room.id, false)
	}
}

async function toggleRoomAvailability(room: RoomQueueRow) {
	if (!canManageRooms.value) return
	const nextStatus: RoomAvailabilityStatus = room.availabilityStatus === 'AVAILABLE'
		? 'OUT_OF_SERVICE'
		: 'AVAILABLE'
	const message = nextStatus === 'AVAILABLE'
		? `Marked ${room.buildingCode ?? ''} ${room.roomNumber} as available`
		: `Marked ${room.buildingCode ?? ''} ${room.roomNumber} as unavailable`
	await updateRoomOperational(room, { availabilityStatus: nextStatus }, message.trim(), `availability set to ${nextStatus}`)
}

async function markRoomMaintenance(room: RoomQueueRow) {
	if (!canManageRooms.value) return
	await updateRoomOperational(
		room,
		{ availabilityStatus: 'MAINTENANCE' },
		`Moved ${room.roomNumber} to maintenance`,
		'availability set to MAINTENANCE',
	)
}

async function quickAddFeatureTag(room: RoomQueueRow) {
	if (!canManageRooms.value) return
	const input = window.prompt(`Add a feature tag for room ${room.roomNumber}`, '')
	if (!input) return

	const normalized = normalizeTag(input)
	if (!normalized) {
		toast.error('Feature tag cannot be blank')
		return
	}

	if (room.featureSet.includes(normalized)) {
		toast.info('Feature tag already exists on this room')
		return
	}

	const nextFeatureSet = [...room.featureSet, normalized]
	await updateRoomOperational(
		room,
		{
			featureSet: nextFeatureSet,
			features: nextFeatureSet.join(', '),
		},
		`Added feature tag "${labelizeTag(normalized)}"`,
		`feature tag added: ${normalized}`,
	)
}

function openMatchesDrawer(room: RoomQueueRow) {
	drawerRoomId.value = room.id
}

function closeDrawer() {
	drawerRoomId.value = null
}

const requiredFeaturesByCourseId = computed(() => {
	const map = new Map<number, string[]>()
	for (const course of courses.value) {
		const instructorId = course.instructor?.id
		if (!instructorId) {
			map.set(course.id, [])
			continue
		}
		const preference = instructorPreferencesByInstructor.value[instructorId]
		const required = preference?.requiredRoomFeatures ?? []
		map.set(
			course.id,
			[...new Set(required.map(normalizeTag).filter(Boolean))],
		)
	}
	return map
})

const scheduleByCourseId = computed(() => {
	const map = new Map<number, Schedule>()
	for (const schedule of schedules.value) {
		map.set(schedule.course.id, schedule)
	}
	return map
})

const scheduledCourseIds = computed(() => new Set(scheduleByCourseId.value.keys()))

const availableRooms = computed(() => {
	return items.value.filter((room) => room.availabilityStatus === 'AVAILABLE')
})

const upcomingLoadByRoomId = computed(() => {
	const counts = new Map<number, number>()
	for (const schedule of schedules.value) {
		const roomId = schedule.room.id
		counts.set(roomId, (counts.get(roomId) || 0) + 1)
	}
	return counts
})

const roomQueueRows = computed<RoomQueueRow[]>(() => {
	const rows = items.value.map((room) => {
		const roomFeatures = collectFeatureSet(room)
		const missingFeatureDemand = new Map<string, number>()
		let featureGapCount = 0
		let capacityRiskCount = 0
		let matchingCoursesCount = 0
		let unscheduledMatchCount = 0

		for (const course of courses.value) {
			const requiredFeatures = requiredFeaturesByCourseId.value.get(course.id) ?? []
			const missingFeatures = requiredFeatures.filter((feature) => !roomFeatures.has(feature))
			const typeFit = matchesCourseTypePreference(course, room)
			const capacityFit = room.capacity >= course.enrollmentCapacity
			const fullMatch = room.availabilityStatus === 'AVAILABLE'
				&& typeFit
				&& capacityFit
				&& missingFeatures.length === 0

			if (capacityFit && typeFit && missingFeatures.length > 0) {
				featureGapCount += 1
				for (const feature of missingFeatures) {
					missingFeatureDemand.set(feature, (missingFeatureDemand.get(feature) || 0) + 1)
				}
			}
			if (typeFit && !capacityFit) {
				capacityRiskCount += 1
			}
			if (fullMatch) {
				matchingCoursesCount += 1
				if (!scheduledCourseIds.value.has(course.id)) {
					unscheduledMatchCount += 1
				}
			}
		}

		let readinessStatus: ReadinessStatus = 'ready'
		let topConstraint = 'No blockers'

		if (room.availabilityStatus === 'OUT_OF_SERVICE') {
			readinessStatus = 'out_of_service'
			topConstraint = 'Marked out of service'
		} else if (room.availabilityStatus === 'MAINTENANCE') {
			readinessStatus = 'maintenance'
			topConstraint = 'In maintenance window'
		} else if (featureGapCount > 0) {
			readinessStatus = 'feature_gap'
			const topMissing = [...missingFeatureDemand.entries()]
				.sort((a, b) => b[1] - a[1])
				.map(([feature]) => labelizeTag(feature))
				.slice(0, 2)
			topConstraint = topMissing.length > 0
				? `Missing ${topMissing.join(', ')}`
				: 'Missing required room setup'
		} else if (capacityRiskCount > 0) {
			readinessStatus = 'capacity_risk'
			topConstraint = `${capacityRiskCount} oversized demand section${capacityRiskCount === 1 ? '' : 's'}`
		} else if (unscheduledMatchCount > 0) {
			topConstraint = `${unscheduledMatchCount} unscheduled match${unscheduledMatchCount === 1 ? '' : 'es'}`
		}

		return {
			...room,
			readinessStatus,
			topConstraint,
			upcomingLoad: upcomingLoadByRoomId.value.get(room.id) || 0,
			matchingCoursesCount,
			unscheduledMatchCount,
			featureGapCount,
			capacityRiskCount,
		}
	})

	return rows.sort((a, b) => {
		const statusDiff = getReadinessRank(a.readinessStatus) - getReadinessRank(b.readinessStatus)
		if (statusDiff !== 0) return statusDiff

		const unscheduledDiff = b.unscheduledMatchCount - a.unscheduledMatchCount
		if (unscheduledDiff !== 0) return unscheduledDiff

		const loadDiff = b.upcomingLoad - a.upcomingLoad
		if (loadDiff !== 0) return loadDiff

		const buildingDiff = (a.buildingName || '').localeCompare(b.buildingName || '')
		if (buildingDiff !== 0) return buildingDiff

		return a.roomNumber.localeCompare(b.roomNumber)
	})
})

function matchesStatusFilter(room: RoomQueueRow): boolean {
	switch (statusFilter.value) {
		case 'blocking':
			if (maintenanceOnly.value) {
				return room.readinessStatus === 'maintenance'
			}
			return room.readinessStatus === 'out_of_service' || room.readinessStatus === 'maintenance'
		case 'at-risk':
			return room.readinessStatus === 'feature_gap' || room.readinessStatus === 'capacity_risk'
		case 'ready':
			return room.readinessStatus === 'ready'
		default:
			return true
	}
}

const filteredQueueRows = computed(() => {
	return roomQueueRows.value.filter((room) => {
		if (!matchesStatusFilter(room)) return false

		if (buildingFilter.value !== 'all' && String(room.buildingId ?? '') !== buildingFilter.value) {
			return false
		}

		if (roomTypeFilter.value !== 'all' && room.type !== roomTypeFilter.value) {
			return false
		}

		if (capacityBandFilter.value !== 'all' && getCapacityBandBySeats(room.capacity) !== capacityBandFilter.value) {
			return false
		}

		if (featureFilter.value !== 'all') {
			const roomFeatures = collectFeatureSet(room)
			if (!roomFeatures.has(featureFilter.value)) return false
		}

		if (unscheduledDemandOnly.value && room.unscheduledMatchCount === 0) {
			return false
		}

		return true
	})
})

const buildingOptions = computed(() => {
	const counts = new Map<string, { id: number; label: string; count: number }>()
	for (const room of roomQueueRows.value) {
		if (room.buildingId == null) continue
		const key = String(room.buildingId)
		if (!counts.has(key)) {
			counts.set(key, {
				id: room.buildingId,
				label: `${room.buildingCode || ''} ${room.buildingName || ''}`.trim(),
				count: 0,
			})
		}
		const entry = counts.get(key)
		if (entry) entry.count += 1
	}

	return [...counts.entries()]
		.sort((a, b) => a[1].label.localeCompare(b[1].label))
		.map(([value, entry]) => ({
			value,
			label: `${entry.label} (${entry.count})`,
		}))
})

const roomTypeOptions = computed(() => {
	const counts = new Map<string, number>()
	for (const room of roomQueueRows.value) {
		counts.set(room.type, (counts.get(room.type) || 0) + 1)
	}

	return [...counts.entries()]
		.sort((a, b) => a[0].localeCompare(b[0]))
		.map(([value, count]) => ({
			value,
			label: `${value.replace(/_/g, ' ')} (${count})`,
		}))
})

const featureOptions = computed(() => {
	const counts = new Map<string, number>()
	for (const room of roomQueueRows.value) {
		for (const feature of collectFeatureSet(room)) {
			counts.set(feature, (counts.get(feature) || 0) + 1)
		}
	}

	return [...counts.entries()]
		.sort((a, b) => a[0].localeCompare(b[0]))
		.map(([value, count]) => ({
			value,
			label: `${labelizeTag(value)} (${count})`,
		}))
})

const typeDemandStats = computed(() => {
	const demand = new Map<string, number>()
	for (const course of courses.value) {
		const preferredType = inferPreferredRoomType(course)
		if (!preferredType) continue
		demand.set(preferredType, (demand.get(preferredType) || 0) + 1)
	}

	const supply = new Map<string, number>()
	for (const room of availableRooms.value) {
		supply.set(room.type, (supply.get(room.type) || 0) + 1)
	}

	const keys = new Set([...demand.keys(), ...supply.keys()])
	return [...keys]
		.map((type) => {
			const demandCount = demand.get(type) || 0
			const supplyCount = supply.get(type) || 0
			return {
				type,
				demand: demandCount,
				supply: supplyCount,
				gap: demandCount - supplyCount,
			}
		})
		.sort((a, b) => b.gap - a.gap)
})

const highDemandRoomTypes = computed(() => {
	return typeDemandStats.value.filter((item) => item.gap > 0)
})

const topHighDemandType = computed(() => highDemandRoomTypes.value[0]?.type ?? null)

const featureDemandRows = computed<FeatureDemandRow[]>(() => {
	const demand = new Map<string, number>()
	const supply = new Map<string, number>()

	for (const course of courses.value) {
		const required = requiredFeaturesByCourseId.value.get(course.id) ?? []
		for (const feature of required) {
			demand.set(feature, (demand.get(feature) || 0) + 1)
		}
	}

	for (const room of availableRooms.value) {
		for (const feature of collectFeatureSet(room)) {
			supply.set(feature, (supply.get(feature) || 0) + 1)
		}
	}

	const labDemand = courses.value.filter((course) => inferPreferredRoomType(course) === 'LAB').length
	const labSupply = availableRooms.value.filter((room) => room.type === 'LAB').length
	if (labDemand > 0 || labSupply > 0) {
		demand.set('lab-ready', labDemand)
		supply.set('lab-ready', labSupply)
	}

	const lectureDemand = courses.value.filter((course) => inferPreferredRoomType(course) === 'LECTURE_HALL').length
	const lectureSupply = availableRooms.value.filter((room) => room.type === 'LECTURE_HALL').length
	if (lectureDemand > 0 || lectureSupply > 0) {
		demand.set('large-lecture', lectureDemand)
		supply.set('large-lecture', lectureSupply)
	}

	const keys = new Set([...demand.keys(), ...supply.keys()])
	return [...keys]
		.map((key) => ({
			key,
			label: labelizeTag(key),
			demand: demand.get(key) || 0,
			supply: supply.get(key) || 0,
		}))
		.filter((row) => row.demand > 0 || row.supply > 0)
		.sort((a, b) => (b.demand - b.supply) - (a.demand - a.supply))
		.slice(0, 8)
})

const seatDemandDistribution = computed(() => {
	const counts = {
		small: 0,
		medium: 0,
		large: 0,
	}

	for (const course of courses.value) {
		counts[getCapacityBandBySeats(course.enrollmentCapacity)] += 1
	}

	return (['small', 'medium', 'large'] as const).map((band) => ({
		band,
		label: getCapacityBandLabel(band),
		count: counts[band],
	}))
})

const roomCapacityDistribution = computed(() => {
	const counts = {
		small: 0,
		medium: 0,
		large: 0,
	}

	for (const room of availableRooms.value) {
		counts[getCapacityBandBySeats(room.capacity)] += 1
	}

	return (['small', 'medium', 'large'] as const).map((band) => ({
		band,
		label: getCapacityBandLabel(band),
		count: counts[band],
	}))
})

const totalActiveRooms = computed(() => {
	return items.value.filter((room) => room.availabilityStatus !== 'OUT_OF_SERVICE').length
})

const unschedulableRoomsCount = computed(() => {
	return roomQueueRows.value.filter((room) =>
		room.readinessStatus === 'out_of_service' || room.readinessStatus === 'maintenance').length
})

const roomsUnderMaintenanceCount = computed(() => {
	return items.value.filter((room) => room.availabilityStatus === 'MAINTENANCE').length
})

const blockingCount = computed(() => {
	return roomQueueRows.value.filter((room) =>
		room.readinessStatus === 'out_of_service' || room.readinessStatus === 'maintenance').length
})

const atRiskCount = computed(() => {
	return roomQueueRows.value.filter((room) =>
		room.readinessStatus === 'feature_gap' || room.readinessStatus === 'capacity_risk').length
})

const readyCount = computed(() => roomQueueRows.value.filter((room) => room.readinessStatus === 'ready').length)

const activeFilterSummary = computed(() => {
	const summary: string[] = []

	if (statusFilter.value !== 'all') {
		summary.push(`status: ${statusFilterLabels[statusFilter.value].toLowerCase()}`)
	}
	if (maintenanceOnly.value) {
		summary.push('maintenance only')
	}
	if (buildingFilter.value !== 'all') {
		const label = buildingOptions.value.find((option) => option.value === buildingFilter.value)?.label
		if (label) summary.push(`building: ${label}`)
	}
	if (roomTypeFilter.value !== 'all') {
		summary.push(`room type: ${roomTypeFilter.value.replace(/_/g, ' ')}`)
	}
	if (capacityBandFilter.value !== 'all') {
		summary.push(`capacity: ${getCapacityBandLabel(capacityBandFilter.value)}`)
	}
	if (featureFilter.value !== 'all') {
		summary.push(`feature: ${labelizeTag(featureFilter.value)}`)
	}
	if (unscheduledDemandOnly.value) {
		summary.push('unscheduled-demand matches only')
	}

	return summary.length > 0 ? summary.join(', ') : 'current filters'
})

const selectedRoomForDrawer = computed(() => {
	if (!drawerRoomId.value) return null
	return roomQueueRows.value.find((room) => room.id === drawerRoomId.value) ?? null
})

function getCurrentCourseIssues(course: Course): string[] {
	const issues: string[] = []
	const schedule = scheduleByCourseId.value.get(course.id)
	if (!schedule) {
		issues.push('unscheduled')
		return issues
	}

	const currentRoom = schedule.room
	if (currentRoom.availabilityStatus && currentRoom.availabilityStatus !== 'AVAILABLE') {
		issues.push('availability')
	}
	if (currentRoom.capacity < course.enrollmentCapacity) {
		issues.push('capacity')
	}

	const required = requiredFeaturesByCourseId.value.get(course.id) ?? []
	if (required.length > 0) {
		const currentRoomFeatures = collectFeatureSet(currentRoom)
		if (required.some((feature) => !currentRoomFeatures.has(feature))) {
			issues.push('feature')
		}
	}

	const preferredType = inferPreferredRoomType(course)
	if (preferredType && currentRoom.type !== preferredType) {
		issues.push('type')
	}

	return issues
}

function buildImpactTags(currentIssues: string[]): string[] {
	const tags: string[] = []
	if (currentIssues.includes('unscheduled')) tags.push('supports unscheduled demand')
	if (currentIssues.includes('capacity')) tags.push('resolves capacity risk')
	if (currentIssues.includes('feature')) tags.push('resolves feature gap')
	if (currentIssues.includes('availability')) tags.push('restores schedulability')
	if (currentIssues.includes('type')) tags.push('improves room-type fit')
	if (tags.length === 0) tags.push('improves room utilization')
	return tags
}

function formatCurrentRoomLabel(course: Course): string {
	const schedule = scheduleByCourseId.value.get(course.id)
	if (!schedule) return 'Unscheduled'
	const buildingCode = schedule.room.buildingCode ? `${schedule.room.buildingCode} ` : ''
	return `${buildingCode}${schedule.room.roomNumber}`
}

const drawerSuggestions = computed<MatchSuggestion[]>(() => {
	const room = selectedRoomForDrawer.value
	if (!room) return []

	const roomFeatures = collectFeatureSet(room)
	const suggestions: MatchSuggestion[] = []

	for (const course of courses.value) {
		const requiredFeatures = requiredFeaturesByCourseId.value.get(course.id) ?? []
		const missingFeatures = requiredFeatures.filter((feature) => !roomFeatures.has(feature))
		const typeFit = matchesCourseTypePreference(course, room)
		const capacityFit = room.capacity >= course.enrollmentCapacity
		const match = room.availabilityStatus === 'AVAILABLE'
			&& typeFit
			&& capacityFit
			&& missingFeatures.length === 0

		if (!match) continue

		const currentIssues = getCurrentCourseIssues(course)
		const schedule = scheduleByCourseId.value.get(course.id)
		suggestions.push({
			course,
			currentRoomLabel: formatCurrentRoomLabel(course),
			currentIssues,
			impactTags: buildImpactTags(currentIssues),
			isScheduled: Boolean(schedule),
			capacitySlack: room.capacity - course.enrollmentCapacity,
		})
	}

	return suggestions.sort((a, b) => {
		if (a.isScheduled !== b.isScheduled) return a.isScheduled ? 1 : -1
		const impactDiff = b.impactTags.length - a.impactTags.length
		if (impactDiff !== 0) return impactDiff
		if (a.capacitySlack !== b.capacitySlack) return a.capacitySlack - b.capacitySlack
		return b.course.enrollmentCapacity - a.course.enrollmentCapacity
	})
})

const drawerFitSuggestions = computed(() => drawerSuggestions.value.slice(0, 12))

const drawerMismatchSuggestions = computed(() => {
	return drawerSuggestions.value
		.filter((suggestion) => suggestion.isScheduled && suggestion.currentIssues.length > 0)
		.slice(0, 8)
})

function resetFilters() {
	statusFilter.value = 'all'
	buildingFilter.value = 'all'
	roomTypeFilter.value = 'all'
	capacityBandFilter.value = 'all'
	featureFilter.value = 'all'
	unscheduledDemandOnly.value = false
	maintenanceOnly.value = false
}

function applyUnschedulableFilter() {
	statusFilter.value = 'blocking'
	maintenanceOnly.value = false
}

function applyMaintenanceFilter() {
	statusFilter.value = 'blocking'
	maintenanceOnly.value = true
}

function applyHighDemandTypeFilter() {
	statusFilter.value = 'at-risk'
	maintenanceOnly.value = false
	if (topHighDemandType.value) {
		roomTypeFilter.value = topHighDemandType.value
	}
}

async function loadAuxiliaryData() {
	auxiliaryError.value = null

	const [coursesResult, schedulesResult, instructorsResult] = await Promise.allSettled([
		coursesService.getAll(),
		schedulesService.getAll(),
		instructorsService.getAll(),
	])

	const failedParts: string[] = []

	if (coursesResult.status === 'fulfilled') {
		courses.value = coursesResult.value
	} else {
		courses.value = []
		failedParts.push('course demand data')
	}

	if (schedulesResult.status === 'fulfilled') {
		schedules.value = schedulesResult.value
	} else {
		schedules.value = []
		failedParts.push('schedule load data')
	}

	if (instructorsResult.status === 'fulfilled') {
		instructors.value = instructorsResult.value
	} else {
		instructors.value = []
		failedParts.push('instructor profiles')
	}

	instructorPreferencesByInstructor.value = {}
	if (instructorsResult.status === 'fulfilled' && instructors.value.length > 0) {
		const preferenceResults = await Promise.allSettled(
			instructors.value.map((instructor) => instructorPreferencesService.getByInstructorId(instructor.id)),
		)

		const preferences: Record<number, InstructorPreference> = {}
		preferenceResults.forEach((result, index) => {
			if (result.status === 'fulfilled') {
				const instructor = instructors.value[index]
				if (instructor) {
					preferences[instructor.id] = result.value
				}
			}
		})
		instructorPreferencesByInstructor.value = preferences

		const hasBlockingPreferenceFailure = preferenceResults.some(
			(result) => result.status === 'rejected' && !isNotFoundError(result.reason),
		)
		if (hasBlockingPreferenceFailure) {
			failedParts.push('instructor preference constraints')
		}
	}

	if (failedParts.length > 0) {
		auxiliaryError.value = `Could not load ${failedParts.join(' and ')}. Some readiness insights are limited.`
	}
}

const columns: Column<RoomQueueRow>[] = [
	{ key: 'roomNumber', label: 'Room', render: (room) => room.roomNumber },
	{ key: 'buildingName', label: 'Building', render: (room) => room.buildingName || '-' },
	{ key: 'readinessStatus', label: 'Readiness Status', render: (room) => getReadinessLabel(room.readinessStatus) },
	{ key: 'topConstraint', label: 'Top Constraint' },
	{ key: 'upcomingLoad', label: 'Upcoming Load', render: (room) => `${room.upcomingLoad} classes/week` },
]

watch(statusFilter, (nextStatus) => {
	if (nextStatus !== 'blocking') {
		maintenanceOnly.value = false
	}
})

watch(buildingOptions, (options) => {
	if (buildingFilter.value === 'all') return
	if (!options.some((option) => option.value === buildingFilter.value)) {
		buildingFilter.value = 'all'
	}
})

watch(roomTypeOptions, (options) => {
	if (roomTypeFilter.value === 'all') return
	if (!options.some((option) => option.value === roomTypeFilter.value)) {
		roomTypeFilter.value = 'all'
	}
})

watch(featureOptions, (options) => {
	if (featureFilter.value === 'all') return
	if (!options.some((option) => option.value === featureFilter.value)) {
		featureFilter.value = 'all'
	}
})

watch(roomQueueRows, () => {
	if (!drawerRoomId.value) return
	if (!roomQueueRows.value.some((room) => room.id === drawerRoomId.value)) {
		drawerRoomId.value = null
	}
})

onMounted(async () => {
	await Promise.all([
		fetchAll(),
		loadAuxiliaryData(),
	])
})
</script>

<template>
	<div>
		<DataTable title="Rooms" :items="filteredQueueRows" :columns="columns" :loading="loading" :error="error"
			:create-route="canManageRooms ? '/rooms/new' : undefined" create-label="Add Room"
			:edit-route="canManageRooms ? ((room) => `/rooms/${room.id}/edit`) : undefined"
			:on-delete="canManageRooms ? handleDelete : undefined"
			search-placeholder="Search by room, building, readiness, constraint, or load">
			<template #filters>
				<select v-model="statusFilter" aria-label="Room status filter"
					class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
					<option value="all">All Statuses</option>
					<option value="blocking">Blocking</option>
					<option value="at-risk">At Risk</option>
					<option value="ready">Ready</option>
				</select>

				<select v-model="buildingFilter" aria-label="Building filter"
					class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
					<option value="all">All Buildings</option>
					<option v-for="option in buildingOptions" :key="option.value" :value="option.value">
						{{ option.label }}
					</option>
				</select>

				<select v-model="roomTypeFilter" aria-label="Room type filter"
					class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
					<option value="all">All Room Types</option>
					<option v-for="option in roomTypeOptions" :key="option.value" :value="option.value">
						{{ option.label }}
					</option>
				</select>

				<select v-model="capacityBandFilter" aria-label="Capacity band filter"
					class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
					<option value="all">All Capacity Bands</option>
					<option value="small">Small (<=25)</option>
					<option value="medium">Medium (26-60)</option>
					<option value="large">Large Lecture (61+)</option>
				</select>

				<select v-model="featureFilter" aria-label="Feature filter"
					class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
					<option value="all">All Features</option>
					<option v-for="option in featureOptions" :key="option.value" :value="option.value">
						{{ option.label }}
					</option>
				</select>

				<label
					class="inline-flex items-center gap-2 rounded border border-gray-300 bg-white px-3 py-2 text-xs text-gray-700">
					<input v-model="unscheduledDemandOnly" type="checkbox"
						class="h-4 w-4 rounded border-gray-300 text-blue-600" />
					Show only unscheduled-demand matches
				</label>
			</template>

			<template #metrics="{ filteredItems }">
				<div class="space-y-4">
					<div class="grid grid-cols-2 gap-3 xl:grid-cols-4">
						<button type="button" class="rounded border p-3 text-left transition-colors" :class="statusFilter === 'all' && !maintenanceOnly
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="resetFilters">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Total Active
								Rooms</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-gray-900">{{ totalActiveRooms }}
							</div>
							<div class="mt-1 text-xs text-gray-500">Available or maintenance</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="statusFilter === 'blocking' && !maintenanceOnly
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="applyUnschedulableFilter">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Unschedulable
								Rooms</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-red-700">{{
								unschedulableRoomsCount }}</div>
							<div class="mt-1 text-xs text-gray-500">Hard blockers</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="statusFilter === 'at-risk' && roomTypeFilter !== 'all'
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="applyHighDemandTypeFilter">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">High-Demand Room
								Types</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-amber-800">{{
								highDemandRoomTypes.length }}</div>
							<div class="mt-1 text-xs text-gray-500">
								{{ topHighDemandType ? `${topHighDemandType.replace(/_/g, ' ')} has demand gap` : 'No demand gap detected' }}
							</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="maintenanceOnly
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="applyMaintenanceFilter">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Rooms Under
								Maintenance</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-red-700">{{
								roomsUnderMaintenanceCount }}</div>
							<div class="mt-1 text-xs text-gray-500">Needs reopen planning</div>
						</button>
					</div>

					<div v-if="auxiliaryError"
						class="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
						{{ auxiliaryError }}
					</div>

					<div class="grid gap-4 lg:grid-cols-3">
						<div class="rounded border border-gray-200 bg-white p-4 lg:col-span-2">
							<div class="flex items-start justify-between gap-3">
								<div>
									<div class="text-sm font-semibold text-gray-900">Demand vs Supply</div>
									<div class="mt-0.5 text-xs text-gray-500">Seat and feature pressure by current room
										inventory.</div>
								</div>
								<button type="button"
									class="rounded border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
									@click="resetFilters">
									Clear Filters
								</button>
							</div>

							<div class="mt-4 grid gap-4 md:grid-cols-2">
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs font-semibold uppercase tracking-wide text-gray-500">Requested
										Seat Distribution</div>
									<div class="mt-2 space-y-2">
										<div v-for="row in seatDemandDistribution" :key="row.band"
											class="flex items-center justify-between text-sm">
											<span class="text-gray-700">{{ row.label }}</span>
											<span class="font-medium tabular-nums text-gray-900">{{ row.count }}</span>
										</div>
									</div>
								</div>
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs font-semibold uppercase tracking-wide text-gray-500">Room
										Capacity Distribution</div>
									<div class="mt-2 space-y-2">
										<div v-for="row in roomCapacityDistribution" :key="row.band"
											class="flex items-center justify-between text-sm">
											<span class="text-gray-700">{{ row.label }}</span>
											<span class="font-medium tabular-nums text-gray-900">{{ row.count }}</span>
										</div>
									</div>
								</div>
							</div>

							<div class="mt-4 rounded border border-gray-200 p-3">
								<div class="text-xs font-semibold uppercase tracking-wide text-gray-500">Feature Demand
									vs Supply</div>
								<div v-if="featureDemandRows.length === 0" class="mt-2 text-sm text-gray-600">
									No feature demand constraints detected yet.
								</div>
								<div v-else class="mt-2 space-y-2">
									<div v-for="row in featureDemandRows" :key="row.key"
										class="flex items-center justify-between rounded border px-2 py-1.5 text-sm"
										:class="row.demand > row.supply ? 'border-amber-200 bg-amber-50' : 'border-gray-200 bg-gray-50'">
										<span class="text-gray-700">{{ row.label }}</span>
										<span class="font-medium tabular-nums text-gray-900">{{ row.demand }} demand /
											{{ row.supply }} supply</span>
									</div>
								</div>
							</div>
						</div>

						<div class="rounded border border-gray-200 bg-white p-4">
							<div class="text-sm font-semibold text-gray-900">Queue Snapshot</div>
							<div class="mt-3 space-y-3">
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs text-gray-500">Blocking</div>
									<div class="mt-1 text-xl font-semibold tabular-nums text-red-700">{{ blockingCount
										}}</div>
								</div>
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs text-gray-500">At Risk</div>
									<div class="mt-1 text-xl font-semibold tabular-nums text-amber-800">{{ atRiskCount
										}}</div>
								</div>
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs text-gray-500">Ready</div>
									<div class="mt-1 text-xl font-semibold tabular-nums text-green-800">{{ readyCount }}
									</div>
								</div>
								<div class="rounded border border-gray-200 p-3">
									<div class="text-xs text-gray-500">Shown</div>
									<div class="mt-1 text-xl font-semibold tabular-nums text-gray-900">{{
										filteredItems.length }}</div>
									<div class="mt-1 text-xs text-gray-500">After filters and search</div>
								</div>
							</div>
						</div>
					</div>
				</div>
			</template>

			<template #cell="{ item, column, value }">
				<div v-if="column.key === 'roomNumber'" class="space-y-1">
					<RouterLink :to="`/rooms/${item.id}`" class="font-medium text-blue-600 hover:underline">
						{{ item.roomNumber }}
					</RouterLink>
					<div class="text-xs text-gray-500">{{ item.type.replace(/_/g, ' ') }} • {{ item.capacity }} seats
					</div>
				</div>

				<div v-else-if="column.key === 'buildingName'" class="space-y-1">
					<div class="text-gray-800">{{ item.buildingName || '-' }}</div>
					<div class="text-xs text-gray-500">{{ item.buildingCode || 'No code' }}</div>
				</div>

				<div v-else-if="column.key === 'readinessStatus'" class="space-y-1">
					<span class="inline-flex items-center rounded border px-2 py-1 text-xs font-medium"
						:class="getReadinessClass(item.readinessStatus)">
						{{ getReadinessLabel(item.readinessStatus) }}
					</span>
				</div>

				<div v-else-if="column.key === 'topConstraint'" class="space-y-1">
					<div class="text-gray-700">{{ item.topConstraint }}</div>
					<div class="text-xs text-gray-500">
						{{ item.unscheduledMatchCount }} unscheduled match{{ item.unscheduledMatchCount === 1 ? '' :
						'es' }}
					</div>
				</div>

				<div v-else-if="column.key === 'upcomingLoad'" class="space-y-1">
					<div class="font-medium tabular-nums text-gray-800">{{ item.upcomingLoad }}</div>
					<div class="text-xs text-gray-500">classes this week</div>
				</div>

				<span v-else class="text-gray-600">{{ value }}</span>
			</template>

			<template #actions="{ item }">
				<div class="flex flex-wrap items-center gap-2">
					<template v-if="canManageRooms">
						<button type="button"
							class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
							:disabled="isRoomUpdating(item.id)" @click="toggleRoomAvailability(item)">
							{{
								item.availabilityStatus === 'AVAILABLE'
									? (isRoomUpdating(item.id) ? 'Updating...' : 'Mark Unavailable')
									: (isRoomUpdating(item.id) ? 'Updating...' : 'Mark Available')
							}}
						</button>

						<button v-if="item.availabilityStatus === 'AVAILABLE'" type="button"
							class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
							:disabled="isRoomUpdating(item.id)" @click="markRoomMaintenance(item)">
							Maintenance
						</button>

						<button type="button"
							class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
							:disabled="isRoomUpdating(item.id)" @click="quickAddFeatureTag(item)">
							Add Feature
						</button>
					</template>

					<button type="button"
						class="h-8 rounded border border-blue-300 bg-blue-50 px-2 text-xs font-medium text-blue-700 hover:bg-blue-100"
						@click="openMatchesDrawer(item)">
						Matching Courses
					</button>

					<template v-if="canManageRooms">
						<RouterLink :to="`/rooms/${item.id}/edit`" class="text-blue-600 hover:underline">
							Edit
						</RouterLink>
						<button type="button" class="text-red-600 hover:underline" @click="handleDelete(item.id)">
							Delete
						</button>
					</template>
				</div>
			</template>

			<template #filtered-empty>
				<div class="rounded border border-gray-200 bg-white p-4 text-sm text-gray-700">
					<div class="font-medium">No rooms match {{ activeFilterSummary }}.</div>
					<div class="mt-1 text-gray-600">Adjust filters or search to broaden the queue.</div>
					<button type="button"
						class="mt-3 rounded border border-gray-300 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
						@click="resetFilters">
						Reset Filters
					</button>
				</div>
			</template>

			<template #empty>
				<EmptyState title="No rooms yet"
					description="Rooms are physical spaces where classes are held. Add rooms with capacity, type, and operational status."
					action-label="Add Room" action-route="/rooms/new" secondary-label="Add Building First"
					secondary-route="/buildings/new"
					hint="Rooms belong to buildings. Create at least one building before adding rooms." />
			</template>
		</DataTable>

		<div v-if="selectedRoomForDrawer" class="fixed inset-0 z-40 flex">
			<button type="button" class="flex-1 bg-black/40" aria-label="Close suggestions drawer"
				@click="closeDrawer"></button>
			<aside class="h-full w-full max-w-2xl overflow-y-auto border-l border-gray-200 bg-white shadow-xl">
				<div class="sticky top-0 z-10 border-b border-gray-200 bg-white p-4">
					<div class="flex items-start justify-between gap-3">
						<div>
							<h2 class="text-lg font-semibold text-gray-900">
								Suggested Matches: {{ selectedRoomForDrawer.buildingCode }} {{
									selectedRoomForDrawer.roomNumber
								}}
							</h2>
							<div class="mt-1 text-sm text-gray-600">
								{{ selectedRoomForDrawer.capacity }} seats • {{ selectedRoomForDrawer.type.replace(/_/g,
								' ') }}
								•
								{{ getReadinessLabel(selectedRoomForDrawer.readinessStatus) }}
							</div>
						</div>
						<button type="button"
							class="rounded border border-gray-300 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
							@click="closeDrawer">
							Close
						</button>
					</div>
				</div>

				<div class="space-y-4 p-4">
					<div class="rounded border border-gray-200 p-3">
						<div class="text-xs font-semibold uppercase tracking-wide text-gray-500">Room Feature Set</div>
						<div class="mt-2 flex flex-wrap gap-2">
							<span v-for="feature in selectedRoomForDrawer.featureSet" :key="feature"
								class="rounded border border-gray-200 bg-gray-50 px-2 py-1 text-xs text-gray-700">
								{{ labelizeTag(feature) }}
							</span>
							<span v-if="selectedRoomForDrawer.featureSet.length === 0" class="text-xs text-gray-500">
								No feature tags configured
							</span>
						</div>
					</div>

					<div class="rounded border border-gray-200 p-3">
						<div class="text-sm font-semibold text-gray-900">Courses That Could Fit This Room</div>
						<div v-if="drawerFitSuggestions.length === 0" class="mt-2 text-sm text-gray-600">
							No course matches found with current room status, capacity, and features.
						</div>
						<div v-else class="mt-3 space-y-2">
							<div v-for="suggestion in drawerFitSuggestions" :key="`fit-${suggestion.course.id}`"
								class="rounded border border-gray-200 bg-gray-50 p-3">
								<div class="flex items-start justify-between gap-3">
									<div>
										<RouterLink :to="`/courses/${suggestion.course.id}`"
											class="font-medium text-blue-600 hover:underline">
											{{ suggestion.course.code }} - {{ suggestion.course.name }}
										</RouterLink>
										<div class="mt-1 text-xs text-gray-600">
											{{ suggestion.course.enrollmentCapacity }} seats • current: {{
												suggestion.currentRoomLabel }}
										</div>
									</div>
									<span
										class="rounded border border-gray-200 bg-white px-2 py-1 text-xs text-gray-700">
										Slack: {{ suggestion.capacitySlack }}
									</span>
								</div>
								<div class="mt-2 flex flex-wrap gap-2">
									<span v-for="tag in suggestion.impactTags" :key="`${suggestion.course.id}-${tag}`"
										class="rounded border border-blue-200 bg-blue-50 px-2 py-1 text-xs text-blue-700">
										{{ tag }}
									</span>
								</div>
							</div>
						</div>
					</div>

					<div class="rounded border border-gray-200 p-3">
						<div class="text-sm font-semibold text-gray-900">Currently Mismatched Courses This Room Could
							Absorb
						</div>
						<div v-if="drawerMismatchSuggestions.length === 0" class="mt-2 text-sm text-gray-600">
							No currently mismatched scheduled courses are resolved by this room.
						</div>
						<ul v-else class="mt-3 space-y-2">
							<li v-for="suggestion in drawerMismatchSuggestions" :key="`risk-${suggestion.course.id}`"
								class="rounded border border-amber-200 bg-amber-50 p-3">
								<div class="font-medium text-gray-900">{{ suggestion.course.code }} - {{
									suggestion.course.name
									}}</div>
								<div class="mt-1 text-xs text-gray-700">Current room: {{ suggestion.currentRoomLabel }}
								</div>
								<div class="mt-2 flex flex-wrap gap-2">
									<span v-for="issue in suggestion.currentIssues"
										:key="`${suggestion.course.id}-${issue}`"
										class="rounded border border-amber-200 bg-white px-2 py-1 text-xs text-amber-900">
										{{ issue === 'feature' ? 'feature mismatch' : issue }}
									</span>
								</div>
							</li>
						</ul>
					</div>
				</div>
			</aside>
		</div>
	</div>
</template>
