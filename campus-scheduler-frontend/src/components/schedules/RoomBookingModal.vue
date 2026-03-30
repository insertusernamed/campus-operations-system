<script setup lang="ts">
import { computed } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'
import type { RoomBookingStudentLookupResponse } from '@/services/roomBookings'
import type { Room } from '@/services/rooms'
import { DAY_OF_WEEK_OPTIONS, timeslotsService, type TimeSlot } from '@/services/timeslots'
import { formatRoom } from '@/views/schedules/helpers'

const props = defineProps<{
	modelValue: boolean
	error: string | null
	saving: boolean
	semesterOptions: string[]
	semester: string
	timeSlots: TimeSlot[]
	timeSlotId: number | null
	availableRooms: Room[]
	roomId: number | null
	participantSearchQuery: string
	participantSearchLoading: boolean
	participantSearchError: string | null
	participantSuggestions: RoomBookingStudentLookupResponse[]
	selectedParticipants: RoomBookingStudentLookupResponse[]
	selectedBookingTimeSlot: TimeSlot | null
	canSubmit: boolean
}>()

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void
	(e: 'update:semester', value: string): void
	(e: 'update:timeSlotId', value: number | null): void
	(e: 'update:roomId', value: number | null): void
	(e: 'update:participantSearchQuery', value: string): void
	(e: 'addParticipant', participant: RoomBookingStudentLookupResponse): void
	(e: 'removeParticipant', participantId: number): void
	(e: 'submit'): void
}>()

const semesterModel = computed({
	get: () => props.semester,
	set: (value: string) => emit('update:semester', value),
})

const timeSlotIdModel = computed({
	get: () => props.timeSlotId,
	set: (value: number | null) => emit('update:timeSlotId', value),
})

const roomIdModel = computed({
	get: () => props.roomId,
	set: (value: number | null) => emit('update:roomId', value),
})

const participantSearchQueryModel = computed({
	get: () => props.participantSearchQuery,
	set: (value: string) => emit('update:participantSearchQuery', value),
})

const selectedBookingDayLabel = computed(() =>
	props.selectedBookingTimeSlot
		? DAY_OF_WEEK_OPTIONS.find(option => option.value === props.selectedBookingTimeSlot?.dayOfWeek)?.label
		: null
)

function closeModal() {
	emit('update:modelValue', false)
}
</script>

<template>
	<BaseModal
		:model-value="modelValue"
		title="Book Room"
		size="lg"
		@update:model-value="emit('update:modelValue', $event)"
	>
		<div class="space-y-4">
			<div v-if="error" class="rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
				{{ error }}
			</div>

			<div class="grid gap-4 md:grid-cols-2">
				<div>
					<label for="booking-semester" class="mb-1 block text-sm font-medium text-gray-700">
						Semester
					</label>
					<select
						id="booking-semester"
						v-model="semesterModel"
						class="w-full rounded border border-gray-300 px-3 py-2"
					>
						<option value="" disabled>Select semester</option>
						<option v-for="option in semesterOptions" :key="option" :value="option">
							{{ option }}
						</option>
					</select>
				</div>
				<div>
					<label for="booking-timeslot" class="mb-1 block text-sm font-medium text-gray-700">
						Time Slot
					</label>
					<select
						id="booking-timeslot"
						v-model="timeSlotIdModel"
						class="w-full rounded border border-gray-300 px-3 py-2"
					>
						<option :value="null" disabled>Select time slot</option>
						<option v-for="timeSlot in timeSlots" :key="timeSlot.id" :value="timeSlot.id">
							{{ timeslotsService.formatTimeSlot(timeSlot) }}
						</option>
					</select>
				</div>
			</div>

			<div>
				<label for="booking-room" class="mb-1 block text-sm font-medium text-gray-700">Room</label>
				<select
					id="booking-room"
					v-model="roomIdModel"
					class="w-full rounded border border-gray-300 px-3 py-2"
					:disabled="!semester || !timeSlotId"
				>
					<option :value="null" disabled>Select room</option>
					<option v-for="room in availableRooms" :key="room.id" :value="room.id">
						{{ formatRoom(room) }} ({{ room.capacity }} seats)
					</option>
				</select>
				<p
					v-if="semester && timeSlotId && availableRooms.length === 0"
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
					v-model="participantSearchQueryModel"
					type="text"
					placeholder="Start typing a student email"
					class="w-full rounded border border-gray-300 px-3 py-2"
					:disabled="!semester || !timeSlotId"
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
							@click="emit('addParticipant', candidate)"
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
								@click="emit('removeParticipant', participant.id)"
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
				Booking for {{ semester }} on
				{{ selectedBookingDayLabel }}
				{{ timeslotsService.formatTime(selectedBookingTimeSlot.startTime) }} -
				{{ timeslotsService.formatTime(selectedBookingTimeSlot.endTime) }}.
			</div>
		</div>

		<template #footer>
			<div class="flex justify-end gap-2">
				<button class="rounded border border-gray-300 px-4 py-2" @click="closeModal">
					Cancel
				</button>
				<button
					class="rounded bg-blue-600 px-4 py-2 text-white disabled:cursor-not-allowed disabled:opacity-50"
					:disabled="!canSubmit || saving"
					@click="emit('submit')"
				>
					{{ saving ? 'Creating...' : 'Create Booking' }}
				</button>
			</div>
		</template>
	</BaseModal>
</template>
