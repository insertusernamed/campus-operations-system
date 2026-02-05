<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { toast } from 'vue3-toastify'
import BaseModal from '@/components/common/BaseModal.vue'
import { changeRequestsService, type ScheduleChangeRequest, type ChangeRequestStatus } from '@/services/changeRequests'
import { roomsService, type Room } from '@/services/rooms'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import EmptyState from '@/components/common/EmptyState.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'
import api from '@/services/api'

interface ImpactMove {
    scheduleId: number
    courseCode: string
    fromRoomLabel: string
    toRoomLabel: string
    fromTimeSlotLabel: string
    toTimeSlotLabel: string
    toRoomId: number
    toTimeSlotId: number
}

interface ImpactResponse {
    status: 'SOLVED' | 'NO_SOLUTION'
    score: string | null
    scoreSummary: string | null
    moves: ImpactMove[]
}

const loading = ref(false)
const error = ref<string | null>(null)
const requests = ref<ScheduleChangeRequest[]>([])
const statusFilter = ref<ChangeRequestStatus | 'ALL'>('ALL')
const rooms = ref<Room[]>([])
const timeSlots = ref<TimeSlot[]>([])

const modalOpen = ref(false)
const selectedRequest = ref<ScheduleChangeRequest | null>(null)
const decisionNote = ref('')
const overrideRoomId = ref<number | null>(null)
const overrideTimeSlotId = ref<number | null>(null)
const validation = ref<{ hardConflicts: string[]; softWarnings: string[] }>({
    hardConflicts: [],
    softWarnings: [],
})

const impact = ref<ImpactResponse | null>(null)
const impactLoading = ref(false)

const showAnalyze = computed(() => {
    if (!overrideRoomId.value && !overrideTimeSlotId.value) {
        return false
    }
    return validation.value.hardConflicts.length > 0 || validation.value.softWarnings.length > 0
})

const filteredRequests = computed(() => {
    if (statusFilter.value === 'ALL') return requests.value
    return requests.value.filter(req => req.status === statusFilter.value)
})

async function loadData() {
    loading.value = true
    error.value = null
    try {
        const [requestData, roomData, timeSlotData] = await Promise.all([
            changeRequestsService.getAll(),
            roomsService.getAll(),
            timeslotsService.getAll(),
        ])
        requests.value = requestData
        rooms.value = roomData
        timeSlots.value = timeSlotData
    } catch (e) {
        console.error(e)
        error.value = 'Failed to load requests'
    } finally {
        loading.value = false
    }
}

function openModal(request: ScheduleChangeRequest) {
    selectedRequest.value = request
    decisionNote.value = ''
    overrideRoomId.value = request.proposedRoom?.id ?? null
    overrideTimeSlotId.value = request.proposedTimeSlot?.id ?? null
    validation.value = { hardConflicts: [], softWarnings: [] }
    impact.value = null
    modalOpen.value = true
    runValidation()
}

async function runValidation() {
    if (!selectedRequest.value) return
    if (!overrideRoomId.value && !overrideTimeSlotId.value) {
        validation.value = { hardConflicts: [], softWarnings: [] }
        return
    }
    try {
        const result = await changeRequestsService.validate({
            scheduleId: selectedRequest.value.schedule.id,
            proposedRoomId: overrideRoomId.value,
            proposedTimeSlotId: overrideTimeSlotId.value,
        })
        validation.value = {
            hardConflicts: result.hardConflicts,
            softWarnings: result.softWarnings,
        }
    } catch (e) {
        console.error(e)
    }
}

async function approveRequest() {
    if (!selectedRequest.value) return
    try {
        await changeRequestsService.approve(selectedRequest.value.id, {
            decisionNote: decisionNote.value || undefined,
            proposedRoomId: overrideRoomId.value,
            proposedTimeSlotId: overrideTimeSlotId.value,
        })
        toast.success('Request approved')
        modalOpen.value = false
        await loadData()
    } catch (e: any) {
        const message = e?.response?.data?.error || 'Failed to approve request'
        toast.error(message)
    }
}

async function rejectRequest() {
    if (!selectedRequest.value) return
    try {
        await changeRequestsService.reject(selectedRequest.value.id, {
            decisionNote: decisionNote.value || undefined,
        })
        toast.success('Request rejected')
        modalOpen.value = false
        await loadData()
    } catch (e: any) {
        const message = e?.response?.data?.error || 'Failed to reject request'
        toast.error(message)
    }
}

async function analyzeImpact() {
    if (!selectedRequest.value) return
    impactLoading.value = true
    impact.value = null
    try {
        const response = await api.post<ImpactResponse>('/solver/impact', {
            scheduleId: selectedRequest.value.schedule.id,
            proposedRoomId: overrideRoomId.value,
            proposedTimeSlotId: overrideTimeSlotId.value,
        })
        impact.value = response.data
        if (impact.value?.moves?.length) {
            const match = impact.value.moves.find(move => move.scheduleId === selectedRequest.value?.schedule.id)
            if (match) {
                overrideRoomId.value = match.toRoomId
                overrideTimeSlotId.value = match.toTimeSlotId
            }
        }
    } catch (e: any) {
        const message = e?.response?.data?.error || 'Impact analysis failed'
        toast.error(message)
    } finally {
        impactLoading.value = false
    }
}

