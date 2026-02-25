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
import { exportSingleClass, exportClassForSemester } from '@/utils/icalExport'
import { INSTRUCTOR_FRICTION_MVP } from '@/config/features'
import {
	instructorInsightsService,
	type InstructorFrictionIssue,
} from '@/services/instructorInsights'

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
const frictions = ref<InstructorFrictionIssue[]>([])
const frictionsLoading = ref(false)
const frictionsError = ref<string | null>(null)

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

const insightsSemester = computed(() => selectedSemester.value ?? semesterOptions.value[0] ?? '')

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
	void refreshSchedulesAndFrictions()
})

watch(selectedSemester, () => {
	void refreshSchedulesAndFrictions()
})

onMounted(async () => {
	await refreshSchedulesAndFrictions()
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

async function refreshSchedulesAndFrictions() {
	await fetchAll()
	await fetchFrictions()
}

async function fetchFrictions() {
	if (!INSTRUCTOR_FRICTION_MVP || role.value !== 'instructor' || !instructorId.value || !insightsSemester.value) {
		frictions.value = []
		frictionsError.value = null
		return
	}

	frictionsLoading.value = true
	frictionsError.value = null
	try {
		frictions.value = await instructorInsightsService.getFrictions(instructorId.value, insightsSemester.value)
	} catch (e) {
		console.error('Failed to load frictions', e)
		frictions.value = []
		frictionsError.value = 'Could not load schedule issues'
	} finally {
		frictionsLoading.value = false
	}
}

function frictionSeverityClass(value: InstructorFrictionIssue['severity']): string {
	switch (value) {
		case 'HIGH':
			return 'bg-red-100 text-red-700'
		case 'MEDIUM':
			return 'bg-amber-100 text-amber-700'
		default:
			return 'bg-blue-100 text-blue-700'
	}
}

function formatFrictionType(value: InstructorFrictionIssue['type']): string {
	switch (value) {
		case 'LARGE_GAP':
			return 'Long break'
		case 'TIGHT_BUILDING_HOP':
			return 'Short travel time'
		case 'OUTSIDE_PREFERRED_WINDOW':
			return 'Outside your preferred hours'
		case 'ROOM_FEATURE_MISMATCH':
			return 'Missing room setup'
		case 'NON_PREFERRED_BUILDING':
			return 'Different building'
		default:
			return value
				.replace(/_/g, ' ')
				.toLowerCase()
				.split(' ')
				.map(word => word.slice(0, 1).toUpperCase() + word.slice(1))
				.join(' ')
	}
}

function handleEventClick(scheduleId: number) {
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

function handleExportSingleClass() {
	if (!selectedSchedule.value) return
	exportSingleClass(selectedSchedule.value)
}

function handleExportClassForSemester() {
	if (!selectedSchedule.value) return
	exportClassForSemester(selectedSchedule.value)
}
</script>

<template>
	<div>
		<div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">Schedules</h1>
			<div class="flex flex-wrap items-center gap-2">
				<!-- Building Filter -->
				<select v-model="selectedBuildingId" aria-label="Building Filter"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Buildings</option>
					<option v-for="building in buildings" :key="building.id" :value="building.id">
						{{ building.code }} - {{ building.name }}
					</option>
				</select>
				<!-- Room Filter -->
				<select v-model="selectedRoomId" aria-label="Room Filter"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Rooms</option>
					<option v-for="room in filteredRooms" :key="room.id" :value="room.id">
						{{ room.roomNumber }} ({{ room.capacity }} seats)
						{{ scheduleCountByRoom.get(room.id) ? `- ${scheduleCountByRoom.get(room.id)} classes` : '' }}
					</option>
				</select>
				<!-- Semester Filter -->
				<select v-model="selectedSemester" aria-label="Semester Filter"
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
				<div v-if="viewMode === 'calendar' && role === 'admin'"
					class="flex border border-gray-300 rounded overflow-hidden">
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

		<div v-if="INSTRUCTOR_FRICTION_MVP && role === 'instructor'" class="mb-6 rounded border border-gray-200 bg-white">
			<div class="flex items-center justify-between border-b border-gray-200 p-4">
				<div>
					<h2 class="text-sm font-semibold text-gray-900">Schedule Issues</h2>
					<p class="mt-0.5 text-xs text-gray-500">Found for {{ insightsSemester || 'current semester' }}.</p>
				</div>
				<button class="text-sm font-medium text-blue-700 hover:underline" @click="fetchFrictions">Refresh</button>
			</div>

			<div v-if="frictionsLoading" class="p-4 space-y-2">
				<div v-for="i in 3" :key="i" class="h-4 w-56 rounded bg-gray-200 animate-pulse"></div>
			</div>
			<div v-else-if="frictionsError" class="p-4 text-sm text-red-700">{{ frictionsError }}</div>
			<div v-else-if="frictions.length === 0" class="p-4 text-sm text-gray-600">No schedule issues found.</div>
			<ul v-else class="divide-y divide-gray-100">
				<li v-for="issue in frictions.slice(0, 8)" :key="issue.id" class="p-4 flex items-start gap-4">
					<div class="flex-1 min-w-0">
						<div class="flex flex-wrap items-center gap-2">
							<span class="text-xs px-2 py-0.5 rounded font-medium" :class="frictionSeverityClass(issue.severity)">
								{{ issue.severity }}
							</span>
							<span class="text-xs text-gray-500">{{ formatFrictionType(issue.type) }}</span>
						</div>
						<p class="mt-2 text-sm text-gray-700">{{ issue.message }}</p>
					</div>
					<RouterLink
						:to="{ path: '/requests/new', query: { scheduleId: String(issue.scheduleId), issue: issue.recommendedIssue } }"
						class="shrink-0 inline-flex items-center rounded border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50">
						Fix
					</RouterLink>
				</li>
			</ul>
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
		<AdminDailyScheduleGrid v-else-if="viewMode === 'calendar' && role === 'admin' && calendarRangeMode === 'day'"
			:schedules="filteredItems" :buildings="buildings" :rooms="rooms" :selected-building-id="selectedBuildingId"
			:selected-room-id="selectedRoomId" @event-click="handleEventClick"
			@building-drilldown="handleBuildingDrilldown" />
		<ScheduleCalendar v-else-if="viewMode === 'calendar'" :schedules="filteredItems" :view-mode="calendarRangeMode"
			:week-days="5" :height="role === 'admin' ? 920 : undefined"
			:day-start="role === 'admin' ? '08:00' : undefined" :day-end="role === 'admin' ? '21:00' : undefined"
			:grid-step="role === 'admin' ? 30 : undefined"
			:event-width="role === 'admin' && calendarRangeMode === 'day' ? 100 : 95" @event-click="handleEventClick" />

		<!-- Table View -->
		<div v-else class="overflow-x-auto">
			<table class="w-full bg-white border border-gray-200">
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
		</div>

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

					<!-- iCal export section -->
					<div class="border-t border-gray-200 pt-3 mt-1">
						<p class="text-xs font-medium text-gray-700 mb-1">Add to Calendar</p>
						<p class="text-xs text-gray-500 mb-2">Download an .ics file to import into Google Calendar,
							Outlook, or Apple Calendar.</p>
						<div class="flex flex-wrap gap-2">
							<button v-tooltip="'One-time event for the next upcoming session of this class'"
								class="inline-flex items-center gap-1.5 rounded border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
								@click="handleExportSingleClass">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-gray-500"
									viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
									<path fill-rule="evenodd"
										d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
										clip-rule="evenodd" />
								</svg>
								Next Session
							</button>
							<button v-tooltip="'Weekly recurring events for this class through the end of the semester'"
								class="inline-flex items-center gap-1.5 rounded border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
								@click="handleExportClassForSemester">
								<svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-gray-500"
									viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
									<path fill-rule="evenodd"
										d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
										clip-rule="evenodd" />
								</svg>
								Full Semester
							</button>
						</div>
					</div>

					<!-- Change request section -->
					<div class="border-t border-gray-200 pt-3 mt-1">
						<p class="text-xs font-medium text-gray-700 mb-1">Request a Change</p>
						<p class="text-gray-600 text-xs mb-2">
							Start a change request for this class. You can refine the details on the next step.
						</p>
						<div>
							<label for="request-change-issue" class="block text-sm font-medium text-gray-700 mb-1">Why
								is
								this a problem?</label>
							<select id="request-change-issue" v-model="selectedIssue" aria-label="Request Issue"
								class="w-full px-3 py-2 border border-gray-300 rounded">
								<option value="" disabled>Select a reason</option>
								<option v-for="option in changeRequestIssueOptions" :key="option.value"
									:value="option.value">
									{{ option.label }}
								</option>
							</select>
						</div>
					</div>
				</template>
			</div>
			<div v-else class="text-sm text-gray-600">{{ role === 'admin'
				? 'Select a class to view schedule details.'
				: 'Select a class to request a change.' }}</div>

			<template #footer>
				<div v-if="role === 'admin'" class="flex justify-end gap-2">
					<button class="px-4 py-2 border border-gray-300 rounded"
						@click="handleModalVisibilityChange(false)">
						Close
					</button>
					<button :disabled="!selectedSchedule"
						class="px-4 py-2 border border-gray-300 rounded disabled:opacity-50"
						@click="handleOpenCourseFromModal">
						View Course
					</button>
					<button :disabled="!selectedSchedule"
						class="px-4 py-2 border border-gray-300 rounded disabled:opacity-50"
						@click="handleOpenRoomFromModal">
						View Room
					</button>
					<button :disabled="!selectedSchedule"
						class="px-4 py-2 bg-red-600 text-white rounded disabled:opacity-50"
						@click="handleDeleteFromModal">
						Delete
					</button>
				</div>
				<div v-else class="flex justify-end gap-2">
					<button class="px-4 py-2 border border-gray-300 rounded"
						@click="handleModalVisibilityChange(false)">
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
