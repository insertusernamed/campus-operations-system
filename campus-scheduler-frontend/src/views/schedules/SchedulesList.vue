<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { schedulesService, type Schedule } from '@/services/schedules'
import { roomsService, type Room } from '@/services/rooms'
import { buildingsService, type Building } from '@/services/buildings'
import { timeslotsService } from '@/services/timeslots'
import { useRole } from '@/composables/useRole'
import ScheduleCalendar from '@/components/calendar/ScheduleCalendar.vue'
import AdminDailyScheduleGrid from '@/components/calendar/AdminDailyScheduleGrid.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'
import BaseModal from '@/components/common/BaseModal.vue'
import { changeRequestIssueOptions, type ChangeRequestIssue } from '@/constants/changeRequestIssues'

type ViewMode = 'table' | 'calendar'
type CalendarRangeMode = 'day' | 'week'
const viewMode = ref<ViewMode>('calendar')
const selectedBuildingId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const selectedSemester = ref<string | null>(null)
const rooms = ref<Room[]>([])
const buildings = ref<Building[]>([])

const { role, instructorId } = useRole()
const router = useRouter()
const calendarRangeMode = ref<CalendarRangeMode>(role.value === 'admin' ? 'day' : 'week')

const requestModalOpen = ref(false)
const selectedScheduleId = ref<number | null>(null)
const selectedIssue = ref<ChangeRequestIssue | ''>('')

const { items, loading, error, fetchAll, handleDelete } = useCrud<Schedule, never>({
	getAll: () => schedulesService.getAll({
		instructorId: role.value === 'admin' ? undefined : (instructorId.value ?? undefined),
		semester: selectedSemester.value ?? undefined,
	}),
	deleteItem: schedulesService.delete,
	listRoute: '/schedules',
	deleteConfirm: 'Are you sure you want to delete this schedule?',
})

// Hydrate schedules with full room details (in case backend schedule.room is missing building fields)
const hydratedItems = computed(() => {
	if (rooms.value.length === 0) return items.value

	const roomMap = new Map<number, Room>()
	rooms.value.forEach(r => roomMap.set(r.id, r))

	return items.value.map(s => {
		const fullRoom = roomMap.get(s.room.id)
		if (fullRoom) {
			return {
				...s,
				room: {
					...s.room,
					...fullRoom // Merge full room details including buildingId/Code
				}
			}
		}
		return s
	})
})

// Count schedules per room for indicator
const scheduleCountByRoom = computed(() => {
	const counts = new Map<number, number>()
	for (const schedule of items.value) { // Use raw items for counting to avoid circular dependency if needed, but hydrated is fine too
		const roomId = schedule.room.id
		counts.set(roomId, (counts.get(roomId) || 0) + 1)
	}
	return counts
})

// Filter rooms by selected building
const filteredRooms = computed(() => {
	if (!selectedBuildingId.value) return rooms.value
	return rooms.value.filter(r => r.buildingId === selectedBuildingId.value)
})

function parseSemesterForSort(semester: string): { year: number; termRank: number } {
	const match = semester.match(/([A-Za-z]+)\s+(\d{4})/)
	if (!match) {
		return { year: 0, termRank: 0 }
	}
	const term = match[1]?.toUpperCase() ?? ''
	const year = Number(match[2] ?? 0)
	const termOrder: Record<string, number> = {
		WINTER: 1,
		SPRING: 2,
		SUMMER: 3,
		FALL: 4,
	}
	return {
		year: Number.isNaN(year) ? 0 : year,
		termRank: termOrder[term] ?? 0,
	}
}

const semesterOptions = computed(() => {
	return Array.from(new Set(items.value.map(schedule => schedule.semester)))
		.sort((a, b) => {
			const parsedA = parseSemesterForSort(a)
			const parsedB = parseSemesterForSort(b)
			if (parsedA.year !== parsedB.year) {
				return parsedB.year - parsedA.year
			}
			if (parsedA.termRank !== parsedB.termRank) {
				return parsedB.termRank - parsedA.termRank
			}
			return a.localeCompare(b)
		})
})

