<script setup lang="ts">
import { computed } from 'vue'
import type { Role } from '@/composables/useRole'
import BaseModal from '@/components/common/BaseModal.vue'
import { changeRequestIssueOptions, type ChangeRequestIssue } from '@/constants/changeRequestIssues'
import type { EnrollmentStatus } from '@/services/enrollments'
import type { RoomBooking } from '@/services/roomBookings'
import type { Schedule } from '@/services/schedules'
import { timeslotsService } from '@/services/timeslots'
import {
	formatRoom,
	getBookingPrivacyMessage,
	getInstructorName,
	getParticipantTotalLabel,
	getSeatPressure,
	getSeatUtilization,
	getStudentStatusLabel,
} from '@/views/schedules/helpers'

const props = defineProps<{
	modelValue: boolean
	title: string
	role: Role
	selectedSchedule: Schedule | null
	selectedRoomBooking: RoomBooking | null
	selectedStudentStatus: EnrollmentStatus | null
	selectedIssue: ChangeRequestIssue | ''
}>()

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void
	(e: 'update:selectedIssue', value: ChangeRequestIssue | ''): void
	(e: 'openCourse'): void
	(e: 'openRoom'): void
	(e: 'deleteSchedule'): void
	(e: 'startRequest'): void
	(e: 'exportSingleClass'): void
	(e: 'exportClassForSemester'): void
}>()

const selectedIssueModel = computed({
	get: () => props.selectedIssue,
	set: value => emit('update:selectedIssue', value),
})

const roomBookingViewerCanSeeStudentDetails = computed(() =>
	!!props.selectedRoomBooking
	&& (props.selectedRoomBooking.viewerCanSeeStudentDetails || props.selectedRoomBooking.viewerIsOwner)
)

function closeModal() {
	emit('update:modelValue', false)
}
</script>

<template>
	<BaseModal
		:model-value="modelValue"
		:title="title"
		size="lg"
		@update:model-value="emit('update:modelValue', $event)"
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
							@click="emit('exportSingleClass')"
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
							@click="emit('exportClassForSemester')"
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
							v-model="selectedIssueModel"
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
			<div v-if="roomBookingViewerCanSeeStudentDetails && selectedRoomBooking.bookedBy">
				<span class="font-medium text-gray-900">Booked by:</span>
				{{ selectedRoomBooking.bookedBy.fullName }} ({{ selectedRoomBooking.bookedBy.email }})
			</div>
			<div v-if="roomBookingViewerCanSeeStudentDetails">
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
				<button class="rounded border border-gray-300 px-4 py-2" @click="closeModal">
					Close
				</button>
				<button
					class="rounded border border-gray-300 px-4 py-2 disabled:opacity-50"
					:disabled="!selectedSchedule"
					@click="emit('openCourse')"
				>
					View Course
				</button>
				<button
					class="rounded border border-gray-300 px-4 py-2 disabled:opacity-50"
					@click="emit('openRoom')"
				>
					View Room
				</button>
				<button
					class="rounded bg-red-600 px-4 py-2 text-white disabled:opacity-50"
					:disabled="!selectedSchedule"
					@click="emit('deleteSchedule')"
				>
					Delete
				</button>
			</div>
			<div v-else-if="role === 'admin' && selectedRoomBooking" class="flex justify-end gap-2">
				<button class="rounded border border-gray-300 px-4 py-2" @click="closeModal">
					Close
				</button>
				<button class="rounded border border-gray-300 px-4 py-2" @click="emit('openRoom')">
					View Room
				</button>
			</div>
			<div v-else-if="role === 'instructor' && selectedSchedule" class="flex justify-end gap-2">
				<button class="rounded border border-gray-300 px-4 py-2" @click="closeModal">
					Cancel
				</button>
				<button
					class="rounded bg-blue-600 px-4 py-2 text-white disabled:opacity-50"
					:disabled="!selectedIssue"
					@click="emit('startRequest')"
				>
					Request Change
				</button>
			</div>
			<div v-else class="flex justify-end gap-2">
				<button class="rounded border border-gray-300 px-4 py-2" @click="closeModal">
					Close
				</button>
			</div>
		</template>
	</BaseModal>
</template>
