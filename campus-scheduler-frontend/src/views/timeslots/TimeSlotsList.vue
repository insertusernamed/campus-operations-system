<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { timeslotsService, type TimeSlot, DAY_OF_WEEK_OPTIONS } from '@/services/timeslots'

const timeslots = ref<TimeSlot[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const groupedTimeslots = computed(() => {
    const groups: Record<string, TimeSlot[]> = {}
    for (const day of DAY_OF_WEEK_OPTIONS) groups[day.value] = []
    for (const slot of timeslots.value) {
        const arr = groups[slot.dayOfWeek]
        if (arr) arr.push(slot)
    }
    for (const day in groups) {
        const arr = groups[day]
        if (arr) arr.sort((a, b) => a.startTime.localeCompare(b.startTime))
    }
    return groups
})

onMounted(async () => {
    try { timeslots.value = await timeslotsService.getAll() }
    catch { error.value = 'Failed to load' }
    finally { loading.value = false }
})

async function handleDelete(id: number) {
    if (!confirm('Delete this time slot?')) return
    await timeslotsService.delete(id)
    timeslots.value = timeslots.value.filter(t => t.id !== id)
}
</script>

<template>
    <div>
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Time Slots</h1>
            <RouterLink to="/timeslots/new" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Add Time
                Slot</RouterLink>
        </div>
        <div v-if="loading" class="text-gray-500">Loading...</div>
        <div v-else-if="error" class="text-red-600">{{ error }}</div>
        <div v-else-if="timeslots.length === 0" class="text-gray-500">No time slots found.</div>
        <div v-else class="space-y-4">
            <div v-for="day in DAY_OF_WEEK_OPTIONS" :key="day.value">
                <template v-if="groupedTimeslots[day.value]?.length">
                    <h2 class="text-lg font-medium text-gray-800 mb-2">{{ day.label }}</h2>
                    <table class="w-full bg-white border border-gray-200 mb-4">
                        <thead>
                            <tr class="bg-gray-50 border-b">
                                <th class="text-left px-4 py-2 text-sm font-medium text-gray-700">Time</th>
                                <th class="text-left px-4 py-2 text-sm font-medium text-gray-700">Label</th>
                                <th class="text-left px-4 py-2 text-sm font-medium text-gray-700">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr v-for="s in groupedTimeslots[day.value]" :key="s.id" class="border-b border-gray-100">
                                <td class="px-4 py-2">{{ s.startTime }} - {{ s.endTime }}</td>
                                <td class="px-4 py-2 text-gray-600">{{ s.label || '-' }}</td>
                                <td class="px-4 py-2">
                                    <RouterLink :to="`/timeslots/${s.id}/edit`"
                                        class="text-blue-600 hover:underline mr-4">Edit</RouterLink>
                                    <button @click="handleDelete(s.id)"
                                        class="text-red-600 hover:underline">Delete</button>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </template>
            </div>
        </div>
    </div>
</template>