// Filter schedules by selected building and room
const filteredItems = computed(() => {
	let result = hydratedItems.value
	if (selectedSemester.value) {
		result = result.filter(s => s.semester === selectedSemester.value)
	}
	if (selectedBuildingId.value) {
		result = result.filter(s => s.room.buildingId === selectedBuildingId.value)
	}
	if (selectedRoomId.value) {
		result = result.filter(s => s.room.id === selectedRoomId.value)
	}
	return result
})

const selectedSchedule = computed(() => {
	if (!selectedScheduleId.value) return null
	return hydratedItems.value.find(schedule => schedule.id === selectedScheduleId.value) ?? null
})

// Reset room selection when building changes
watch(selectedBuildingId, () => {
	selectedRoomId.value = null
})

// Refetch when role or instructor changes
watch([role, instructorId], () => {
	calendarRangeMode.value = role.value === 'admin' ? 'day' : 'week'
	fetchAll()
})

watch(selectedSemester, () => {
	fetchAll()
})

onMounted(async () => {
	await fetchAll()
	try {
		const [roomsData, buildingsData] = await Promise.all([
			roomsService.getAll(),
			buildingsService.getAll()
		])
		rooms.value = roomsData
		buildings.value = buildingsData
	} catch (e) {
		console.error('Failed to load filter data:', e)
		error.value = 'Failed to load filter data. Please try reloading the page.' as any
	}
})

function handleEventClick(scheduleId: number) {
	if (role.value === 'student') {
		return
	}
	selectedScheduleId.value = scheduleId
	if (role.value === 'instructor') {
		selectedIssue.value = ''
	}
	requestModalOpen.value = true
}

function handleStartRequest() {
	if (!selectedScheduleId.value || !selectedIssue.value) return
	requestModalOpen.value = false
	router.push({
		path: '/requests/new',
		query: {
			scheduleId: String(selectedScheduleId.value),
			issue: selectedIssue.value,
		},
	})
}

function handleModalVisibilityChange(isOpen: boolean) {
	requestModalOpen.value = isOpen
	if (!isOpen) {
		selectedScheduleId.value = null
		selectedIssue.value = ''
	}
}

function getInstructorName(schedule: Schedule): string {
	const instructor = schedule.course.instructor
	if (!instructor) {
		return 'Unassigned'
	}
	return `${instructor.firstName} ${instructor.lastName}`
}

function getSeatUtilization(schedule: Schedule): string {
	const seatsFilled = schedule.course.enrollmentCapacity
	const roomCapacity = schedule.room.capacity
	if (!roomCapacity) {
		return `${seatsFilled} students`
	}
	const percent = Math.round((seatsFilled / roomCapacity) * 100)
	return `${seatsFilled}/${roomCapacity} seats (${percent}%)`
}

function handleOpenCourseFromModal() {
	if (!selectedSchedule.value) return
	requestModalOpen.value = false
	router.push(`/courses/${selectedSchedule.value.course.id}`)
}

function handleOpenRoomFromModal() {
	if (!selectedSchedule.value) return
	requestModalOpen.value = false
	router.push(`/rooms/${selectedSchedule.value.room.id}`)
}

async function handleDeleteFromModal() {
	if (role.value !== 'admin' || !selectedScheduleId.value) return
	const scheduleId = selectedScheduleId.value
	requestModalOpen.value = false
	selectedScheduleId.value = null
	await handleDelete(scheduleId)
}

function handleBuildingDrilldown(buildingId: number) {
	selectedBuildingId.value = buildingId
	selectedRoomId.value = null
}
</script>

