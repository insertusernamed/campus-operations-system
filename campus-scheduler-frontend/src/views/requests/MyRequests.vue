<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { toast } from 'vue3-toastify'
import { useRole } from '@/composables/useRole'
import { changeRequestsService, type ScheduleChangeRequest } from '@/services/changeRequests'
import { roomsService, type Room } from '@/services/rooms'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import EmptyState from '@/components/common/EmptyState.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'

const { instructorId } = useRole()
const loading = ref(false)
const error = ref<string | null>(null)
const items = ref<ScheduleChangeRequest[]>([])
const rooms = ref<Room[]>([])
const timeSlots = ref<TimeSlot[]>([])
const statusByRequestId = ref<Record<number, string>>({})
let pollHandle: number | null = null

const sortedItems = computed(() => {
	return [...items.value].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
})

const roomById = computed(() => {
	const map = new Map<number, Room>()
	for (const room of rooms.value) {
		map.set(room.id, room)
	}
	return map
})

const timeSlotById = computed(() => {
	const map = new Map<number, TimeSlot>()
	for (const slot of timeSlots.value) {
		map.set(slot.id, slot)
	}
	return map
})

function formatReason(value: string): string {
	const words = value.replace(/_/g, ' ').toLowerCase().split(' ').filter(Boolean)
	return words.map(word => word.slice(0, 1).toUpperCase() + word.slice(1)).join(' ')
}

function formatDateTime(value: string | null): string {
	if (!value) return '—'
	const date = new Date(value)
	if (Number.isNaN(date.getTime())) return value
	return date.toLocaleString(undefined, {
		month: 'short',
		day: 'numeric',
		year: 'numeric',
		hour: 'numeric',
		minute: '2-digit',
	})
}

function formatPendingAge(value: string): string {
	const created = new Date(value)
	if (Number.isNaN(created.getTime())) return '—'
	const now = new Date()
	const diffMs = now.getTime() - created.getTime()
	const days = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)))
	return days === 0 ? 'Today' : `${days} day${days === 1 ? '' : 's'}`
}

function formatRoomLabelById(roomId: number | null): string {
	if (!roomId) return 'Unknown room'
	const room = roomById.value.get(roomId)
	if (!room) return `Room #${roomId}`
	return `${room.buildingCode} ${room.roomNumber}`
}

function formatTimeSlotLabelById(timeSlotId: number | null): string {
	if (!timeSlotId) return 'Unknown time'
	const slot = timeSlotById.value.get(timeSlotId)
	if (!slot) return `Time slot #${timeSlotId}`
	return timeslotsService.formatTimeSlot(slot)
}

function statusClass(status: string): string {
	if (status === 'APPROVED') return 'bg-emerald-100 text-emerald-700'
	if (status === 'REJECTED') return 'bg-red-100 text-red-700'
	return 'bg-gray-100 text-gray-700'
}

function notifyStatusChanges(nextItems: ScheduleChangeRequest[]) {
	const isInitialSnapshot = Object.keys(statusByRequestId.value).length === 0
	if (isInitialSnapshot) {
		statusByRequestId.value = Object.fromEntries(nextItems.map(item => [item.id, item.status]))
		return
	}

	for (const item of nextItems) {
		const previousStatus = statusByRequestId.value[item.id]
		if (previousStatus && previousStatus !== item.status) {
			toast.info(`${item.schedule.course.code} request is now ${item.status.toLowerCase()}`)
			continue
		}

		if (!previousStatus) {
			toast.info(`${item.schedule.course.code} request was added`)
		}
	}

	statusByRequestId.value = Object.fromEntries(nextItems.map(item => [item.id, item.status]))
}

async function loadMetadata() {
	try {
		const [roomsData, timeSlotData] = await Promise.all([roomsService.getAll(), timeslotsService.getAll()])
		rooms.value = roomsData
		timeSlots.value = timeSlotData
	} catch (e) {
		console.error(e)
	}
}

