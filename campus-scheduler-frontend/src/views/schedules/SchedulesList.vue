<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { useCrud } from '@/composables/useCrud'
import { schedulesService, type Schedule } from '@/services/schedules'
import { timeslotsService } from '@/services/timeslots'

const { items, loading, error, fetchAll, handleDelete } = useCrud<Schedule, never>({
    getAll: schedulesService.getAll,
    deleteItem: schedulesService.delete,
    listRoute: '/schedules',
    deleteConfirm: 'Are you sure you want to delete this schedule?',
})

onMounted(fetchAll)


</script>

<template>
    <div>
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Schedules</h1>
            <RouterLink to="/schedules/new" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Add
                Schedule</RouterLink>
        </div>
        <div v-if="loading" class="text-gray-500">Loading...</div>
        <div v-else-if="error" class="text-red-600">{{ error }}</div>
        <div v-else-if="items.length === 0" class="text-gray-500">No schedules found.</div>
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
                <tr v-for="s in items" :key="s.id" class="border-b border-gray-100">
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
