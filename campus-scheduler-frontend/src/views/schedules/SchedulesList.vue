<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { useRole } from '@/composables/useRole'
import { useRoomBookingWorkflow } from '@/composables/useRoomBookingWorkflow'
import ScheduleCalendar from '@/components/calendar/ScheduleCalendar.vue'
import AdminDailyScheduleGrid from '@/components/calendar/AdminDailyScheduleGrid.vue'
import {
	toRoomBookingCalendarEntry,
	toScheduleCalendarEntry,
	type CalendarSelection,
} from '@/components/calendar/types'
import TableSkeleton from '@/components/common/TableSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import RoomBookingModal from '@/components/schedules/RoomBookingModal.vue'
import ScheduleSelectionModal from '@/components/schedules/ScheduleSelectionModal.vue'
import { INSTRUCTOR_FRICTION_MVP } from '@/config/features'
import { exportClassForSemester, exportSingleClass } from '@/utils/icalExport'
import { formatFrictionType, frictionSeverityClass } from '@/utils/friction'
import { buildingsService, type Building } from '@/services/buildings'
import { enrollmentsService, type EnrollmentStatus } from '@/services/enrollments'
import {
	instructorInsightsService,
	type InstructorFrictionIssue,
} from '@/services/instructorInsights'
import { roomsService, type Room } from '@/services/rooms'
import { schedulesService, type Schedule } from '@/services/schedules'
import { studentsService } from '@/services/students'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import type { ChangeRequestIssue } from '@/constants/changeRequestIssues'
import {
	formatRoom,
	getSeatPressure,
	getSeatUtilization,
	getWaitlistSummary,
	sortRoomBookingsByTime,
	sortSchedulesByTime,
	sortSemesters,
} from '@/views/schedules/helpers'

type ViewMode = 'table' | 'calendar'
type CalendarRangeMode = 'day' | 'week'

const viewMode = ref<ViewMode>('calendar')
const selectedBuildingId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const selectedSemester = ref<string | null>(null)
const rooms = ref<Room[]>([])
const buildings = ref<Building[]>([])
const timeSlots = ref<TimeSlot[]>([])
const studentSemesters = ref<string[]>([])
const studentEnrolledItems = ref<Schedule[]>([])
const studentWaitlistedItems = ref<Schedule[]>([])
const studentSemesterSchedules = ref<Schedule[]>([])
const studentLoading = ref(false)
const studentError = ref<string | null>(null)
const referenceDataError = ref<string | null>(null)

const detailsModalOpen = ref(false)
const selectedCalendarSelection = ref<CalendarSelection | null>(null)
const selectedIssue = ref<ChangeRequestIssue | ''>('')

const frictions = ref<InstructorFrictionIssue[]>([])
const frictionsLoading = ref(false)
const frictionsError = ref<string | null>(null)

const { role, instructorId, studentId } = useRole()
const router = useRouter()
const calendarRangeMode = ref<CalendarRangeMode>(role.value === 'admin' ? 'day' : 'week')

const { items, loading, error, fetchAll, handleDelete } = useCrud<Schedule, never>({
	getAll: () => {
		if (role.value === 'student') {
			return Promise.resolve([])
		}
		return schedulesService.getAll({
			instructorId: role.value === 'instructor' ? (instructorId.value ?? undefined) : undefined,
			semester: selectedSemester.value ?? undefined,
		})
	},
	deleteItem: schedulesService.delete,
	listRoute: '/schedules',
	deleteConfirm: 'Are you sure you want to delete this schedule?',
})

const pageTitle = computed(() => role.value === 'student' ? 'My Schedule' : 'Schedules')

const hydratedItems = computed(() => {
	if (rooms.value.length === 0) return items.value

	const roomMap = new Map<number, Room>()
	rooms.value.forEach(room => roomMap.set(room.id, room))

	return items.value.map(schedule => {
		const fullRoom = roomMap.get(schedule.room.id)
		if (!fullRoom) {
			return schedule
		}

		return {
			...schedule,
			room: {
				...schedule.room,
				...fullRoom,
			},
		}
	})
})

