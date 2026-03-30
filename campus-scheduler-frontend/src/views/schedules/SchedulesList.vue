<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { toast } from 'vue3-toastify'
import { useCrud } from '@/composables/useCrud'
import { schedulesService, type Schedule } from '@/services/schedules'
import { roomsService, type Room } from '@/services/rooms'
import { buildingsService, type Building } from '@/services/buildings'
import {
	DAY_OF_WEEK_OPTIONS,
	timeslotsService,
	type DayOfWeek,
	type TimeSlot,
} from '@/services/timeslots'
import { useRole } from '@/composables/useRole'
import { studentsService } from '@/services/students'
import { enrollmentsService, type EnrollmentStatus } from '@/services/enrollments'
import {
	roomBookingsService,
	type RoomBooking,
	type RoomBookingStudentLookupResponse,
} from '@/services/roomBookings'
import ScheduleCalendar from '@/components/calendar/ScheduleCalendar.vue'
import AdminDailyScheduleGrid from '@/components/calendar/AdminDailyScheduleGrid.vue'
import {
	toRoomBookingCalendarEntry,
	toScheduleCalendarEntry,
	type CalendarSelection,
} from '@/components/calendar/types'
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
import { formatFrictionType, frictionSeverityClass } from '@/utils/friction'

type ViewMode = 'table' | 'calendar'
type CalendarRangeMode = 'day' | 'week'

const DAY_ORDER: Record<DayOfWeek, number> = {
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
	SUNDAY: 7,
}

const PARTICIPANT_SEARCH_DEBOUNCE_MS = 250

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
const roomBookings = ref<RoomBooking[]>([])
const roomBookingsLoading = ref(false)
const roomBookingsError = ref<string | null>(null)

const detailsModalOpen = ref(false)
const selectedCalendarSelection = ref<CalendarSelection | null>(null)
const selectedIssue = ref<ChangeRequestIssue | ''>('')

const bookingModalOpen = ref(false)
const bookingSaving = ref(false)
const bookingError = ref<string | null>(null)
const bookingForm = ref({
	semester: '',
	timeSlotId: null as number | null,
	roomId: null as number | null,
})
const selectedParticipants = ref<RoomBookingStudentLookupResponse[]>([])
const participantSearchQuery = ref('')
const participantSearchResults = ref<RoomBookingStudentLookupResponse[]>([])
const participantSearchLoading = ref(false)
const participantSearchError = ref<string | null>(null)

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

function sortSemesters(semesters: string[]): string[] {
	return semesters
		.filter(semester => semester.trim().length > 0)
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
}

function compareByTimeSlot(a: { timeSlot: TimeSlot }, b: { timeSlot: TimeSlot }): number {
	const dayA = DAY_ORDER[a.timeSlot.dayOfWeek] ?? Number.MAX_SAFE_INTEGER
	const dayB = DAY_ORDER[b.timeSlot.dayOfWeek] ?? Number.MAX_SAFE_INTEGER
	if (dayA !== dayB) {
		return dayA - dayB
	}
	if (a.timeSlot.startTime !== b.timeSlot.startTime) {
		return a.timeSlot.startTime.localeCompare(b.timeSlot.startTime)
	}
	if (a.timeSlot.endTime !== b.timeSlot.endTime) {
		return a.timeSlot.endTime.localeCompare(b.timeSlot.endTime)
	}
	return 0
}

function compareTimeSlots(a: TimeSlot, b: TimeSlot): number {
	return compareByTimeSlot({ timeSlot: a }, { timeSlot: b })
}

function sortSchedulesByTime(schedules: Schedule[]): Schedule[] {
	return [...schedules].sort((a, b) => {
		const timeComparison = compareByTimeSlot(a, b)
		if (timeComparison !== 0) {
			return timeComparison
		}
		return a.course.code.localeCompare(b.course.code)
	})
}

function sortRoomBookingsByTime(bookings: RoomBooking[]): RoomBooking[] {
	return [...bookings].sort((a, b) => {
		const semesterComparison = a.semester.localeCompare(b.semester)
		if (semesterComparison !== 0) {
			return semesterComparison
		}

		const timeComparison = compareByTimeSlot(a, b)
		if (timeComparison !== 0) {
			return timeComparison
		}

		const buildingA = a.room.buildingCode ?? ''
		const buildingB = b.room.buildingCode ?? ''
		if (buildingA !== buildingB) {
			return buildingA.localeCompare(buildingB)
		}

		if (a.room.roomNumber !== b.room.roomNumber) {
			return a.room.roomNumber.localeCompare(b.room.roomNumber)
		}

		return a.id - b.id
	})
}

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

