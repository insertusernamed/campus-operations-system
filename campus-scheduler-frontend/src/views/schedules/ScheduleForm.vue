<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { schedulesService, type ScheduleCreateRequest } from '@/services/schedules'
import { coursesService, type Course } from '@/services/courses'
import { roomsService, type Room } from '@/services/rooms'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import type { AxiosError } from 'axios'

const router = useRouter()

const form = ref<ScheduleCreateRequest>({ courseId: 0, roomId: 0, timeSlotId: 0, semester: '' })
const courses = ref<Course[]>([])
const rooms = ref<Room[]>([])
const timeslots = ref<TimeSlot[]>([])

const saving = ref(false)
const error = ref<string | null>(null)
const conflictWarning = ref<string | null>(null)

onMounted(async () => {
    try {
        [courses.value, rooms.value, timeslots.value] = await Promise.all([
            coursesService.getAll(),
            roomsService.getAll(),
            timeslotsService.getAll(),
        ])
    } catch {
        error.value = 'Failed to load form data'
    }
})

// Check conflicts when room/timeslot/semester change
watch([() => form.value.roomId, () => form.value.timeSlotId, () => form.value.semester], async () => {
    conflictWarning.value = null
    if (form.value.roomId && form.value.timeSlotId) {
        try {
            const result = await schedulesService.checkConflicts(form.value.roomId, form.value.timeSlotId, form.value.semester || undefined)
            if (result.hasConflict) conflictWarning.value = 'This room is already booked at this time!'
        } catch {
            // Silently ignore conflict check failures - user can still try to submit
        }
    }
}, { immediate: false })

async function handleSubmit() {
    if (!form.value.courseId || !form.value.roomId || !form.value.timeSlotId || !form.value.semester) {
        error.value = 'All fields are required'
        return
    }
    saving.value = true
    error.value = null
    try {
        await schedulesService.create(form.value)
        router.push('/schedules')
    } catch (e) {
        const axiosError = e as AxiosError<{ error?: string; message?: string }>
        error.value = axiosError.response?.data?.error || axiosError.response?.data?.message || 'Failed to save'
    } finally {
        saving.value = false
    }
}

function formatTimeSlot(ts: TimeSlot) {
    return timeslotsService.formatTimeSlot(ts)
}
</script>

<template>
    <div>
        <div class="mb-6">
            <RouterLink to="/schedules" class="text-blue-600 hover:underline text-sm">Back to Schedules</RouterLink>
        </div>
        <div class="bg-white border border-gray-200 p-6 max-w-xl">
            <h1 class="text-2xl font-semibold text-gray-900 mb-6">New Schedule</h1>
            <form @submit.prevent="handleSubmit" class="space-y-4">
                <div v-if="error" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded">{{ error }}</div>
                <div v-if="conflictWarning" class="p-3 bg-yellow-50 border border-yellow-200 text-yellow-700 rounded">{{
                    conflictWarning }}</div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Course <span
                            class="text-red-500">*</span></label>
                    <select v-model.number="form.courseId" required
                        class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
                        <option :value="0" disabled>-- Select course --</option>
                        <option v-for="c in courses" :key="c.id" :value="c.id">{{ c.code }} - {{ c.name }}</option>
                    </select>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Room <span
                            class="text-red-500">*</span></label>
                    <select v-model.number="form.roomId" required
                        class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
                        <option :value="0" disabled>-- Select room --</option>
                        <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.buildingCode }} {{ r.roomNumber }} ({{
                            r.capacity }} seats)</option>
                    </select>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Time Slot <span
                            class="text-red-500">*</span></label>
                    <select v-model.number="form.timeSlotId" required
                        class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
                        <option :value="0" disabled>-- Select time slot --</option>
                        <option v-for="ts in timeslots" :key="ts.id" :value="ts.id">{{ formatTimeSlot(ts) }}</option>
                    </select>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">Semester <span
                            class="text-red-500">*</span></label>
                    <input v-model="form.semester" type="text" required maxlength="20"
                        class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500"
                        placeholder="e.g., Fall 2026" />
                </div>

                <div class="flex gap-4 pt-4">
                    <button type="submit" :disabled="saving || !!conflictWarning"
                        class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">{{ saving
                            ? 'Saving...' : 'Create' }}</button>
                    <RouterLink to="/schedules" class="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">Cancel
                    </RouterLink>
                </div>
            </form>
        </div>
    </div>
</template>