const {
	roomBookings,
	roomBookingsLoading,
	roomBookingsError,
	bookingModalOpen,
	bookingSaving,
	bookingError,
	bookingForm,
	selectedParticipants,
	participantSearchQuery,
	participantSearchLoading,
	participantSearchError,
	bookingSemesterOptions,
	sortedTimeSlots,
	selectedBookingTimeSlot,
	availableBookingRooms,
	canSubmitBooking,
	participantSuggestions,
	loadRoomBookings,
	setBookingModalOpen,
	openBookingModal,
	addParticipant,
	removeParticipant,
	submitRoomBooking,
} = useRoomBookingWorkflow({
	role,
	studentId,
	selectedSemester,
	rooms,
	timeSlots,
	studentSemesters,
	scheduleAvailabilitySource: hydratedItems,
	studentSemesterSchedules,
})

const scheduleCountByRoom = computed(() => {
	const counts = new Map<number, number>()
	for (const schedule of items.value) {
		counts.set(schedule.room.id, (counts.get(schedule.room.id) ?? 0) + 1)
	}
	return counts
})

const filteredRooms = computed(() => {
	if (!selectedBuildingId.value) return rooms.value
	return rooms.value.filter(room => room.buildingId === selectedBuildingId.value)
})

const semesterOptions = computed(() => {
	if (role.value === 'student') {
		return studentSemesters.value
	}

	return sortSemesters(
		Array.from(
			new Set([
				...items.value.map(schedule => schedule.semester),
				...roomBookings.value.map(booking => booking.semester),
			])
		)
	)
})

const insightsSemester = computed(() => selectedSemester.value ?? semesterOptions.value[0] ?? '')

const filteredItems = computed(() => {
	let result = hydratedItems.value
	if (selectedSemester.value) {
		result = result.filter(schedule => schedule.semester === selectedSemester.value)
	}
	if (selectedBuildingId.value) {
		result = result.filter(schedule => schedule.room.buildingId === selectedBuildingId.value)
	}
	if (selectedRoomId.value) {
		result = result.filter(schedule => schedule.room.id === selectedRoomId.value)
	}
	return result
})

const filteredRoomBookings = computed(() => {
	let result = roomBookings.value
	if (selectedSemester.value) {
		result = result.filter(booking => booking.semester === selectedSemester.value)
	}
	if (selectedBuildingId.value) {
		result = result.filter(booking => booking.room.buildingId === selectedBuildingId.value)
	}
	if (selectedRoomId.value) {
		result = result.filter(booking => booking.room.id === selectedRoomId.value)
	}
	return sortRoomBookingsByTime(result)
})

const calendarEntries = computed(() => [
	...filteredItems.value.map(schedule => toScheduleCalendarEntry(schedule)),
	...filteredRoomBookings.value.map(booking => toRoomBookingCalendarEntry(booking)),
])

const studentRoomBookings = computed(() =>
	sortRoomBookingsByTime(
		filteredRoomBookings.value.filter(
			booking => booking.viewerIsParticipant || booking.viewerIsOwner
		)
	)
)

const studentHasAnyRows = computed(() =>
	studentEnrolledItems.value.length > 0
	|| studentWaitlistedItems.value.length > 0
	|| studentRoomBookings.value.length > 0
)

const selectedSchedule = computed(() => {
	if (selectedCalendarSelection.value?.kind !== 'schedule') return null

	if (role.value === 'student') {
		return studentEnrolledItems.value.find(schedule => schedule.id === selectedCalendarSelection.value?.sourceId)
			?? studentWaitlistedItems.value.find(schedule => schedule.id === selectedCalendarSelection.value?.sourceId)
			?? null
	}

	return hydratedItems.value.find(schedule => schedule.id === selectedCalendarSelection.value?.sourceId) ?? null
})

const selectedRoomBooking = computed(() => {
	if (selectedCalendarSelection.value?.kind !== 'roomBooking') return null
	return roomBookings.value.find(booking => booking.id === selectedCalendarSelection.value?.sourceId) ?? null
})

