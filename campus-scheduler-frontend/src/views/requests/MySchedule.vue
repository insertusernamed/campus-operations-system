<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRole } from '@/composables/useRole'
import { schedulesService, type Schedule } from '@/services/schedules'
import { timeslotsService } from '@/services/timeslots'
import EmptyState from '@/components/common/EmptyState.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'

const { role, instructorId } = useRole()

const loading = ref(false)
const error = ref<string | null>(null)
const items = ref<Schedule[]>([])

const filteredItems = computed(() => items.value)

async function loadData() {
    loading.value = true
    error.value = null
    try {
        const data = await schedulesService.getAll({
            instructorId: role.value === 'admin' ? undefined : (instructorId.value ?? undefined),
        })
        items.value = data
    } catch (e) {
        console.error(e)
        error.value = 'Failed to load schedules'
    } finally {
        loading.value = false
    }
}

watch([role, instructorId], loadData)

onMounted(loadData)
</script>

<template>
    <div>
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">My Schedule</h1>
        </div>

        <TableSkeleton v-if="loading" :columns="5" :rows="6" />
        <div v-else-if="error" class="text-red-600">{{ error }}</div>
        <EmptyState v-else-if="filteredItems.length === 0" title="No schedules"
            description="No schedule entries available." />

        <table v-else class="w-full bg-white border border-gray-200">
            <thead>
                <tr class="bg-gray-50 border-b border-gray-200">
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Course</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Room</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Time</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Semester</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="schedule in filteredItems" :key="schedule.id" class="border-b border-gray-100">
                    <td class="px-4 py-3">
                        <div class="text-sm text-gray-900">{{ schedule.course.code }}</div>
                        <div class="text-xs text-gray-500">{{ schedule.course.name }}</div>
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600">
                        {{ schedule.room.buildingCode }} {{ schedule.room.roomNumber }}
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600">
                        {{ timeslotsService.formatTimeSlot(schedule.timeSlot) }}
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600">{{ schedule.semester }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</template>
