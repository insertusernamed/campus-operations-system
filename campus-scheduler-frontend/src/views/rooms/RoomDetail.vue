<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { roomsService, type Room } from '@/services/rooms'
import type { AxiosError } from 'axios'

const route = useRoute()
const room = ref<Room | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

async function fetchRoom(id: number) {
	loading.value = true
	error.value = null
	room.value = null

	if (isNaN(id)) {
		error.value = 'Invalid room ID'
		loading.value = false
		return
	}

	try {
		room.value = await roomsService.getById(id)
	} catch (e) {
		const axiosError = e as AxiosError
		if (axiosError.response?.status === 404) {
			error.value = 'Room not found'
		} else {
			error.value = 'Failed to load room'
		}
		console.error(e)
	} finally {
		loading.value = false
	}
}

watch(
	() => route.params.id,
	(newId) => fetchRoom(Number(newId)),
	{ immediate: true }
)
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink to="/rooms" class="text-blue-600 hover:underline text-sm">
				Back to Rooms
			</RouterLink>
		</div>

		<div v-if="loading" class="text-gray-500">Loading...</div>
		<div v-else-if="error" class="text-red-600">{{ error }}</div>

		<div v-else-if="room" class="bg-white border border-gray-200 p-6">
			<h1 class="text-2xl font-semibold text-gray-900 mb-4">
				Room {{ room.roomNumber }}
			</h1>

			<dl class="grid grid-cols-2 gap-4">
				<div>
					<dt class="text-sm font-medium text-gray-500">Building</dt>
					<dd class="text-gray-900">{{ room.buildingName || '-' }}</dd>
				</div>
				<div>
					<dt class="text-sm font-medium text-gray-500">Capacity</dt>
					<dd class="text-gray-900">{{ room.capacity }}</dd>
				</div>
				<div>
					<dt class="text-sm font-medium text-gray-500">Type</dt>
					<dd class="text-gray-900">{{ room.type }}</dd>
				</div>
				<div>
					<dt class="text-sm font-medium text-gray-500">Features</dt>
					<dd class="text-gray-900">{{ room.features || '-' }}</dd>
				</div>
			</dl>
		</div>
	</div>
</template>