const selectedStudentStatus = computed<EnrollmentStatus | null>(() => {
	if (selectedCalendarSelection.value?.kind !== 'schedule') return null
	const scheduleId = selectedCalendarSelection.value.sourceId
	if (studentEnrolledItems.value.some(schedule => schedule.id === scheduleId)) {
		return 'ENROLLED'
	}
	if (studentWaitlistedItems.value.some(schedule => schedule.id === scheduleId)) {
		return 'WAITLISTED'
	}
	return null
})

const detailModalTitle = computed(() => {
	if (selectedRoomBooking.value) {
		return 'Room Booking Details'
	}
	if (role.value === 'instructor') {
		return 'Request a Change'
	}
	return 'Schedule Details'
})

const selectedDetailRoomId = computed(() =>
	selectedRoomBooking.value?.room.id ?? selectedSchedule.value?.room.id ?? null
)

const activeLoading = computed(() => {
	if (role.value === 'student') {
		return studentLoading.value || roomBookingsLoading.value
	}
	return loading.value || roomBookingsLoading.value
})

const activeError = computed(() => {
	if (referenceDataError.value) return referenceDataError.value
	if (role.value === 'student') return studentError.value
	return error.value
})

let syncingStudentSemester = false

watch(selectedBuildingId, () => {
	selectedRoomId.value = null
})

watch(
	selectedSemester,
	() => {
		if (role.value === 'student' && syncingStudentSemester) {
			return
		}
		void refreshSchedulesAndFrictions()
	},
	{ flush: 'sync' }
)

watch([role, instructorId, studentId], async () => {
	calendarRangeMode.value = role.value === 'admin' ? 'day' : 'week'
	if (role.value === 'student') {
		viewMode.value = 'table'
		selectedBuildingId.value = null
		selectedRoomId.value = null
	}
	await loadReferenceData()
	await refreshSchedulesAndFrictions()
})

onMounted(async () => {
	await loadReferenceData()
	await refreshSchedulesAndFrictions()
})

async function loadReferenceData() {
	referenceDataError.value = null
	try {
		const [roomData, buildingData, timeSlotData] = await Promise.all([
			roomsService.getAll(),
			buildingsService.getAll(),
			timeslotsService.getAll(),
		])
		rooms.value = roomData
		buildings.value = buildingData
		timeSlots.value = timeSlotData
	} catch (cause) {
		console.error('Failed to load reference data:', cause)
		referenceDataError.value = 'Failed to load schedule reference data. Please try reloading the page.'
	}
}

function isDefinedSchedule(schedule: Schedule | null | undefined): schedule is Schedule {
	return schedule !== null && schedule !== undefined
}

function mergeSeatSummary(schedule: Schedule, summary: Schedule | undefined): Schedule {
	if (!summary) {
		return schedule
	}
	return {
		...schedule,
		filledSeats: summary.filledSeats ?? schedule.filledSeats ?? null,
		seatLimit: summary.seatLimit ?? schedule.seatLimit ?? null,
		remainingSeats: summary.remainingSeats ?? schedule.remainingSeats ?? null,
		waitlistCount: summary.waitlistCount ?? schedule.waitlistCount ?? null,
	}
}