async function loadData(silent = false) {
	if (!instructorId.value) {
		items.value = []
		statusByRequestId.value = {}
		return
	}

	if (!silent) {
		loading.value = true
	}
	error.value = null
	try {
		const nextItems = await changeRequestsService.getAll({ instructorId: instructorId.value })
		notifyStatusChanges(nextItems)
		items.value = nextItems
	} catch (e) {
		console.error(e)
		error.value = 'Failed to load requests'
	} finally {
		if (!silent) {
			loading.value = false
		}
	}
}

function startPolling() {
	if (pollHandle !== null) {
		window.clearInterval(pollHandle)
	}
	pollHandle = window.setInterval(() => {
		void loadData(true)
	}, 60_000)
}

function stopPolling() {
	if (pollHandle !== null) {
		window.clearInterval(pollHandle)
		pollHandle = null
	}
}

watch(instructorId, () => {
	statusByRequestId.value = {}
	void loadData()
})

onMounted(async () => {
	await Promise.all([loadMetadata(), loadData()])
	startPolling()
})

onUnmounted(() => {
	stopPolling()
})
</script>

<template>
	<div>
		<div class="flex justify-between items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">My Requests</h1>
			<RouterLink to="/requests/new" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
				Request Change
			</RouterLink>
		</div>

		<TableSkeleton v-if="loading" :columns="5" :rows="6" />
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<EmptyState v-else-if="items.length === 0" title="No requests yet"
			description="Submit a change request to adjust your schedule." action-label="Request Change"
			action-route="/requests/new" />

		<div v-else class="space-y-4">
			<div v-for="request in sortedItems" :key="request.id" class="rounded border border-gray-200 bg-white p-4">
				<div class="flex flex-wrap items-start justify-between gap-3">
					<div>
						<div class="text-sm font-semibold text-gray-900">{{ request.schedule.course.code }} - {{
							request.schedule.course.name }}</div>
						<div class="mt-1 text-xs text-gray-500">Reason: {{ formatReason(request.reasonCategory) }}</div>
					</div>
					<div class="text-right">
						<span class="inline-flex rounded px-2 py-1 text-xs font-medium" :class="statusClass(request.status)">
							{{ request.status }}
						</span>
						<div v-if="request.status === 'PENDING'" class="mt-1 text-xs text-gray-500">
							Pending for {{ formatPendingAge(request.createdAt) }}
						</div>
					</div>
				</div>

				<div class="mt-3 grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
					<div>
						<div class="text-xs font-medium uppercase tracking-wide text-gray-500">Requested Change</div>
						<div class="mt-1 text-gray-700">
							From: {{ formatRoomLabelById(request.originalRoomId) }} / {{ formatTimeSlotLabelById(request.originalTimeSlotId) }}
						</div>
						<div class="text-gray-700">
							To: {{ request.proposedRoom ? `${request.proposedRoom.buildingCode} ${request.proposedRoom.roomNumber}` : 'Keep current room' }}
							/
							{{ request.proposedTimeSlot ? timeslotsService.formatTimeSlot(request.proposedTimeSlot) : 'Keep current time' }}
						</div>
					</div>

					<div>
						<div class="text-xs font-medium uppercase tracking-wide text-gray-500">Timeline</div>
						<div class="mt-1 text-gray-700">Requested: {{ formatDateTime(request.createdAt) }}</div>
						<div class="text-gray-700">Reviewed: {{ formatDateTime(request.reviewedAt) }}</div>
						<div class="text-gray-700">Applied: {{ formatDateTime(request.appliedAt) }}</div>
					</div>
				</div>

				<div v-if="request.reasonDetails" class="mt-3 text-sm text-gray-700">
					<span class="text-xs font-medium uppercase tracking-wide text-gray-500">Details:</span>
					{{ request.reasonDetails }}
				</div>

				<div v-if="request.decisionNote" class="mt-2 rounded bg-gray-50 px-3 py-2 text-sm text-gray-700">
					<span class="text-xs font-medium uppercase tracking-wide text-gray-500">Decision Note:</span>
					{{ request.decisionNote }}
				</div>
			</div>
		</div>
	</div>
</template>
