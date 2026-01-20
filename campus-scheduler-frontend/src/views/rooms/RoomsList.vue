<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { roomsService, type Room } from '@/services/rooms'

const rooms = ref<Room[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

onMounted(async () => {
    try {
        rooms.value = await roomsService.getAll()
    } catch (e) {
        error.value = 'Failed to load rooms'
        console.error(e)
    } finally {
        loading.value = false
    }
})
</script>

<template>
    <div>
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Rooms</h1>
        </div>

        <div v-if="loading" class="text-gray-500">Loading...</div>
        <div v-else-if="error" class="text-red-600">{{ error }}</div>
        <div v-else-if="rooms.length === 0" class="text-gray-500">No rooms found.</div>

        <table v-else class="w-full bg-white border border-gray-200">
            <thead>
                <tr class="bg-gray-50 border-b border-gray-200">
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Building</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Room</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Capacity</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Type</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Features</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="room in rooms" :key="room.id" class="border-b border-gray-100">
                    <td class="px-4 py-3 text-gray-600">{{ room.buildingName || '-' }}</td>
                    <td class="px-4 py-3">
                        <RouterLink :to="`/rooms/${room.id}`" class="text-blue-600 hover:underline">
                            {{ room.roomNumber }}
                        </RouterLink>
                    </td>
                    <td class="px-4 py-3 text-gray-600">{{ room.capacity }}</td>
                    <td class="px-4 py-3 text-gray-600">{{ room.type }}</td>
                    <td class="px-4 py-3 text-gray-600">{{ room.features || '-' }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>