async function fetchStudentSchedules() {
	studentLoading.value = true
	studentError.value = null

	try {
		if (!studentId.value) {
			studentSemesters.value = []
			studentEnrolledItems.value = []
			studentWaitlistedItems.value = []
			studentSemesterSchedules.value = []
			syncingStudentSemester = true
			selectedSemester.value = null
			syncingStudentSemester = false
			return
		}

		const allEnrollments = await enrollmentsService.getAll({ studentId: studentId.value })
		const semesters = sortSemesters(Array.from(new Set(allEnrollments.map(enrollment => enrollment.semester))))
		studentSemesters.value = semesters

		if (semesters.length === 0) {
			syncingStudentSemester = true
			selectedSemester.value = null
			syncingStudentSemester = false
			studentEnrolledItems.value = []
			studentWaitlistedItems.value = []
			studentSemesterSchedules.value = []
			return
		}

		const semesterToLoad = selectedSemester.value && semesters.includes(selectedSemester.value)
			? selectedSemester.value
			: (semesters[0] ?? null)

		if (!semesterToLoad) {
			studentEnrolledItems.value = []
			studentWaitlistedItems.value = []
			return
		}

		if (selectedSemester.value !== semesterToLoad) {
			syncingStudentSemester = true
			selectedSemester.value = semesterToLoad
			syncingStudentSemester = false
		}

		const [studentSchedule, semesterSchedules] = await Promise.all([
			studentsService.getSchedule(studentId.value, semesterToLoad),
			schedulesService.getAll({ semester: semesterToLoad }),
		])
		studentSemesterSchedules.value = semesterSchedules

		const summaryByScheduleId = new Map<number, Schedule>()
		for (const schedule of semesterSchedules) {
			summaryByScheduleId.set(schedule.id, schedule)
		}

		studentEnrolledItems.value = sortSchedulesByTime(
			studentSchedule.enrolled
				.map(enrollment => enrollment.schedule)
				.filter(isDefinedSchedule)
				.map(schedule => mergeSeatSummary(schedule, summaryByScheduleId.get(schedule.id)))
		)

		studentWaitlistedItems.value = sortSchedulesByTime(
			studentSchedule.waitlisted
				.map(enrollment => enrollment.schedule)
				.filter(isDefinedSchedule)
				.map(schedule => mergeSeatSummary(schedule, summaryByScheduleId.get(schedule.id)))
		)
	} catch (cause) {
		console.error('Failed to load student schedule', cause)
		studentError.value = 'Failed to load student schedule data.'
		studentEnrolledItems.value = []
		studentWaitlistedItems.value = []
		studentSemesterSchedules.value = []
	} finally {
		studentLoading.value = false
	}
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
	} catch (cause) {
		console.error('Failed to load frictions', cause)
		frictions.value = []
		frictionsError.value = 'Could not load schedule issues'
	} finally {
		frictionsLoading.value = false
	}
}

async function refreshSchedulesAndFrictions() {
	if (role.value === 'student') {
		await fetchStudentSchedules()
		await loadRoomBookings()
		frictions.value = []
		frictionsError.value = null
		frictionsLoading.value = false
		return
	}

	await Promise.all([fetchAll(), loadRoomBookings()])
	await fetchFrictions()
}

function handleEventClick(selection: CalendarSelection) {
	selectedCalendarSelection.value = selection
	if (selection.kind === 'schedule' && role.value === 'instructor') {
		selectedIssue.value = ''
	}
	detailsModalOpen.value = true
}

function openScheduleDetails(scheduleId: number) {
	handleEventClick({
		kind: 'schedule',
		sourceId: scheduleId,
	})
}

function openRoomBookingDetails(bookingId: number) {
	handleEventClick({
		kind: 'roomBooking',
		sourceId: bookingId,
	})
}

function handleStartRequest() {
	if (role.value !== 'instructor' || !selectedSchedule.value || !selectedIssue.value) {
		return
	}

	detailsModalOpen.value = false
	router.push({
		path: '/requests/new',
		query: {
			scheduleId: String(selectedSchedule.value.id),
			issue: selectedIssue.value,
		},
	})
}

function handleDetailsModalVisibilityChange(isOpen: boolean) {
	detailsModalOpen.value = isOpen
	if (!isOpen) {
		selectedCalendarSelection.value = null
		selectedIssue.value = ''
	}
}

function handleOpenCourseFromModal() {
	if (!selectedSchedule.value) return
	detailsModalOpen.value = false
	router.push(`/courses/${selectedSchedule.value.course.id}`)
}

function handleOpenRoomFromModal() {
	if (!selectedDetailRoomId.value) return
	detailsModalOpen.value = false
	router.push(`/rooms/${selectedDetailRoomId.value}`)
}

