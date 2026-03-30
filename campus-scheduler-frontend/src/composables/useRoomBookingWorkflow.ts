import { computed, onBeforeUnmount, ref, watch, type ComputedRef, type Ref } from 'vue'
import { toast } from 'vue3-toastify'
import type { Role } from '@/composables/useRole'
import {
	roomBookingsService,
	type RoomBooking,
	type RoomBookingStudentLookupResponse,
} from '@/services/roomBookings'
import type { Room } from '@/services/rooms'
import type { Schedule } from '@/services/schedules'
import type { TimeSlot } from '@/services/timeslots'
import { compareTimeSlots } from '@/views/schedules/helpers'

const PARTICIPANT_SEARCH_DEBOUNCE_MS = 250

type MaybeReadonlyRef<T> = Ref<T> | ComputedRef<T>

interface UseRoomBookingWorkflowOptions {
	role: Ref<Role>
	studentId: Ref<number | null>
	selectedSemester: Ref<string | null>
	rooms: Ref<Room[]>
	timeSlots: Ref<TimeSlot[]>
	studentSemesters: Ref<string[]>
	scheduleAvailabilitySource: MaybeReadonlyRef<Schedule[]>
	studentSemesterSchedules: Ref<Schedule[]>
}

export function useRoomBookingWorkflow(options: UseRoomBookingWorkflowOptions) {
	const roomBookings = ref<RoomBooking[]>([])
	const roomBookingsLoading = ref(false)
	const roomBookingsError = ref<string | null>(null)

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

	const bookingSemesterOptions = computed(() => {
		if (options.studentSemesters.value.length > 0) {
			return options.studentSemesters.value
		}
		return options.selectedSemester.value ? [options.selectedSemester.value] : []
	})

	const sortedTimeSlots = computed(() =>
		[...options.timeSlots.value].sort((a, b) => {
			const timeComparison = compareTimeSlots(a, b)
			if (timeComparison !== 0) {
				return timeComparison
			}
			return a.id - b.id
		})
	)

	const selectedBookingTimeSlot = computed(() =>
		options.timeSlots.value.find(timeSlot => timeSlot.id === bookingForm.value.timeSlotId) ?? null
	)

	const blockedScheduleRoomIds = computed(() => {
		if (!bookingForm.value.semester || !bookingForm.value.timeSlotId) {
			return new Set<number>()
		}

		const schedulesToCheck = options.role.value === 'student'
			? options.studentSemesterSchedules.value
			: options.scheduleAvailabilitySource.value

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
		options.rooms.value
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
		!!options.studentId.value
		&& !!bookingForm.value.semester
		&& !!bookingForm.value.timeSlotId
		&& !!bookingForm.value.roomId
		&& !bookingSaving.value
	)

	const selectedParticipantIds = computed(() =>
		new Set(selectedParticipants.value.map(participant => participant.id))
	)

	const participantSuggestions = computed(() => {
		const ownerId = options.studentId.value
		return participantSearchResults.value.filter(candidate =>
			candidate.id !== ownerId && !selectedParticipantIds.value.has(candidate.id)
		)
	})

	const participantSearchReady = computed(() =>
		bookingModalOpen.value
		&& !!options.studentId.value
		&& !!bookingForm.value.semester
		&& !!bookingForm.value.timeSlotId
	)

	let participantSearchTimer: ReturnType<typeof setTimeout> | null = null
	let participantSearchRequestId = 0

	function clearParticipantSearchTimer() {
		if (participantSearchTimer !== null) {
			clearTimeout(participantSearchTimer)
			participantSearchTimer = null
		}
	}

	function getSemesterValueForStudent(): string | null {
		if (
			options.selectedSemester.value
			&& bookingSemesterOptions.value.includes(options.selectedSemester.value)
		) {
			return options.selectedSemester.value
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

	async function loadRoomBookings() {
		roomBookingsLoading.value = true
		roomBookingsError.value = null

		try {
			if (options.role.value === 'student') {
				if (!options.studentId.value || !options.selectedSemester.value) {
					roomBookings.value = []
					return
				}

				roomBookings.value = await roomBookingsService.getAll({
					semester: options.selectedSemester.value,
				})
				return
			}

			roomBookings.value = await roomBookingsService.getAll({
				semester: options.selectedSemester.value ?? undefined,
			})
		} catch (cause) {
			console.error('Failed to load room bookings', cause)
			roomBookingsError.value = 'Failed to load room bookings.'
			roomBookings.value = []
		} finally {
			roomBookingsLoading.value = false
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
					options.studentId.value as number,
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

	function setBookingModalOpen(isOpen: boolean) {
		bookingModalOpen.value = isOpen
		if (!isOpen) {
			resetBookingForm()
		}
	}

	function openBookingModal() {
		if (!options.studentId.value) {
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
		if (!canSubmitBooking.value || !options.studentId.value) {
			return
		}

		bookingSaving.value = true
		bookingError.value = null

		try {
			await roomBookingsService.create({
				studentId: options.studentId.value,
				semester: bookingForm.value.semester,
				timeSlotId: bookingForm.value.timeSlotId as number,
				roomId: bookingForm.value.roomId as number,
				participantEmails: selectedParticipants.value.map(participant => participant.email),
			})

			await loadRoomBookings()
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

	watch(availableBookingRooms, nextRooms => {
		if (bookingForm.value.roomId && !nextRooms.some(room => room.id === bookingForm.value.roomId)) {
			bookingForm.value.roomId = null
		}
	})

	watch(
		[
			participantSearchQuery,
			() => bookingForm.value.semester,
			() => bookingForm.value.timeSlotId,
			options.studentId,
			bookingModalOpen,
		],
		() => {
			void scheduleParticipantSearch()
		}
	)

	onBeforeUnmount(() => {
		clearParticipantSearchTimer()
	})

	return {
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
	}
}