<template>
	<div>
		<div class="flex justify-between items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">Schedules</h1>
			<div class="flex items-center gap-3">
				<!-- Building Filter -->
				<select v-model="selectedBuildingId"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Buildings</option>
					<option v-for="building in buildings" :key="building.id" :value="building.id">
						{{ building.code }} - {{ building.name }}
					</option>
				</select>
				<!-- Room Filter -->
				<select v-model="selectedRoomId"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Rooms</option>
					<option v-for="room in filteredRooms" :key="room.id" :value="room.id">
						{{ room.roomNumber }} ({{ room.capacity }} seats)
						{{ scheduleCountByRoom.get(room.id) ? `- ${scheduleCountByRoom.get(room.id)} classes` : '' }}
					</option>
				</select>
				<!-- Semester Filter -->
				<select v-model="selectedSemester"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Semesters</option>
					<option v-for="semester in semesterOptions" :key="semester" :value="semester">
						{{ semester }}
					</option>
				</select>
				<!-- View Toggle -->
				<div class="flex border border-gray-300 rounded overflow-hidden">
					<button @click="viewMode = 'calendar'"
						:class="['px-3 py-1.5 text-sm', viewMode === 'calendar' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']">
						Calendar
					</button>
					<button @click="viewMode = 'table'"
						:class="['px-3 py-1.5 text-sm', viewMode === 'table' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']">
						Table
					</button>
				</div>
				<div v-if="viewMode === 'calendar' && role === 'admin'" class="flex border border-gray-300 rounded overflow-hidden">
					<button @click="calendarRangeMode = 'day'"
						:class="['px-3 py-1.5 text-sm', calendarRangeMode === 'day' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']">
						Day
					</button>
					<button @click="calendarRangeMode = 'week'"
						:class="['px-3 py-1.5 text-sm', calendarRangeMode === 'week' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50']">
						Week
					</button>
				</div>
				<RouterLink v-if="role === 'admin'" to="/schedules/new"
					class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Add
					Schedule</RouterLink>
			</div>
		</div>

		<TableSkeleton v-if="loading" :columns="5" :rows="6" />
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<EmptyState v-else-if="filteredItems.length === 0" title="No schedules yet"
			description="Schedules connect courses to rooms and time slots. Create your first schedule manually or use the auto-scheduler."
			:action-label="role === 'admin' ? 'Add Schedule' : undefined"
			:action-route="role === 'admin' ? '/schedules/new' : undefined"
			:secondary-label="role === 'admin' ? 'Use Solver' : undefined"
			:secondary-route="role === 'admin' ? '/solver' : undefined"
			hint="The solver can generate an optimized schedule for all your courses at once." />

		<!-- Calendar View -->
		<AdminDailyScheduleGrid
			v-else-if="viewMode === 'calendar' && role === 'admin' && calendarRangeMode === 'day'"
			:schedules="filteredItems"
			:buildings="buildings"
			:rooms="rooms"
			:selected-building-id="selectedBuildingId"
			:selected-room-id="selectedRoomId"
			@event-click="handleEventClick"
			@building-drilldown="handleBuildingDrilldown"
		/>
		<ScheduleCalendar v-else-if="viewMode === 'calendar'" :schedules="filteredItems" :view-mode="calendarRangeMode"
			:week-days="5" :height="role === 'admin' ? 920 : undefined" :day-start="role === 'admin' ? '08:00' : undefined"
			:day-end="role === 'admin' ? '21:00' : undefined" :grid-step="role === 'admin' ? 30 : undefined"
			:event-width="role === 'admin' && calendarRangeMode === 'day' ? 100 : 95" @event-click="handleEventClick" />

		<!-- Table View -->
		<table v-else class="w-full bg-white border border-gray-200">
			<thead>
				<tr class="bg-gray-50 border-b border-gray-200">
					<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Course</th>
					<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Room</th>
					<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Time</th>
					<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Semester</th>
					<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Actions</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="s in filteredItems" :key="s.id" class="border-b border-gray-100">
					<td class="px-4 py-3">
						<RouterLink :to="`/courses/${s.course.id}`" class="text-blue-600 hover:underline">{{
							s.course.code }}</RouterLink>
						<span class="text-gray-500 ml-1">{{ s.course.name }}</span>
					</td>
					<td class="px-4 py-3">
						<RouterLink :to="`/rooms/${s.room.id}`" class="text-blue-600 hover:underline">{{
							s.room.buildingCode }} {{ s.room.roomNumber }}</RouterLink>
					</td>
					<td class="px-4 py-3 text-gray-600">{{ timeslotsService.formatTimeSlot(s.timeSlot) }}</td>
					<td class="px-4 py-3 text-gray-600">{{ s.semester }}</td>
					<td class="px-4 py-3">
						<button v-if="role === 'admin'" @click="handleDelete(s.id)"
							class="text-red-600 hover:underline">Delete</button>
					</td>
				</tr>
			</tbody>
		</table>

		<BaseModal :model-value="requestModalOpen" :title="role === 'admin' ? 'Schedule Details' : 'Request a Change'"
			@update:model-value="handleModalVisibilityChange">
			<div v-if="selectedSchedule" class="space-y-3 text-sm text-gray-700">
				<div class="text-base font-medium text-gray-900">
					{{ selectedSchedule.course.code }} - {{ selectedSchedule.course.name }}
				</div>
				<template v-if="role === 'admin'">
					<div>
						<span class="font-medium text-gray-900">Time:</span>
						{{ timeslotsService.formatTimeSlot(selectedSchedule.timeSlot) }}
					</div>
					<div>
						<span class="font-medium text-gray-900">Room:</span>
						{{ selectedSchedule.room.buildingCode }} {{ selectedSchedule.room.roomNumber }}
					</div>
					<div>
						<span class="font-medium text-gray-900">Instructor:</span>
						{{ getInstructorName(selectedSchedule) }}
					</div>
					<div>
						<span class="font-medium text-gray-900">Semester:</span>
						{{ selectedSchedule.semester }}
					</div>
					<div>
						<span class="font-medium text-gray-900">Seat utilization:</span>
						{{ getSeatUtilization(selectedSchedule) }}
					</div>
				</template>
				<template v-else>
					<div>
						<span class="font-medium text-gray-900">Current time:</span>
						{{ timeslotsService.formatTimeSlot(selectedSchedule.timeSlot) }}
					</div>
					<div>
						<span class="font-medium text-gray-900">Current room:</span>
						{{ selectedSchedule.room.buildingCode }} {{ selectedSchedule.room.roomNumber }}
					</div>
					<p class="text-gray-600">
						Start a change request for this class. You can refine the details on the next step.
					</p>
					<div>
						<label for="request-change-issue" class="block text-sm font-medium text-gray-700 mb-1">Why is this a problem?</label>
						<select id="request-change-issue" v-model="selectedIssue" class="w-full px-3 py-2 border border-gray-300 rounded">
							<option value="" disabled>Select a reason</option>
							<option v-for="option in changeRequestIssueOptions" :key="option.value" :value="option.value">
								{{ option.label }}
							</option>
						</select>
					</div>
				</template>
			</div>
			<div v-else class="text-sm text-gray-600">{{ role === 'admin'
				? 'Select a class to view schedule details.'
				: 'Select a class to request a change.' }}</div>

			<template #footer>
				<div v-if="role === 'admin'" class="flex justify-end gap-2">
					<button class="px-4 py-2 border border-gray-300 rounded" @click="handleModalVisibilityChange(false)">
						Close
					</button>
					<button :disabled="!selectedSchedule" class="px-4 py-2 border border-gray-300 rounded disabled:opacity-50"
						@click="handleOpenCourseFromModal">
						View Course
					</button>
					<button :disabled="!selectedSchedule" class="px-4 py-2 border border-gray-300 rounded disabled:opacity-50"
						@click="handleOpenRoomFromModal">
						View Room
					</button>
					<button :disabled="!selectedSchedule" class="px-4 py-2 bg-red-600 text-white rounded disabled:opacity-50"
						@click="handleDeleteFromModal">
						Delete
					</button>
				</div>
				<div v-else class="flex justify-end gap-2">
					<button class="px-4 py-2 border border-gray-300 rounded" @click="handleModalVisibilityChange(false)">
						Cancel
					</button>
					<button :disabled="!selectedSchedule || !selectedIssue"
						class="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
						@click="handleStartRequest">
						Request Change
					</button>
				</div>
			</template>
		</BaseModal>
	</div>
</template>