async function handleDeleteFromModal() {
	if (role.value !== 'admin' || !selectedSchedule.value) return
	const scheduleId = selectedSchedule.value.id
	detailsModalOpen.value = false
	selectedCalendarSelection.value = null
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
		<div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
			<h1 class="text-2xl font-semibold text-gray-900">{{ pageTitle }}</h1>
			<div class="flex flex-wrap items-center gap-2">
				<select
					v-if="role !== 'student'"
					v-model="selectedBuildingId"
					aria-label="Building Filter"
					class="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700"
				>
					<option :value="null">All Buildings</option>
					<option v-for="building in buildings" :key="building.id" :value="building.id">
						{{ building.code }} - {{ building.name }}
					</option>
				</select>
				<select
					v-if="role !== 'student'"
					v-model="selectedRoomId"
					aria-label="Room Filter"
					class="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700"
				>
					<option :value="null">All Rooms</option>
					<option v-for="room in filteredRooms" :key="room.id" :value="room.id">
						{{ room.roomNumber }} ({{ room.capacity }} seats)
						{{ scheduleCountByRoom.get(room.id) ? `- ${scheduleCountByRoom.get(room.id)} classes` : '' }}
					</option>
				</select>
				<select
					v-model="selectedSemester"
					aria-label="Semester Filter"
					:disabled="role === 'student' && semesterOptions.length === 0"
					class="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700"
				>
					<option v-if="role !== 'student'" :value="null">All Semesters</option>
					<option v-else-if="semesterOptions.length === 0" :value="null" disabled>No Semesters</option>
					<option v-for="semester in semesterOptions" :key="semester" :value="semester">
						{{ semester }}
					</option>
				</select>
				<div v-if="role !== 'student'" class="flex overflow-hidden rounded border border-gray-300">
					<button
						@click="viewMode = 'calendar'"
						:class="[
							'px-3 py-1.5 text-sm',
							viewMode === 'calendar' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50',
						]"
					>
						Calendar
					</button>
					<button
						@click="viewMode = 'table'"
						:class="[
							'px-3 py-1.5 text-sm',
							viewMode === 'table' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50',
						]"
					>
						Table
					</button>
				</div>
				<div
					v-if="viewMode === 'calendar' && role === 'admin'"
					class="flex overflow-hidden rounded border border-gray-300"
				>
					<button
						@click="calendarRangeMode = 'day'"
						:class="[
							'px-3 py-1.5 text-sm',
							calendarRangeMode === 'day' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50',
						]"
					>
						Day
					</button>
					<button
						@click="calendarRangeMode = 'week'"
						:class="[
							'px-3 py-1.5 text-sm',
							calendarRangeMode === 'week' ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 hover:bg-gray-50',
						]"
					>
						Week
					</button>
				</div>
				<button
					v-if="role === 'student'"
					class="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
					:disabled="bookingSemesterOptions.length === 0"
					@click="openBookingModal"
				>
					Book Room
				</button>
				<RouterLink
					v-if="role === 'admin'"
					to="/schedules/new"
					class="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700"
				>
					Add Schedule
				</RouterLink>
			</div>
		</div>

		<div
			v-if="INSTRUCTOR_FRICTION_MVP && role === 'instructor'"
			class="mb-6 rounded border border-gray-200 bg-white"
		>
			<div class="flex items-center justify-between border-b border-gray-200 p-4">
				<div>
					<h2 class="text-sm font-semibold text-gray-900">Schedule Issues</h2>
					<p class="mt-0.5 text-xs text-gray-500 slate:text-gray-600">
						Found for {{ insightsSemester || 'current semester' }}.
					</p>
				</div>
				<button class="text-sm font-medium text-blue-700 hover:underline" @click="fetchFrictions">
					Refresh
				</button>
			</div>

			<div v-if="frictionsLoading" class="space-y-2 p-4">
				<div v-for="i in 3" :key="i" class="h-4 w-56 animate-pulse rounded bg-gray-200"></div>
			</div>
			<div v-else-if="frictionsError" class="p-4 text-sm text-red-700">{{ frictionsError }}</div>
			<div v-else-if="frictions.length === 0" class="p-4 text-sm text-gray-600">No schedule issues found.</div>
			<ul v-else class="divide-y divide-gray-100">
				<li v-for="issue in frictions.slice(0, 8)" :key="issue.id" class="flex items-start gap-4 p-4">
					<div class="min-w-0 flex-1">
						<div class="flex flex-wrap items-center gap-2">
							<span
								class="rounded px-2 py-0.5 text-xs font-medium"
								:class="frictionSeverityClass(issue.severity)"
							>
								{{ issue.severity }}
							</span>
							<span class="text-xs text-gray-500 slate:text-gray-600">
								{{ formatFrictionType(issue.type) }}
							</span>
						</div>
						<p class="mt-2 text-sm text-gray-700">{{ issue.message }}</p>
					</div>
					<RouterLink
						:to="{ path: '/requests/new', query: { scheduleId: String(issue.scheduleId), issue: issue.recommendedIssue } }"
						class="inline-flex shrink-0 items-center rounded border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
					>
						Fix
					</RouterLink>
				</li>
			</ul>
		</div>

		<div
			v-if="roomBookingsError"
			class="mb-4 rounded border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
		>
			{{ roomBookingsError }}
		</div>

		<TableSkeleton v-if="activeLoading" :columns="6" :rows="6" />
		<div v-else-if="activeError" class="text-red-600">{{ activeError }}</div>
		<template v-else-if="role === 'student'">
			<EmptyState
				v-if="!studentHasAnyRows"
				title="No classes for this semester"
				description="This semester has no enrolled or waitlisted classes yet."
				hint="Try selecting a different semester from the filter above."
			/>
			<div v-else class="space-y-6">
				<section class="rounded border border-emerald-200 bg-emerald-50/40">
					<div class="flex items-center justify-between border-b border-emerald-200 px-4 py-3">
						<h2 class="text-sm font-semibold text-emerald-900">Enrolled Classes</h2>
						<span class="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
							{{ studentEnrolledItems.length }}
						</span>
					</div>
					<div v-if="studentEnrolledItems.length === 0" class="px-4 py-3 text-sm text-gray-600">
						No enrolled classes for this semester.
					</div>
					<div v-else class="overflow-x-auto">
						<table class="w-full bg-white">
							<thead>
								<tr class="border-b border-emerald-100 bg-emerald-50/50">
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Course</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Time</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Room</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Seat Pressure</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="schedule in studentEnrolledItems" :key="schedule.id" class="border-b border-gray-100">
									<td class="px-4 py-3">
										<div class="font-medium text-gray-900">{{ schedule.course.code }}</div>
										<div class="text-sm text-gray-600">{{ schedule.course.name }}</div>
									</td>
									<td class="px-4 py-3 text-gray-600">
										{{ timeslotsService.formatTimeSlot(schedule.timeSlot) }}
									</td>
									<td class="px-4 py-3 text-gray-600">{{ formatRoom(schedule.room) }}</td>
									<td class="px-4 py-3">
										<div class="text-sm text-gray-700">{{ getSeatUtilization(schedule) }}</div>
										<div class="text-xs text-gray-500">{{ getSeatPressure(schedule) }}</div>
									</td>
									<td class="px-4 py-3">
										<button class="text-blue-600 hover:underline" @click="openScheduleDetails(schedule.id)">
											Details
										</button>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
				</section>

				<section class="rounded border border-amber-200 bg-amber-50/40">
					<div class="flex items-center justify-between border-b border-amber-200 px-4 py-3">
						<h2 class="text-sm font-semibold text-amber-900">Waitlisted Classes</h2>
						<span class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
							{{ studentWaitlistedItems.length }}
						</span>
					</div>
					<div v-if="studentWaitlistedItems.length === 0" class="px-4 py-3 text-sm text-gray-600">
						No waitlisted classes for this semester.
					</div>
					<div v-else class="overflow-x-auto">
						<table class="w-full bg-white">
							<thead>
								<tr class="border-b border-amber-100 bg-amber-50/50">
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Course</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Time</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Room</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Seat Pressure</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="schedule in studentWaitlistedItems" :key="schedule.id" class="border-b border-gray-100">
									<td class="px-4 py-3">
										<div class="flex items-center gap-2">
											<div class="font-medium text-gray-900">{{ schedule.course.code }}</div>
											<span class="rounded bg-amber-100 px-1.5 py-0.5 text-[10px] font-semibold uppercase text-amber-800">
												Waitlisted
											</span>
										</div>
										<div class="text-sm text-gray-600">{{ schedule.course.name }}</div>
									</td>
									<td class="px-4 py-3 text-gray-600">
										{{ timeslotsService.formatTimeSlot(schedule.timeSlot) }}
									</td>
									<td class="px-4 py-3 text-gray-600">{{ formatRoom(schedule.room) }}</td>
									<td class="px-4 py-3">
										<div class="text-sm text-gray-700">{{ getSeatUtilization(schedule) }}</div>
										<div class="text-xs text-gray-500">{{ getSeatPressure(schedule) }}</div>
									</td>
									<td class="px-4 py-3">
										<button class="text-blue-600 hover:underline" @click="openScheduleDetails(schedule.id)">
											Details
										</button>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
				</section>

				<section class="rounded border border-blue-200 bg-blue-50/40">
					<div class="flex items-center justify-between border-b border-blue-200 px-4 py-3">
						<div>
							<h2 class="text-sm font-semibold text-blue-900">My Room Bookings</h2>
							<p class="mt-0.5 text-xs text-blue-800/80">
								Bookings appear on the main schedule calendar for other viewers without student names.
							</p>
						</div>
						<span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800">
							{{ studentRoomBookings.length }}
						</span>
					</div>
					<div v-if="studentRoomBookings.length === 0" class="px-4 py-3 text-sm text-gray-600">
						No room bookings for this semester.
					</div>
					<div v-else class="overflow-x-auto">
						<table class="w-full bg-white">
							<thead>
								<tr class="border-b border-blue-100 bg-blue-50/50">
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Room</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Time</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Students</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Role</th>
									<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="booking in studentRoomBookings" :key="booking.id" class="border-b border-gray-100">
									<td class="px-4 py-3 text-gray-700">{{ formatRoom(booking.room) }}</td>
									<td class="px-4 py-3 text-gray-600">
										{{ timeslotsService.formatTimeSlot(booking.timeSlot) }}
									</td>
									<td class="px-4 py-3 text-gray-600">
										{{ booking.participantCount === 1 ? '1 student' : `${booking.participantCount} students` }}
									</td>
									<td class="px-4 py-3 text-gray-600">
										{{ booking.viewerIsOwner ? 'Owner' : 'Participant' }}
									</td>
									<td class="px-4 py-3">
										<button class="text-blue-600 hover:underline" @click="openRoomBookingDetails(booking.id)">
											Details
										</button>
									</td>
								</tr>
							</tbody>
						</table>
					</div>
				</section>
			</div>
		</template>
		<template v-else>
			<EmptyState
				v-if="viewMode === 'calendar' ? calendarEntries.length === 0 : filteredItems.length === 0"
				title="No schedules yet"
				description="Schedules connect courses to rooms and time slots. Create your first schedule manually or use the auto-scheduler."
				:action-label="role === 'admin' ? 'Add Schedule' : undefined"
				:action-route="role === 'admin' ? '/schedules/new' : undefined"
				:secondary-label="role === 'admin' ? 'Use Solver' : undefined"
				:secondary-route="role === 'admin' ? '/solver' : undefined"
				hint="The solver can generate an optimized schedule for all your courses at once."
			/>

			<AdminDailyScheduleGrid
				v-else-if="viewMode === 'calendar' && role === 'admin' && calendarRangeMode === 'day'"
				:entries="calendarEntries"
				:buildings="buildings"
				:rooms="rooms"
				:selected-building-id="selectedBuildingId"
				:selected-room-id="selectedRoomId"
				@event-click="handleEventClick"
				@building-drilldown="handleBuildingDrilldown"
			/>
			<ScheduleCalendar
				v-else-if="viewMode === 'calendar'"
				:entries="calendarEntries"
				:view-mode="calendarRangeMode"
				:week-days="5"
				:height="role === 'admin' ? 920 : undefined"
				:day-start="role === 'admin' ? '08:00' : undefined"
				:day-end="role === 'admin' ? '21:00' : undefined"
				:grid-step="role === 'admin' ? 30 : undefined"
				:event-width="role === 'admin' && calendarRangeMode === 'day' ? 100 : 95"
				@event-click="handleEventClick"
			/>

			<div v-else class="overflow-x-auto">
				<table class="w-full border border-gray-200 bg-white">
					<thead>
						<tr class="border-b border-gray-200 bg-gray-50">
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Course</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Room</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Time</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Semester</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Seats</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Waitlist</th>
							<th class="px-4 py-3 text-left text-sm font-medium text-gray-700">Actions</th>
						</tr>
					</thead>
					<tbody>
						<tr v-for="schedule in filteredItems" :key="schedule.id" class="border-b border-gray-100">
							<td class="px-4 py-3">
								<RouterLink :to="`/courses/${schedule.course.id}`" class="text-blue-600 hover:underline">
									{{ schedule.course.code }}
								</RouterLink>
								<span class="ml-1 text-gray-500">{{ schedule.course.name }}</span>
							</td>
							<td class="px-4 py-3">
								<RouterLink :to="`/rooms/${schedule.room.id}`" class="text-blue-600 hover:underline">
									{{ formatRoom(schedule.room) }}
								</RouterLink>
							</td>
							<td class="px-4 py-3 text-gray-600">
								{{ timeslotsService.formatTimeSlot(schedule.timeSlot) }}
							</td>
							<td class="px-4 py-3 text-gray-600">{{ schedule.semester }}</td>
							<td class="px-4 py-3">
								<div class="text-sm text-gray-700">{{ getSeatUtilization(schedule) }}</div>
								<div class="text-xs text-gray-500">{{ getSeatPressure(schedule) }}</div>
							</td>
							<td class="px-4 py-3 text-gray-600">{{ getWaitlistSummary(schedule) }}</td>
							<td class="px-4 py-3">
								<button class="mr-3 text-blue-600 hover:underline" @click="openScheduleDetails(schedule.id)">
									Details
								</button>
								<button
									v-if="role === 'admin'"
									@click="handleDelete(schedule.id)"
									class="text-red-600 hover:underline"
								>
									Delete
								</button>
							</td>
						</tr>
					</tbody>
				</table>
			</div>
		</template>

		<ScheduleSelectionModal
			:model-value="detailsModalOpen"
			:title="detailModalTitle"
			:role="role"
			:selected-schedule="selectedSchedule"
			:selected-room-booking="selectedRoomBooking"
			:selected-student-status="selectedStudentStatus"
			:selected-issue="selectedIssue"
			@update:model-value="handleDetailsModalVisibilityChange"
			@update:selected-issue="selectedIssue = $event"
			@open-course="handleOpenCourseFromModal"
			@open-room="handleOpenRoomFromModal"
			@delete-schedule="handleDeleteFromModal"
			@start-request="handleStartRequest"
			@export-single-class="handleExportSingleClass"
			@export-class-for-semester="handleExportClassForSemester"
		/>

		<RoomBookingModal
			:model-value="bookingModalOpen"
			:error="bookingError"
			:saving="bookingSaving"
			:semester-options="bookingSemesterOptions"
			:semester="bookingForm.semester"
			:time-slots="sortedTimeSlots"
			:time-slot-id="bookingForm.timeSlotId"
			:available-rooms="availableBookingRooms"
			:room-id="bookingForm.roomId"
			:participant-search-query="participantSearchQuery"
			:participant-search-loading="participantSearchLoading"
			:participant-search-error="participantSearchError"
			:participant-suggestions="participantSuggestions"
			:selected-participants="selectedParticipants"
			:selected-booking-time-slot="selectedBookingTimeSlot"
			:can-submit="canSubmitBooking"
			@update:model-value="setBookingModalOpen"
			@update:semester="bookingForm.semester = $event"
			@update:time-slot-id="bookingForm.timeSlotId = $event"
			@update:room-id="bookingForm.roomId = $event"
			@update:participant-search-query="participantSearchQuery = $event"
			@add-participant="addParticipant"
			@remove-participant="removeParticipant"
			@submit="submitRoomBooking"
		/>
	</div>
</template>
