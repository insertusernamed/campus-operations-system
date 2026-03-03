<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { AxiosError } from 'axios'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import FormSkeleton from '@/components/common/FormSkeleton.vue'
import { buildingsService, type Building } from '@/services/buildings'
import {
	roomsService,
	type CreateRoomRequest,
	type RoomAvailabilityStatus,
} from '@/services/rooms'

interface RoomFormState {
	roomNumber: string
	capacity: number
	type: string
	availabilityStatus: RoomAvailabilityStatus
	featureSetInput: string
	accessibilityFlagsInput: string
	operationalNotes: string
	lastInspectionDate: string
}

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.params.id !== undefined)
const roomId = computed(() => Number(route.params.id))

const ROOM_TYPES = ['CLASSROOM', 'LAB', 'LECTURE_HALL', 'SEMINAR', 'CONFERENCE']
const availabilityOptions: Array<{ value: RoomAvailabilityStatus; label: string }> = [
	{ value: 'AVAILABLE', label: 'Available' },
	{ value: 'MAINTENANCE', label: 'Maintenance' },
	{ value: 'OUT_OF_SERVICE', label: 'Out of Service' },
]

const form = ref<RoomFormState>({
	roomNumber: '',
	capacity: 30,
	type: 'CLASSROOM',
	availabilityStatus: 'AVAILABLE',
	featureSetInput: '',
	accessibilityFlagsInput: '',
	operationalNotes: '',
	lastInspectionDate: '',
})
const selectedBuildingId = ref<number | null>(null)
const buildings = ref<Building[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

function parseTagInput(value: string): string[] {
	return [...new Set(value
		.split(',')
		.map((token) => token.trim().toLowerCase())
		.filter(Boolean))]
}

function formatTagInput(values: string[], fallback: string | null): string {
	if (values.length > 0) {
		return values.join(', ')
	}
	return fallback ?? ''
}

function buildRequestPayload(): CreateRoomRequest {
	const featureSet = parseTagInput(form.value.featureSetInput)
	const accessibilityFlags = parseTagInput(form.value.accessibilityFlagsInput)

	return {
		roomNumber: form.value.roomNumber.trim(),
		capacity: form.value.capacity,
		type: form.value.type,
		availabilityStatus: form.value.availabilityStatus,
		featureSet,
		features: featureSet.join(', '),
		accessibilityFlags,
		operationalNotes: form.value.operationalNotes.trim(),
		lastInspectionDate: form.value.lastInspectionDate || null,
	}
}

onMounted(async () => {
	try {
		buildings.value = await buildingsService.getAll()
	} catch {
		error.value = 'Failed to load buildings'
	}
	if (isEdit.value) {
		loading.value = true
		try {
			const room = await roomsService.getById(roomId.value)
			form.value = {
				roomNumber: room.roomNumber,
				capacity: room.capacity,
				type: room.type,
				availabilityStatus: room.availabilityStatus,
				featureSetInput: formatTagInput(room.featureSet, room.features),
				accessibilityFlagsInput: room.accessibilityFlags.join(', '),
				operationalNotes: room.operationalNotes ?? '',
				lastInspectionDate: room.lastInspectionDate ?? '',
			}
			selectedBuildingId.value = room.buildingId
		} catch {
			error.value = 'Failed to load room'
		} finally {
			loading.value = false
		}
	}
})

async function handleSubmit() {
	if (!selectedBuildingId.value) {
		error.value = 'Select a building'
		return
	}
	saving.value = true
	error.value = null
	try {
		const payload = buildRequestPayload()
		if (isEdit.value) {
			await roomsService.update(roomId.value, payload)
		} else {
			await roomsService.create(selectedBuildingId.value, payload)
		}
		router.push('/rooms')
	} catch (e) {
		error.value = (e as AxiosError<{ message?: string }>).response?.data?.message || 'Failed to save'
	} finally {
		saving.value = false
	}
}
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink to="/rooms" class="text-sm text-blue-600 hover:underline">Back to Rooms</RouterLink>
		</div>
		<div class="max-w-3xl border border-gray-200 bg-white p-6">
			<h1 class="mb-6 text-2xl font-semibold text-gray-900">{{ isEdit ? 'Edit Room' : 'New Room' }}</h1>
			<FormSkeleton v-if="loading" :fields="8" />
			<form v-else class="space-y-4" @submit.prevent="handleSubmit">
				<div v-if="error" class="rounded border border-red-200 bg-red-50 p-3 text-red-600">{{ error }}</div>
				<div class="grid grid-cols-2 gap-4">
					<div class="col-span-2">
						<label for="room-building" class="mb-1 block text-sm font-medium text-gray-700">Building <span
								class="text-red-500">*</span></label>
						<select id="room-building" v-model="selectedBuildingId" required :disabled="isEdit"
							aria-label="Building"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100">
							<option :value="null" disabled>-- Select --</option>
							<option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.code }} - {{ b.name }}
							</option>
						</select>
						<p v-if="isEdit" class="mt-1 text-xs text-gray-500">Building cannot be changed after creation
						</p>
					</div>
					<div>
						<label for="room-number" class="mb-1 block text-sm font-medium text-gray-700">Room Number <span
								class="text-red-500">*</span></label>
						<input id="room-number" v-model="form.roomNumber" type="text" required maxlength="20"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label for="room-capacity" class="mb-1 block text-sm font-medium text-gray-700">Capacity <span
								class="text-red-500">*</span></label>
						<input id="room-capacity" v-model.number="form.capacity" type="number" required min="1"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label for="room-type" class="mb-1 block text-sm font-medium text-gray-700">
							Type <span class="text-red-500">*</span>
							<span
								v-tooltip="'Room type determines which courses can be scheduled here. The solver uses this with capacity and feature constraints when assigning courses.'"
								class="ml-1 cursor-help text-gray-400 hover:text-gray-600">ⓘ</span>
						</label>
						<select id="room-type" v-model="form.type" required aria-label="Room Type"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500">
							<option v-for="t in ROOM_TYPES" :key="t" :value="t">{{ t.replace('_', ' ') }}</option>
						</select>
					</div>
					<div>
						<label for="room-availability" class="mb-1 block text-sm font-medium text-gray-700">
							Availability Status <span class="text-red-500">*</span>
						</label>
						<select id="room-availability" v-model="form.availabilityStatus" required
							aria-label="Availability Status"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500">
							<option v-for="status in availabilityOptions" :key="status.value" :value="status.value">
								{{ status.label }}
							</option>
						</select>
					</div>
					<div class="col-span-2">
						<label for="room-feature-set" class="mb-1 block text-sm font-medium text-gray-700">Feature
							Tags</label>
						<input id="room-feature-set" v-model="form.featureSetInput" type="text" maxlength="600"
							placeholder="projector, lab-ready, smart-board"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500" />
						<p class="mt-1 text-xs text-gray-500">Use comma-separated tags for matching and readiness
							analysis.</p>
					</div>
					<div class="col-span-2">
						<label for="room-accessibility"
							class="mb-1 block text-sm font-medium text-gray-700">Accessibility Flags</label>
						<input id="room-accessibility" v-model="form.accessibilityFlagsInput" type="text"
							maxlength="600" placeholder="wheelchair, assistive-listening, adjustable-desks"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label for="room-inspection-date" class="mb-1 block text-sm font-medium text-gray-700">
							Last Inspection Date
						</label>
						<input id="room-inspection-date" v-model="form.lastInspectionDate" type="date"
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500" />
					</div>
					<div class="col-span-2">
						<label for="room-operational-notes"
							class="mb-1 block text-sm font-medium text-gray-700">Operational Notes</label>
						<textarea id="room-operational-notes" v-model="form.operationalNotes" rows="3" maxlength="1000"
							placeholder="Temporary constraints, equipment updates, or maintenance notes."
							class="w-full rounded border border-gray-300 px-3 py-2 focus:ring-2 focus:ring-blue-500"></textarea>
					</div>
				</div>
				<div class="flex gap-4 pt-4">
					<button type="submit" :disabled="saving"
						class="rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50">{{ saving
							? 'Saving...'
							: (isEdit ? 'Update' : 'Create') }}</button>
					<RouterLink to="/rooms" class="rounded border border-gray-300 px-4 py-2 hover:bg-gray-50">Cancel
					</RouterLink>
				</div>
			</form>
		</div>
	</div>
</template>