const sortedTimeSlots = computed(() =>
	[...timeSlots.value].sort((a, b) => {
		const timeComparison = compareTimeSlots(a, b)
		if (timeComparison !== 0) {
			return timeComparison
		}
		return a.id - b.id
	})
)

const bookingSemesterOptions = computed(() => {
	if (studentSemesters.value.length > 0) {
		return studentSemesters.value
	}
	return selectedSemester.value ? [selectedSemester.value] : []
})

const selectedBookingTimeSlot = computed(() =>
	timeSlots.value.find(timeSlot => timeSlot.id === bookingForm.value.timeSlotId) ?? null
)

const blockedScheduleRoomIds = computed(() => {
	if (!bookingForm.value.semester || !bookingForm.value.timeSlotId) {
		return new Set<number>()
	}

	const schedulesToCheck = role.value === 'student'
		? studentSemesterSchedules.value
		: hydratedItems.value

	return new Set(
		schedulesToCheck
			.filter(schedule =>
				schedule.semester === bookingForm.value.semester
				&& schedule.timeSlot.id === bookingForm.value.timeSlotId
			)
			.map(schedule => schedule.room.id)
	)
})

const blockedRoomBookingRoomIds = computed(() => {
	if (!bookingForm.value.semester || !bookingForm.value.timeSlotId) {
		return new Set<number>()
	}

	return new Set(
		roomBookings.value
			.filter(booking =>
				booking.semester === bookingForm.value.semester
				&& booking.timeSlot.id === bookingForm.value.timeSlotId
			)
			.map(booking => booking.room.id)
	)
})

const availableBookingRooms = computed(() =>
	rooms.value
		.filter(room => room.availabilityStatus === 'AVAILABLE')
		.filter(room => !blockedScheduleRoomIds.value.has(room.id))
		.filter(room => !blockedRoomBookingRoomIds.value.has(room.id))
		.sort((a, b) => {
			const buildingA = a.buildingCode ?? ''
			const buildingB = b.buildingCode ?? ''
			if (buildingA !== buildingB) {
				return buildingA.localeCompare(buildingB)
			}
			return a.roomNumber.localeCompare(b.roomNumber)
		})
)

const canSubmitBooking = computed(() =>
	!!studentId.value
	&& !!bookingForm.value.semester
	&& !!bookingForm.value.timeSlotId
	&& !!bookingForm.value.roomId
	&& !bookingSaving.value
)

const selectedParticipantIds = computed(() =>
	new Set(selectedParticipants.value.map(participant => participant.id))
)

const participantSuggestions = computed(() => {
	const ownerId = studentId.value
	return participantSearchResults.value.filter(candidate =>
		candidate.id !== ownerId && !selectedParticipantIds.value.has(candidate.id)
	)
})

const participantSearchReady = computed(() =>
	bookingModalOpen.value
	&& !!studentId.value
	&& !!bookingForm.value.semester
	&& !!bookingForm.value.timeSlotId
)

let syncingStudentSemester = false
let participantSearchTimer: ReturnType<typeof setTimeout> | null = null
let participantSearchRequestId = 0

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

watch(availableBookingRooms, (nextRooms) => {
	if (bookingForm.value.roomId && !nextRooms.some(room => room.id === bookingForm.value.roomId)) {
		bookingForm.value.roomId = null
	}
})

watch(
	[
		participantSearchQuery,
		() => bookingForm.value.semester,
		() => bookingForm.value.timeSlotId,
		studentId,
		bookingModalOpen,
	],
	() => {
		void scheduleParticipantSearch()
	}
)

onMounted(async () => {
	await loadReferenceData()
	await refreshSchedulesAndFrictions()
})

onBeforeUnmount(() => {
	clearParticipantSearchTimer()
})

function formatRoom(room: Room): string {
	const parts = [room.buildingCode, room.roomNumber].filter(Boolean)
	return parts.join(' ') || 'Room unavailable'
}

