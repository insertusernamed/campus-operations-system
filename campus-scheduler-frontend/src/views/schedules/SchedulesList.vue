<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { schedulesService, type Schedule } from '@/services/schedules'
import { roomsService, type Room } from '@/services/rooms'
import { buildingsService, type Building } from '@/services/buildings'
import { timeslotsService } from '@/services/timeslots'
import ScheduleCalendar from '@/components/calendar/ScheduleCalendar.vue'

type ViewMode = 'table' | 'calendar'
const viewMode = ref<ViewMode>('calendar')
const selectedBuildingId = ref<number | null>(null)
const selectedRoomId = ref<number | null>(null)
const rooms = ref<Room[]>([])
const buildings = ref<Building[]>([])

const { items, loading, error, fetchAll, handleDelete } = useCrud<Schedule, never>({
	getAll: schedulesService.getAll,
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

// Filter schedules by selected building and room
const filteredItems = computed(() => {
	let result = hydratedItems.value
	if (selectedBuildingId.value) {
		result = result.filter(s => s.room.buildingId === selectedBuildingId.value)
	}
	if (selectedRoomId.value) {
		result = result.filter(s => s.room.id === selectedRoomId.value)
	}
	return result
})

// Reset room selection when building changes
watch(selectedBuildingId, () => {
	selectedRoomId.value = null
})

onMounted(async () => {
	await fetchAll()
	try {
		const [roomsData, buildingsData] = await Promise.all([
			roomsService.getAll(),
			buildingsService.getAll()
		])
		rooms.value = roomsData
		buildings.value = buildingsData
	} catch (e) {
		console.error('Failed to load filter data:', e)
	}
})
</script>

<template>
	<div>
		<div class="flex justify-between items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">Schedules</h1>
			<div class="flex items-center gap-3">
				<!-- Building Filter -->
				<select v-model="selectedBuildingId"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Buildings</option>
					<option v-for="building in buildings" :key="building.id" :value="building.id">
						{{ building.code }} - {{ building.name }}
					</option>
				</select>
				<!-- Room Filter -->
				<select v-model="selectedRoomId"
					class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
					<option :value="null">All Rooms</option>
					<option v-for="room in filteredRooms" :key="room.id" :value="room.id">
						{{ room.roomNumber }} ({{ room.capacity }} seats)
						{{ scheduleCountByRoom.get(room.id) ? `- ${scheduleCountByRoom.get(room.id)} classes` : '' }}
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
				<RouterLink to="/schedules/new" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Add
					Schedule</RouterLink>
			</div>
		</div>

		<div v-if="loading" class="text-gray-500">Loading...</div>
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<div v-else-if="filteredItems.length === 0" class="text-gray-500">No schedules found.</div>

		<!-- Calendar View -->
		<ScheduleCalendar v-else-if="viewMode === 'calendar'" :schedules="filteredItems" />

		<!-- Table View -->
		<table v-else class="w-full bg-white border border-gray-200">
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
						<button @click="handleDelete(s.id)" class="text-red-600 hover:underline">Delete</button>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
