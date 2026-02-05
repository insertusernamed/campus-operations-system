<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { toast } from 'vue3-toastify'
import { useRole } from '@/composables/useRole'
import { changeRequestsService, type ChangeRequestReason } from '@/services/changeRequests'
import { schedulesService, type Schedule } from '@/services/schedules'
import { roomsService, type Room } from '@/services/rooms'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'

const router = useRouter()
const { role, instructorId } = useRole()

const schedules = ref<Schedule[]>([])
const rooms = ref<Room[]>([])
const timeSlots = ref<TimeSlot[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

const form = ref({
    scheduleId: 0,
    proposedRoomId: null as number | null,
    proposedTimeSlotId: null as number | null,
    reasonCategory: 'MEDICAL' as ChangeRequestReason,
    reasonDetails: '',
})

const validation = ref<{ hardConflicts: string[]; softWarnings: string[] }>({
    hardConflicts: [],
    softWarnings: [],
})

const canSubmit = computed(() => {
    return !!form.value.scheduleId && (form.value.proposedRoomId || form.value.proposedTimeSlotId)
})

const availableSchedules = computed(() => schedules.value)

async function loadData() {
    loading.value = true
    error.value = null
    try {
        const [scheduleData, roomData, timeSlotData] = await Promise.all([
            schedulesService.getAll({ instructorId: instructorId.value ?? undefined }),
            roomsService.getAll(),
            timeslotsService.getAll(),
        ])
        schedules.value = scheduleData
        rooms.value = roomData
        timeSlots.value = timeSlotData
    } catch (e) {
        console.error(e)
        error.value = 'Failed to load data'
    } finally {
        loading.value = false
    }
}

async function runValidation() {
    validation.value = { hardConflicts: [], softWarnings: [] }
    if (!form.value.scheduleId || (!form.value.proposedRoomId && !form.value.proposedTimeSlotId)) {
        return
    }

    try {
        const result = await changeRequestsService.validate({
            scheduleId: form.value.scheduleId,
            proposedRoomId: form.value.proposedRoomId,
            proposedTimeSlotId: form.value.proposedTimeSlotId,
        })
        validation.value = {
            hardConflicts: result.hardConflicts,
            softWarnings: result.softWarnings,
        }
    } catch (e) {
        console.error(e)
    }
}

async function handleSubmit() {
    if (!canSubmit.value) {
        error.value = 'Select a schedule and proposed change'
        return
    }
    if (!instructorId.value) {
        error.value = 'Select an instructor to continue'
        return
    }

    saving.value = true
    error.value = null
    try {
        await changeRequestsService.create({
            scheduleId: form.value.scheduleId,
            requestedByInstructorId: instructorId.value,
            requestedByRole: role.value === 'admin' ? 'ADMIN' : 'INSTRUCTOR',
            reasonCategory: form.value.reasonCategory,
            reasonDetails: form.value.reasonDetails || undefined,
            proposedRoomId: form.value.proposedRoomId,
            proposedTimeSlotId: form.value.proposedTimeSlotId,
        })
        toast.success('Change request submitted')
        router.push('/requests')
    } catch (e: any) {
        error.value = e?.response?.data?.error || 'Failed to submit request'
        toast.error(error.value)
    } finally {
        saving.value = false
    }
}

watch(() => [form.value.scheduleId, form.value.proposedRoomId, form.value.proposedTimeSlotId], () => {
    runValidation()
})

onMounted(loadData)
</script>

<template>
    <div>
        <div class="mb-6">
            <RouterLink to="/requests" class="text-blue-600 hover:underline text-sm">Back to Requests</RouterLink>
        </div>
        <div class="bg-white border border-gray-200 p-6 max-w-2xl">
            <h1 class="text-2xl font-semibold text-gray-900 mb-6">Request Schedule Change</h1>

            <div v-if="loading" class="text-sm text-gray-500">Loading...</div>
            <div v-else>
                <div v-if="error" class="mb-4 text-red-600">{{ error }}</div>

                <div class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Schedule</label>
                        <select v-model.number="form.scheduleId" class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option :value="0" disabled>Select schedule</option>
                            <option v-for="schedule in availableSchedules" :key="schedule.id" :value="schedule.id">
                                {{ schedule.course.code }} - {{ schedule.course.name }} ({{ schedule.semester }})
                            </option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Proposed Room</label>
                        <select v-model="form.proposedRoomId" class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option :value="null">Keep current</option>
                            <option v-for="room in rooms" :key="room.id" :value="room.id">
                                {{ room.buildingCode }} {{ room.roomNumber }} ({{ room.capacity }} seats)
                            </option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Proposed Time Slot</label>
                        <select v-model="form.proposedTimeSlotId" class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option :value="null">Keep current</option>
                            <option v-for="slot in timeSlots" :key="slot.id" :value="slot.id">
                                {{ timeslotsService.formatTimeSlot(slot) }}
                            </option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Reason</label>
                        <select v-model="form.reasonCategory" class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option value="MEDICAL">Medical</option>
                            <option value="EQUIPMENT_FAILURE">Equipment Failure</option>
                            <option value="PEDAGOGICAL_CONFLICT">Pedagogical Conflict</option>
                            <option value="OTHER">Other</option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Details</label>
                        <textarea v-model="form.reasonDetails" rows="3"
                            class="w-full px-3 py-2 border border-gray-300 rounded"></textarea>
                    </div>

                    <div v-if="validation.hardConflicts.length" class="bg-red-50 border border-red-200 text-red-700 p-3 rounded">
                        <p class="font-medium mb-1">Conflicts</p>
                        <ul class="list-disc list-inside text-sm">
                            <li v-for="conflict in validation.hardConflicts" :key="conflict">{{ conflict }}</li>
                        </ul>
                    </div>

                    <div v-if="validation.softWarnings.length" class="bg-yellow-50 border border-yellow-200 text-yellow-700 p-3 rounded">
                        <p class="font-medium mb-1">Warnings</p>
                        <ul class="list-disc list-inside text-sm">
                            <li v-for="warning in validation.softWarnings" :key="warning">{{ warning }}</li>
                        </ul>
                    </div>

                    <div class="flex gap-3 pt-2">
                        <button :disabled="!canSubmit || saving" @click="handleSubmit"
                            class="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50">
                            {{ saving ? 'Submitting...' : 'Submit Request' }}
                        </button>
                        <RouterLink to="/requests" class="px-4 py-2 border border-gray-300 rounded">Cancel</RouterLink>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