function getSemesterValueForStudent(): string | null {
	if (selectedSemester.value && bookingSemesterOptions.value.includes(selectedSemester.value)) {
		return selectedSemester.value
	}
	return bookingSemesterOptions.value[0] ?? null
}

function resetBookingForm() {
	bookingForm.value = {
		semester: getSemesterValueForStudent() ?? '',
		timeSlotId: null,
		roomId: null,
	}
	selectedParticipants.value = []
	participantSearchQuery.value = ''
	participantSearchResults.value = []
	participantSearchError.value = null
	bookingError.value = null
}

function getErrorMessage(cause: unknown, fallback: string): string {
	const errorLike = cause as {
		response?: { data?: { error?: string } }
		message?: string
	}
	return errorLike.response?.data?.error ?? errorLike.message ?? fallback
}

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

async function fetchRoomBookings() {
	roomBookingsLoading.value = true
	roomBookingsError.value = null

	try {
		if (role.value === 'student') {
			if (!studentId.value || !selectedSemester.value) {
				roomBookings.value = []
				return
			}

			roomBookings.value = await roomBookingsService.getAll({
				semester: selectedSemester.value,
				studentId: studentId.value,
			})
			return
		}

		roomBookings.value = await roomBookingsService.getAll({
			semester: selectedSemester.value ?? undefined,
		})
	} catch (cause) {
		console.error('Failed to load room bookings', cause)
		roomBookingsError.value = 'Failed to load room bookings.'
		roomBookings.value = []
	} finally {
		roomBookingsLoading.value = false
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
		await fetchRoomBookings()
		frictions.value = []
		frictionsError.value = null
		frictionsLoading.value = false
		return
	}

	await Promise.all([fetchAll(), fetchRoomBookings()])
	await fetchFrictions()
}

function clearParticipantSearchTimer() {
	if (participantSearchTimer !== null) {
		clearTimeout(participantSearchTimer)
		participantSearchTimer = null
	}
}

async function runParticipantSearch(requestId: number) {
	if (!participantSearchReady.value) {
		participantSearchResults.value = []
		participantSearchLoading.value = false
		return
	}

	const query = participantSearchQuery.value.trim()
	if (query.length < 2) {
		participantSearchResults.value = []
		participantSearchError.value = null
		participantSearchLoading.value = false
		return
	}

	participantSearchLoading.value = true
	participantSearchError.value = null

	try {
		const matches = await roomBookingsService.searchStudents(
			query,
			bookingForm.value.semester,
			bookingForm.value.timeSlotId as number,
			[
				studentId.value as number,
				...selectedParticipants.value.map(participant => participant.id),
			]
		)

		if (requestId !== participantSearchRequestId) {
			return
		}

		participantSearchResults.value = matches
	} catch (cause) {
		if (requestId !== participantSearchRequestId) {
			return
		}
		console.error('Failed to search participants', cause)
		participantSearchResults.value = []
		participantSearchError.value = 'Could not search student email addresses.'
	} finally {
		if (requestId === participantSearchRequestId) {
			participantSearchLoading.value = false
		}
	}
}

