<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { roomsService, type CreateRoomRequest } from '@/services/rooms'
import { buildingsService, type Building } from '@/services/buildings'
import FormSkeleton from '@/components/common/FormSkeleton.vue'
import type { AxiosError } from 'axios'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.params.id !== undefined)
const roomId = computed(() => Number(route.params.id))

const ROOM_TYPES = ['CLASSROOM', 'LAB', 'LECTURE_HALL', 'SEMINAR', 'STUDIO', 'OTHER']

const form = ref<CreateRoomRequest>({ roomNumber: '', capacity: 30, type: 'CLASSROOM', features: '' })
const selectedBuildingId = ref<number | null>(null)
const buildings = ref<Building[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

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
			form.value = { roomNumber: room.roomNumber, capacity: room.capacity, type: room.type, features: room.features || '' }
			selectedBuildingId.value = room.buildingId
		} catch {
			error.value = 'Failed to load room'
		} finally {
			loading.value = false
		}
	}
})

async function handleSubmit() {
	if (!selectedBuildingId.value) { error.value = 'Select a building'; return }
	saving.value = true
	error.value = null
	try {
		if (isEdit.value) await roomsService.update(roomId.value, form.value)
		else await roomsService.create(selectedBuildingId.value, form.value)
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
			<RouterLink to="/rooms" class="text-blue-600 hover:underline text-sm">Back to Rooms</RouterLink>
		</div>
		<div class="bg-white border border-gray-200 p-6 max-w-xl">
			<h1 class="text-2xl font-semibold text-gray-900 mb-6">{{ isEdit ? 'Edit Room' : 'New Room' }}</h1>
			<FormSkeleton v-if="loading" :fields="5" />
			<form v-else @submit.prevent="handleSubmit" class="space-y-4">
				<div v-if="error" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded">{{ error }}</div>
				<div class="grid grid-cols-2 gap-4">
					<div class="col-span-2">
						<label class="block text-sm font-medium text-gray-700 mb-1">Building <span
								class="text-red-500">*</span></label>
						<select v-model="selectedBuildingId" required :disabled="isEdit" aria-label="Building"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100">
							<option :value="null" disabled>-- Select --</option>
							<option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.code }} - {{ b.name }}
							</option>
						</select>
						<p v-if="isEdit" class="text-xs text-gray-500 mt-1">Building cannot be changed after creation
						</p>
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Room Number <span
								class="text-red-500">*</span></label>
						<input v-model="form.roomNumber" type="text" required maxlength="20"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Capacity <span
								class="text-red-500">*</span></label>
						<input v-model.number="form.capacity" type="number" required min="1"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">
							Type <span class="text-red-500">*</span>
							<span
								v-tooltip="'Room type determines which courses can be scheduled here. The solver will only assign courses to rooms matching their required type.'"
								class="ml-1 cursor-help text-gray-400 hover:text-gray-600">ⓘ</span>
						</label>
						<select v-model="form.type" required aria-label="Room Type"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
							<option v-for="t in ROOM_TYPES" :key="t" :value="t">{{ t.replace('_', ' ') }}</option>
						</select>
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Features</label>
						<input v-model="form.features" type="text" maxlength="255"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500"
							placeholder="Projector, Whiteboard..." />
					</div>
				</div>
				<div class="flex gap-4 pt-4">
					<button type="submit" :disabled="saving"
						class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">{{ saving
							? 'Saving...' : (isEdit ? 'Update' : 'Create') }}</button>
					<RouterLink to="/rooms" class="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">Cancel
					</RouterLink>
				</div>
			</form>
		</div>
	</div>
</template>