const reasonLabel = (category: string) => category.replace(/_/g, ' ')

onMounted(loadData)
</script>

<template>
    <div>
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Change Requests</h1>
            <select v-model="statusFilter" class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700">
                <option value="ALL">All</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="REJECTED">Rejected</option>
            </select>
        </div>

        <TableSkeleton v-if="loading" :columns="6" :rows="6" />
        <div v-else-if="error" class="text-red-600">{{ error }}</div>
        <EmptyState v-else-if="filteredRequests.length === 0" title="No change requests"
            description="Requests from instructors will show up here." />

        <table v-else class="w-full bg-white border border-gray-200">
            <thead>
                <tr class="bg-gray-50 border-b border-gray-200">
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Course</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Instructor</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Reason</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Status</th>
                    <th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Actions</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="request in filteredRequests" :key="request.id" class="border-b border-gray-100">
                    <td class="px-4 py-3">
                        <div class="text-sm text-gray-900">{{ request.schedule.course.code }}</div>
                        <div class="text-xs text-gray-500">{{ request.schedule.course.name }}</div>
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600">
                        {{ request.requestedByInstructor.firstName }} {{ request.requestedByInstructor.lastName }}
                    </td>
                    <td class="px-4 py-3 text-sm text-gray-600">
                        {{ reasonLabel(request.reasonCategory) }}
                    </td>
                    <td class="px-4 py-3 text-sm">
                        <span class="px-2 py-1 rounded bg-gray-100 text-gray-700">{{ request.status }}</span>
                    </td>
                    <td class="px-4 py-3 text-sm">
                        <button v-if="request.status === 'PENDING'" @click="openModal(request)"
                            class="text-blue-600 hover:underline">Review</button>
                    </td>
                </tr>
            </tbody>
        </table>

        <BaseModal v-model="modalOpen" title="Review Request">
            <template #default>
                <div v-if="selectedRequest" class="space-y-4">
                    <div>
                        <div class="text-sm text-gray-500">Course</div>
                        <div class="text-sm text-gray-900">{{ selectedRequest.schedule.course.code }} - {{ selectedRequest.schedule.course.name }}</div>
                    </div>
                    <div>
                        <div class="text-sm text-gray-500">Reason</div>
                        <div class="text-sm text-gray-900">
                            {{ reasonLabel(selectedRequest.reasonCategory) }}
                            <div v-if="selectedRequest.reasonDetails" class="text-xs text-gray-500">{{ selectedRequest.reasonDetails }}</div>
                        </div>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Room Override</label>
                        <select v-model="overrideRoomId" @change="runValidation"
                            class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option :value="null">Keep current</option>
                            <option v-for="room in rooms" :key="room.id" :value="room.id">
                                {{ room.buildingCode }} {{ room.roomNumber }} ({{ room.capacity }} seats)
                            </option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Time Slot Override</label>
                        <select v-model="overrideTimeSlotId" @change="runValidation"
                            class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option :value="null">Keep current</option>
                            <option v-for="slot in timeSlots" :key="slot.id" :value="slot.id">
                                {{ timeslotsService.formatTimeSlot(slot) }}
                            </option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">Decision Note</label>
                        <textarea v-model="decisionNote" rows="2" class="w-full px-3 py-2 border border-gray-300 rounded"></textarea>
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

                    <div v-if="impact" class="bg-gray-50 border border-gray-200 text-gray-700 p-3 rounded">
                        <p class="text-sm font-medium">Impact Analysis</p>
                        <p class="text-xs text-gray-500">{{ impact.scoreSummary }}</p>
                        <ul v-if="impact.moves?.length" class="mt-2 text-xs">
                            <li v-for="move in impact.moves" :key="move.scheduleId">
                                {{ move.courseCode }}: {{ move.fromRoomLabel }} / {{ move.fromTimeSlotLabel }} →
                                {{ move.toRoomLabel }} / {{ move.toTimeSlotLabel }}
                            </li>
                        </ul>
                        <p v-else class="text-xs text-gray-500">No moves suggested.</p>
                    </div>
                </div>
            </template>
            <template #footer>
                <div class="flex flex-wrap gap-3">
                    <button v-if="showAnalyze" @click="analyzeImpact" :disabled="impactLoading"
                        class="px-3 py-2 border border-gray-300 rounded">
                        {{ impactLoading ? 'Analyzing...' : 'Analyze Impact' }}
                    </button>
                    <button @click="approveRequest" class="px-3 py-2 bg-blue-600 text-white rounded">Approve</button>
                    <button @click="rejectRequest" class="px-3 py-2 border border-gray-300 rounded">Reject</button>
                </div>
            </template>
        </BaseModal>
    </div>
</template>