async function scheduleParticipantSearch() {
	clearParticipantSearchTimer()
	participantSearchRequestId += 1
	const requestId = participantSearchRequestId

	if (!participantSearchReady.value || participantSearchQuery.value.trim().length < 2) {
		participantSearchResults.value = []
		participantSearchError.value = null
		participantSearchLoading.value = false
		return
	}

	participantSearchTimer = setTimeout(() => {
		void runParticipantSearch(requestId)
	}, PARTICIPANT_SEARCH_DEBOUNCE_MS)
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

function handleBookingModalVisibilityChange(isOpen: boolean) {
	bookingModalOpen.value = isOpen
	if (!isOpen) {
		resetBookingForm()
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
	if (typeof schedule.filledSeats !== 'number' || typeof schedule.seatLimit !== 'number') {
		return 'Seat data unavailable'
	}
	if (schedule.seatLimit <= 0) {
		return `${schedule.filledSeats} enrolled`
	}
	const percent = Math.round((schedule.filledSeats / schedule.seatLimit) * 100)
	return `${schedule.filledSeats}/${schedule.seatLimit} seats (${percent}%)`
}

function getSeatPressure(schedule: Schedule): string {
	if (typeof schedule.remainingSeats !== 'number' || typeof schedule.waitlistCount !== 'number') {
		return 'Seat pressure unavailable'
	}
	return `${schedule.remainingSeats} seats left, ${schedule.waitlistCount} waitlisted`
}

function getWaitlistSummary(schedule: Schedule): string {
	if (typeof schedule.waitlistCount !== 'number') {
		return 'N/A'
	}
	return `${schedule.waitlistCount}`
}

function getStudentStatusLabel(status: EnrollmentStatus | null): string {
	if (status === 'ENROLLED') return 'Enrolled'
	if (status === 'WAITLISTED') return 'Waitlisted'
	return 'Unknown'
}

function getParticipantTotalLabel(count: number): string {
	return count === 1 ? '1 student' : `${count} students`
}

function getBookingPrivacyMessage(booking: RoomBooking): string | null {
	if (booking.viewerCanSeeStudentDetails) {
		return null
	}
	if (booking.viewerIsParticipant) {
		return 'You are included in this booking. Student details are only visible to the booking owner and admins.'
	}
	return 'Student details are hidden for this booking.'
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

function openBookingModal() {
	if (!studentId.value) {
		toast.error('Select a student profile before creating a room booking.')
		return
	}

	if (bookingSemesterOptions.value.length === 0) {
		toast.error('No semester is available for room booking yet.')
		return
	}

	resetBookingForm()
	bookingModalOpen.value = true
}

function addParticipant(participant: RoomBookingStudentLookupResponse) {
	if (selectedParticipantIds.value.has(participant.id)) {
		return
	}

	selectedParticipants.value = [...selectedParticipants.value, participant]
	participantSearchQuery.value = ''
	participantSearchResults.value = []
	participantSearchError.value = null
}

function removeParticipant(participantId: number) {
	selectedParticipants.value = selectedParticipants.value.filter(
		participant => participant.id !== participantId
	)
}

async function submitRoomBooking() {
	if (!canSubmitBooking.value || !studentId.value) {
		return
	}

	bookingSaving.value = true
	bookingError.value = null

	try {
		await roomBookingsService.create({
			studentId: studentId.value,
			semester: bookingForm.value.semester,
			timeSlotId: bookingForm.value.timeSlotId as number,
			roomId: bookingForm.value.roomId as number,
			participantEmails: selectedParticipants.value.map(participant => participant.email),
		})

		await fetchRoomBookings()
		bookingModalOpen.value = false
		resetBookingForm()
		toast.success('Room booking created')
	} catch (cause) {
		const message = getErrorMessage(cause, 'Failed to create room booking.')
		bookingError.value = message
		toast.error(message)
	} finally {
		bookingSaving.value = false
	}
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
										{{ getParticipantTotalLabel(booking.participantCount) }}
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

		<BaseModal
			:model-value="detailsModalOpen"
			:title="detailModalTitle"
			size="lg"
			@update:model-value="handleDetailsModalVisibilityChange"
		>
			<div v-if="selectedSchedule" class="space-y-3 text-sm text-gray-700">
				<div class="text-base font-medium text-gray-900">
					{{ selectedSchedule.course.code }} - {{ selectedSchedule.course.name }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Time:</span>
					{{ timeslotsService.formatTimeSlot(selectedSchedule.timeSlot) }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Room:</span>
					{{ formatRoom(selectedSchedule.room) }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Semester:</span>
					{{ selectedSchedule.semester }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Seat utilization:</span>
					{{ getSeatUtilization(selectedSchedule) }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Seat pressure:</span>
					{{ getSeatPressure(selectedSchedule) }}
				</div>
				<div v-if="role === 'admin'">
					<span class="font-medium text-gray-900">Instructor:</span>
					{{ getInstructorName(selectedSchedule) }}
				</div>
				<div v-if="role === 'student'">
					<span class="font-medium text-gray-900">Status:</span>
					{{ getStudentStatusLabel(selectedStudentStatus) }}
				</div>

				<template v-if="role === 'instructor'">
					<div class="mt-1 border-t border-gray-200 pt-3">
						<p class="mb-1 text-xs font-medium text-gray-700">Add to Calendar</p>
						<p class="mb-2 text-xs text-gray-500">
							Download an .ics file to import into Google Calendar, Outlook, or Apple Calendar.
						</p>
						<div class="flex flex-wrap gap-2">
							<button
								v-tooltip="'One-time event for the next upcoming session of this class'"
								class="inline-flex items-center gap-1.5 rounded border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
								@click="handleExportSingleClass"
							>
								<svg
									xmlns="http://www.w3.org/2000/svg"
									class="h-3.5 w-3.5 text-gray-500"
									viewBox="0 0 20 20"
									fill="currentColor"
									aria-hidden="true"
								>
									<path
										fill-rule="evenodd"
										d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
										clip-rule="evenodd"
									/>
								</svg>
								Next Session
							</button>
							<button
								v-tooltip="'Weekly recurring events for this class through the end of the semester'"
								class="inline-flex items-center gap-1.5 rounded border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
								@click="handleExportClassForSemester"
							>
								<svg
									xmlns="http://www.w3.org/2000/svg"
									class="h-3.5 w-3.5 text-gray-500"
									viewBox="0 0 20 20"
									fill="currentColor"
									aria-hidden="true"
								>
									<path
										fill-rule="evenodd"
										d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
										clip-rule="evenodd"
									/>
								</svg>
								Full Semester
							</button>
						</div>
					</div>

					<div class="mt-1 border-t border-gray-200 pt-3">
						<p class="mb-1 text-xs font-medium text-gray-700">Request a Change</p>
						<p class="mb-2 text-xs text-gray-600">
							Start a change request for this class. You can refine the details on the next step.
						</p>
						<div>
							<label for="request-change-issue" class="mb-1 block text-sm font-medium text-gray-700">
								Why is this a problem?
							</label>
							<select
								id="request-change-issue"
								v-model="selectedIssue"
								aria-label="Request Issue"
								class="w-full rounded border border-gray-300 px-3 py-2"
							>
								<option value="" disabled>Select a reason</option>
								<option
									v-for="option in changeRequestIssueOptions"
									:key="option.value"
									:value="option.value"
								>
									{{ option.label }}
								</option>
							</select>
						</div>
					</div>
				</template>
			</div>

			<div v-else-if="selectedRoomBooking" class="space-y-3 text-sm text-gray-700">
				<div class="text-base font-medium text-gray-900">Room Booking</div>
				<div>
					<span class="font-medium text-gray-900">Time:</span>
					{{ timeslotsService.formatTimeSlot(selectedRoomBooking.timeSlot) }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Room:</span>
					{{ formatRoom(selectedRoomBooking.room) }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Semester:</span>
					{{ selectedRoomBooking.semester }}
				</div>
				<div>
					<span class="font-medium text-gray-900">Students:</span>
					{{ getParticipantTotalLabel(selectedRoomBooking.participantCount) }}
				</div>
				<div v-if="selectedRoomBooking.viewerCanSeeStudentDetails && selectedRoomBooking.bookedBy">
					<span class="font-medium text-gray-900">Booked by:</span>
					{{ selectedRoomBooking.bookedBy.fullName }} ({{ selectedRoomBooking.bookedBy.email }})
				</div>
				<div v-if="selectedRoomBooking.viewerCanSeeStudentDetails">
					<span class="font-medium text-gray-900">Invited students:</span>
					<div v-if="selectedRoomBooking.participants.length === 0" class="mt-1 text-gray-500">
						No invited students.
					</div>
					<ul v-else class="mt-1 space-y-1">
						<li
							v-for="participant in selectedRoomBooking.participants"
							:key="participant.id"
							class="rounded border border-gray-200 bg-gray-50 px-3 py-2"
						>
							<div class="font-medium text-gray-900">{{ participant.fullName }}</div>
							<div class="text-xs text-gray-600">{{ participant.email }}</div>
						</li>
					</ul>
				</div>
				<div
					v-else-if="getBookingPrivacyMessage(selectedRoomBooking)"
					class="rounded border border-gray-200 bg-gray-50 px-3 py-2 text-gray-600"
				>
					{{ getBookingPrivacyMessage(selectedRoomBooking) }}
				</div>
				<div
					v-if="selectedRoomBooking.viewerIsParticipant && !selectedRoomBooking.viewerIsOwner"
					class="rounded border border-blue-200 bg-blue-50 px-3 py-2 text-blue-800"
				>
					You are included in this booking.
				</div>
			</div>

			<div v-else class="text-sm text-gray-600">
				{{ role === 'instructor'
					? 'Select a class to request a change.'
					: 'Select an item to view details.' }}
			</div>

			<template #footer>
				<div v-if="role === 'admin' && selectedSchedule" class="flex justify-end gap-2">
					<button class="rounded border border-gray-300 px-4 py-2" @click="handleDetailsModalVisibilityChange(false)">
						Close
					</button>
					<button
						class="rounded border border-gray-300 px-4 py-2 disabled:opacity-50"
						:disabled="!selectedSchedule"
						@click="handleOpenCourseFromModal"
					>
						View Course
					</button>
					<button
						class="rounded border border-gray-300 px-4 py-2 disabled:opacity-50"
						:disabled="!selectedDetailRoomId"
						@click="handleOpenRoomFromModal"
					>
						View Room
					</button>
					<button
						class="rounded bg-red-600 px-4 py-2 text-white disabled:opacity-50"
						:disabled="!selectedSchedule"
						@click="handleDeleteFromModal"
					>
						Delete
					</button>
				</div>
				<div v-else-if="role === 'admin' && selectedRoomBooking" class="flex justify-end gap-2">
					<button class="rounded border border-gray-300 px-4 py-2" @click="handleDetailsModalVisibilityChange(false)">
						Close
					</button>
					<button
						class="rounded border border-gray-300 px-4 py-2 disabled:opacity-50"
						:disabled="!selectedDetailRoomId"
						@click="handleOpenRoomFromModal"
					>
						View Room
					</button>
				</div>
				<div v-else-if="role === 'instructor' && selectedSchedule" class="flex justify-end gap-2">
					<button class="rounded border border-gray-300 px-4 py-2" @click="handleDetailsModalVisibilityChange(false)">
						Cancel
					</button>
					<button
						class="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
						:disabled="!selectedIssue"
						@click="handleStartRequest"
					>
						Request Change
					</button>
				</div>
				<div v-else class="flex justify-end gap-2">
					<button class="rounded border border-gray-300 px-4 py-2" @click="handleDetailsModalVisibilityChange(false)">
						Close
					</button>
				</div>
			</template>
		</BaseModal>

		<BaseModal
			:model-value="bookingModalOpen"
			title="Book Room"
			size="lg"
			@update:model-value="handleBookingModalVisibilityChange"
		>
			<div class="space-y-4">
				<div v-if="bookingError" class="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
					{{ bookingError }}
				</div>

				<div class="grid gap-4 md:grid-cols-2">
					<div>
						<label for="booking-semester" class="mb-1 block text-sm font-medium text-gray-700">
							Semester
						</label>
						<select
							id="booking-semester"
							v-model="bookingForm.semester"
							class="w-full rounded border border-gray-300 px-3 py-2"
						>
							<option value="" disabled>Select semester</option>
							<option v-for="semester in bookingSemesterOptions" :key="semester" :value="semester">
								{{ semester }}
							</option>
						</select>
					</div>
					<div>
						<label for="booking-timeslot" class="mb-1 block text-sm font-medium text-gray-700">
							Time Slot
						</label>
						<select
							id="booking-timeslot"
							v-model="bookingForm.timeSlotId"
							class="w-full rounded border border-gray-300 px-3 py-2"
						>
							<option :value="null" disabled>Select time slot</option>
							<option v-for="timeSlot in sortedTimeSlots" :key="timeSlot.id" :value="timeSlot.id">
								{{ timeslotsService.formatTimeSlot(timeSlot) }}
							</option>
						</select>
					</div>
				</div>

				<div>
					<label for="booking-room" class="mb-1 block text-sm font-medium text-gray-700">Room</label>
					<select
						id="booking-room"
						v-model="bookingForm.roomId"
						class="w-full rounded border border-gray-300 px-3 py-2"
						:disabled="!bookingForm.semester || !bookingForm.timeSlotId"
					>
						<option :value="null" disabled>Select room</option>
						<option v-for="room in availableBookingRooms" :key="room.id" :value="room.id">
							{{ formatRoom(room) }} ({{ room.capacity }} seats)
						</option>
					</select>
					<p
						v-if="bookingForm.semester && bookingForm.timeSlotId && availableBookingRooms.length === 0"
						class="mt-2 text-sm text-gray-600"
					>
						No available rooms remain for this time slot.
					</p>
					<p v-else class="mt-2 text-xs text-gray-500">
						Only rooms that are available and unused by classes or other student bookings are listed.
					</p>
				</div>

				<div class="rounded border border-gray-200 bg-gray-50 p-4">
					<div class="mb-2">
						<h3 class="text-sm font-semibold text-gray-900">Invite Students</h3>
						<p class="mt-1 text-xs text-gray-500">
							Search by student email. Suggestions show the student name and whether they already have a class during this period.
						</p>
					</div>

					<label for="participant-search" class="mb-1 block text-sm font-medium text-gray-700">
						Student Email
					</label>
					<input
						id="participant-search"
						v-model="participantSearchQuery"
						type="text"
						placeholder="Start typing a student email"
						class="w-full rounded border border-gray-300 px-3 py-2"
						:disabled="!bookingForm.semester || !bookingForm.timeSlotId"
					/>

					<div v-if="participantSearchError" class="mt-2 text-sm text-red-600">
						{{ participantSearchError }}
					</div>
					<div v-else-if="participantSearchLoading" class="mt-2 text-sm text-gray-500">
						Searching students...
					</div>
					<div
						v-else-if="participantSearchQuery.trim().length >= 2 && participantSuggestions.length === 0"
						class="mt-2 text-sm text-gray-500"
					>
						No matching students found.
					</div>

					<ul v-if="participantSuggestions.length > 0" class="mt-3 space-y-2">
						<li
							v-for="candidate in participantSuggestions"
							:key="candidate.id"
							class="flex flex-col gap-2 rounded border border-gray-200 bg-white px-3 py-2 sm:flex-row sm:items-center sm:justify-between"
						>
							<div>
								<div class="font-medium text-gray-900">{{ candidate.fullName }}</div>
								<div class="text-xs text-gray-600">{{ candidate.email }}</div>
								<div class="mt-1 text-xs" :class="candidate.hasClassDuringPeriod ? 'text-amber-700' : 'text-emerald-700'">
									{{ candidate.hasClassDuringPeriod ? 'Has classes during this period' : 'No class conflict reported for this period' }}
								</div>
							</div>
							<button
								class="rounded border border-blue-300 px-3 py-1.5 text-sm text-blue-700 hover:bg-blue-50"
								@click="addParticipant(candidate)"
							>
								Add
							</button>
						</li>
					</ul>

					<div class="mt-4">
						<h4 class="text-sm font-medium text-gray-900">Selected Students</h4>
						<div v-if="selectedParticipants.length === 0" class="mt-2 text-sm text-gray-500">
							No invited students yet.
						</div>
						<ul v-else class="mt-2 space-y-2">
							<li
								v-for="participant in selectedParticipants"
								:key="participant.id"
								class="flex items-center justify-between rounded border border-gray-200 bg-white px-3 py-2"
							>
								<div>
									<div class="font-medium text-gray-900">{{ participant.fullName }}</div>
									<div class="text-xs text-gray-600">{{ participant.email }}</div>
								</div>
								<button
									class="text-sm text-red-600 hover:underline"
									@click="removeParticipant(participant.id)"
								>
									Remove
								</button>
							</li>
						</ul>
					</div>
				</div>

				<div
					v-if="selectedBookingTimeSlot"
					class="rounded border border-blue-200 bg-blue-50 px-3 py-2 text-sm text-blue-900"
				>
					Booking for {{ bookingForm.semester }} on
					{{ DAY_OF_WEEK_OPTIONS.find(option => option.value === selectedBookingTimeSlot?.dayOfWeek)?.label }}
					{{ timeslotsService.formatTime(selectedBookingTimeSlot.startTime) }} -
					{{ timeslotsService.formatTime(selectedBookingTimeSlot.endTime) }}.
				</div>
			</div>

			<template #footer>
				<div class="flex justify-end gap-2">
					<button class="rounded border border-gray-300 px-4 py-2" @click="handleBookingModalVisibilityChange(false)">
						Cancel
					</button>
					<button
						class="rounded bg-blue-600 px-4 py-2 text-white disabled:cursor-not-allowed disabled:opacity-50"
						:disabled="!canSubmitBooking"
						@click="submitRoomBooking"
					>
						Create Booking
					</button>
				</div>
			</template>
		</BaseModal>
	</div>
</template>
