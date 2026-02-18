<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter, useRoute } from 'vue-router'
import { toast } from 'vue3-toastify'
import { useRole } from '@/composables/useRole'
import { changeRequestsService } from '@/services/changeRequests'
import { schedulesService, type Schedule } from '@/services/schedules'
import { roomsService, type Room } from '@/services/rooms'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import {
    buildReasonDetails,
    buildReasonTemplate,
    changeRequestIssueOptions,
    resolveIssueOption,
    type ChangeRequestIssue,
} from '@/constants/changeRequestIssues'
import { solverService, type ImpactAnalysisMove, type ImpactAnalysisResponse } from '@/services/solver'
import ScheduleCalendar from '@/components/calendar/ScheduleCalendar.vue'

const router = useRouter()
const route = useRoute()
const { role, instructorId } = useRole()

const schedules = ref<Schedule[]>([])
const rooms = ref<Room[]>([])
const timeSlots = ref<TimeSlot[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const prefillError = ref<string | null>(null)
const suggestionsLoading = ref(false)
const suggestionsError = ref<string | null>(null)
const validationError = ref<string | null>(null)

const form = ref({
    scheduleId: 0,
    proposedRoomId: null as number | null,
    proposedTimeSlotId: null as number | null,
    issue: '' as ChangeRequestIssue | '',
    notes: '',
})

type RoomScope = 'any' | 'same-building'

const roomScope = ref<RoomScope>('any')

const validation = ref<{ hardConflicts: string[]; softWarnings: string[] }>({
    hardConflicts: [],
    softWarnings: [],
})

interface ImpactSuggestion {
    id: string
    timeSlot: TimeSlot
    room: Room
    impact: ImpactAnalysisResponse
}

const suggestions = ref<ImpactSuggestion[]>([])
const selectedSuggestionId = ref<string | null>(null)
const lastAutoFilledTemplate = ref('')

const selectedSchedule = computed(() => schedules.value.find(schedule => schedule.id === form.value.scheduleId) ?? null)
const selectedSuggestion = computed(() => suggestions.value.find(suggestion => suggestion.id === selectedSuggestionId.value) ?? null)
const OTHER_MIN_DETAILS_LENGTH = 30

const isOtherIssue = computed(() => form.value.issue === 'OTHER')
const otherTemplate = computed(() => buildReasonTemplate('OTHER').trim())
const notesLength = computed(() => form.value.notes.trim().length)
const isOtherTemplateUnchanged = computed(() => {
    return isOtherIssue.value && form.value.notes.trim() === otherTemplate.value
})
const isOtherDetailsTooShort = computed(() => {
    return isOtherIssue.value && notesLength.value < OTHER_MIN_DETAILS_LENGTH
})
const isOtherDetailsValid = computed(() => {
    if (!isOtherIssue.value) return true
    return !isOtherTemplateUnchanged.value && !isOtherDetailsTooShort.value
})

const canSubmit = computed(() => {
    return (
        !!form.value.scheduleId &&
        !!form.value.issue &&
        (form.value.proposedRoomId || form.value.proposedTimeSlotId) &&
        isOtherDetailsValid.value
    )
})

const availableSchedules = computed(() => schedules.value)
const canGenerateSuggestions = computed(() => !!selectedSchedule.value && !!form.value.issue)

function formatRoom(room: Room): string {
    const parts = [room.buildingCode, room.roomNumber].filter(Boolean)
    return parts.join(' ')
}

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

function timeToMinutes(time: string): number {
    const [rawHours = '0', rawMinutes = '0'] = time.split(':')
    const hours = Number(rawHours)
    const minutes = Number(rawMinutes)
    if (Number.isNaN(hours) || Number.isNaN(minutes)) {
        return 0
    }
    return hours * 60 + minutes
}

function overlaps(a: TimeSlot, b: TimeSlot): boolean {
    if (a.dayOfWeek !== b.dayOfWeek) return false
    const aStart = timeToMinutes(a.startTime)
    const aEnd = timeToMinutes(a.endTime)
    const bStart = timeToMinutes(b.startTime)
    const bEnd = timeToMinutes(b.endTime)
    return aStart < bEnd && aEnd > bStart
}

function getCandidateTimeSlots(schedule: Schedule, issue: ChangeRequestIssue): TimeSlot[] {
    const day = schedule.timeSlot.dayOfWeek
    const currentStart = timeToMinutes(schedule.timeSlot.startTime)
    const currentEnd = timeToMinutes(schedule.timeSlot.endTime)

    const sameDayTimeSlots = timeSlots.value.filter(slot => slot.dayOfWeek === day && slot.id !== schedule.timeSlot.id)
    const instructorOtherSlots = schedules.value
        .filter(item => item.id !== schedule.id && item.timeSlot.dayOfWeek === day)
        .map(item => item.timeSlot)

    const availableSlots = sameDayTimeSlots.filter(slot =>
        !instructorOtherSlots.some(other => overlaps(slot, other))
    )

    const previous = schedules.value
        .filter(item => item.id !== schedule.id && item.timeSlot.dayOfWeek === day)
        .filter(item => timeToMinutes(item.timeSlot.endTime) <= currentStart)
        .sort((a, b) => timeToMinutes(b.timeSlot.endTime) - timeToMinutes(a.timeSlot.endTime))[0]

    const next = schedules.value
        .filter(item => item.id !== schedule.id && item.timeSlot.dayOfWeek === day)
        .filter(item => timeToMinutes(item.timeSlot.startTime) >= currentEnd)
        .sort((a, b) => timeToMinutes(a.timeSlot.startTime) - timeToMinutes(b.timeSlot.startTime))[0]

    let candidates = availableSlots

    if (issue === 'GAP_TOO_LARGE_BEFORE') {
        const previousEnd = previous ? timeToMinutes(previous.timeSlot.endTime) : null
        candidates = availableSlots.filter(slot => timeToMinutes(slot.startTime) < currentStart)
        if (previousEnd !== null) {
            candidates = candidates.filter(slot => timeToMinutes(slot.startTime) >= previousEnd)
            candidates = candidates.sort((a, b) => (timeToMinutes(a.startTime) - previousEnd) - (timeToMinutes(b.startTime) - previousEnd))
        } else {
            candidates = candidates.sort((a, b) => Math.abs(currentStart - timeToMinutes(a.startTime)) - Math.abs(currentStart - timeToMinutes(b.startTime)))
        }
    } else if (issue === 'GAP_TOO_LARGE_AFTER') {
        const nextStart = next ? timeToMinutes(next.timeSlot.startTime) : null
        candidates = availableSlots.filter(slot => timeToMinutes(slot.startTime) > currentStart)
        if (nextStart !== null) {
            candidates = candidates.filter(slot => timeToMinutes(slot.endTime) <= nextStart)
            candidates = candidates.sort((a, b) => (nextStart - timeToMinutes(a.endTime)) - (nextStart - timeToMinutes(b.endTime)))
        } else {
            candidates = candidates.sort((a, b) => (timeToMinutes(a.startTime) - currentEnd) - (timeToMinutes(b.startTime) - currentEnd))
        }
    } else {
        candidates = availableSlots.sort((a, b) => Math.abs(currentStart - timeToMinutes(a.startTime)) - Math.abs(currentStart - timeToMinutes(b.startTime)))
    }

    if (candidates.length === 0) {
        candidates = availableSlots.sort((a, b) => Math.abs(currentStart - timeToMinutes(a.startTime)) - Math.abs(currentStart - timeToMinutes(b.startTime)))
    }

    return candidates
}

function getCandidateRooms(schedule: Schedule, scope: RoomScope): Room[] {
    const courseCapacity = schedule.course.enrollmentCapacity
    const roomMap = new Map<number, Room>()

    const roomsToConsider = scope === 'same-building' && schedule.room.buildingId
        ? rooms.value.filter(room => room.buildingId === schedule.room.buildingId)
        : rooms.value

    roomsToConsider.forEach(room => roomMap.set(room.id, room))

    if (!roomMap.has(schedule.room.id)) {
        roomMap.set(schedule.room.id, schedule.room)
    }

    const uniqueRooms = Array.from(roomMap.values())

    uniqueRooms.sort((a, b) => {
        const aFits = a.capacity >= courseCapacity
        const bFits = b.capacity >= courseCapacity
        if (aFits !== bFits) return aFits ? -1 : 1
        const aDiff = Math.abs(a.capacity - courseCapacity)
        const bDiff = Math.abs(b.capacity - courseCapacity)
        if (aDiff !== bDiff) return aDiff - bDiff
        return a.roomNumber.localeCompare(b.roomNumber)
    })

    return uniqueRooms
}

function applySuggestion(suggestion: ImpactSuggestion) {
    selectedSuggestionId.value = suggestion.id
    form.value.proposedTimeSlotId = suggestion.timeSlot.id
    form.value.proposedRoomId = suggestion.room.id
}

function buildPreviewSchedules(moves: ImpactAnalysisMove[]): Schedule[] {
    if (moves.length === 0) {
        return schedules.value
    }

    const moveMap = new Map<number, ImpactAnalysisMove>(moves.map(move => [move.scheduleId, move]))

    return schedules.value.map(schedule => {
        const move = moveMap.get(schedule.id)
        if (!move) return schedule

        const nextRoom = rooms.value.find(room => room.id === move.toRoomId) ?? schedule.room
        const nextTimeSlot = timeSlots.value.find(slot => slot.id === move.toTimeSlotId) ?? schedule.timeSlot

        return {
            ...schedule,
            room: nextRoom,
            timeSlot: nextTimeSlot,
        }
    })
}

const previewSchedules = computed(() => {
    if (!selectedSuggestion.value) return []
    return buildPreviewSchedules(selectedSuggestion.value.impact.moves ?? [])
})

const previewMoveMeta = computed(() => {
    if (!selectedSchedule.value || !selectedSuggestion.value) return null
    const movedTime = selectedSuggestion.value.timeSlot.id !== selectedSchedule.value.timeSlot.id
    const movedRoom = selectedSuggestion.value.room.id !== selectedSchedule.value.room.id
    if (!movedTime && !movedRoom) return null
    return {
        scheduleId: selectedSchedule.value.id,
        ghost: selectedSchedule.value,
    }
})

const previewGhostSchedules = computed(() => {
    return previewMoveMeta.value ? [previewMoveMeta.value.ghost] : []
})

const previewMovedIds = computed(() => {
    return previewMoveMeta.value ? [previewMoveMeta.value.scheduleId] : []
})

const previewArrowId = computed(() => {
    return previewMoveMeta.value ? previewMoveMeta.value.scheduleId : null
})

function suggestionSummary(suggestion: ImpactSuggestion): string {
    const moves = suggestion.impact.moves ?? []
    const chain = moves.length <= 1
        ? 'No chain reaction needed.'
        : `${moves.length - 1} other class${moves.length === 2 ? '' : 'es'} would move.`

    if (suggestion.impact.status === 'NO_SOLUTION') {
        return `${chain} Hard conflicts remain.`
    }

    return chain
}

async function generateSuggestions() {
    suggestionsError.value = null
    suggestions.value = []
    selectedSuggestionId.value = null

    if (!selectedSchedule.value || !form.value.issue) {
        suggestionsError.value = 'Select a class and reason first.'
        return
    }

    const candidateTimeSlots = getCandidateTimeSlots(selectedSchedule.value, form.value.issue)
    if (candidateTimeSlots.length === 0) {
        suggestionsError.value = 'No available time slots found for this day.'
        return
    }

    const candidateRooms = getCandidateRooms(selectedSchedule.value, roomScope.value)
    if (candidateRooms.length === 0) {
        suggestionsError.value = 'No rooms available for this option.'
        return
    }

    suggestionsLoading.value = true
    try {
        // Limit how many slot-room combinations we evaluate per click.
        const maxPairs = 6
        const pairs: { slot: TimeSlot; room: Room }[] = []
        const slots = candidateTimeSlots.slice(0, 3)
        const roomsForSlots = candidateRooms.slice(0, 3)

        for (const slot of slots) {
            for (const room of roomsForSlots) {
                if (pairs.length >= maxPairs) break
                pairs.push({ slot, room })
            }
            if (pairs.length >= maxPairs) break
        }

        const results = await Promise.allSettled(
            pairs.map(async ({ slot, room }) => {
                const impact = await solverService.analyzeImpact({
                    scheduleId: selectedSchedule.value!.id,
                    proposedTimeSlotId: slot.id,
                    proposedRoomId: room.id,
                })
                return { slot, room, impact }
            })
        )

        const resolved = results
            .filter((result): result is PromiseFulfilledResult<{ slot: TimeSlot; room: Room; impact: ImpactAnalysisResponse }> => result.status === 'fulfilled')
            .map(result => result.value)
            .filter(result => (result.impact.moves ?? []).some(move => move.scheduleId === selectedSchedule.value!.id))
            .sort((a, b) => (a.impact.moves?.length ?? 0) - (b.impact.moves?.length ?? 0))
            .slice(0, 3)

        suggestions.value = resolved.map((result) => ({
            id: `slot-${result.slot.id}-room-${result.room.id}`,
            timeSlot: result.slot,
            room: result.room,
            impact: result.impact,
        }))

        if (suggestions.value.length === 0) {
            suggestionsError.value = 'No viable options found. Try adjusting manually.'
        } else {
            const firstSuggestion = suggestions.value[0]
            if (firstSuggestion) {
                applySuggestion(firstSuggestion)
            }
        }
    } catch (e) {
        console.error(e)
        suggestionsError.value = 'Failed to generate options. Please try again.'
    } finally {
        suggestionsLoading.value = false
    }
}

function applySchedulePrefill() {
    prefillError.value = null
	const rawScheduleId = route.query.scheduleId
	if (typeof rawScheduleId !== 'string') return
	const parsedId = Number(rawScheduleId)
	if (!Number.isFinite(parsedId) || parsedId <= 0) return
    const exists = schedules.value.some(schedule => schedule.id === parsedId)
    if (!exists) {
        form.value.scheduleId = 0
        prefillError.value = 'The selected class is unavailable. Please choose a class from the list.'
        return
    }
    form.value.scheduleId = parsedId
}

function applyIssuePrefill() {
    const rawIssue = route.query.issue
    if (typeof rawIssue !== 'string') return
    if (resolveIssueOption(rawIssue)) {
        form.value.issue = rawIssue as ChangeRequestIssue
    }
}

async function runValidation() {
    validation.value = { hardConflicts: [], softWarnings: [] }
    validationError.value = null
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
        validationError.value = 'Failed to validate this option. Conflict checks may be incomplete.'
    }
}

async function handleSubmit() {
    if (!canSubmit.value) {
        if (isOtherIssue.value && isOtherTemplateUnchanged.value) {
            error.value = 'Add specific details for "Other" instead of keeping the template text'
            return
        }
        if (isOtherIssue.value && isOtherDetailsTooShort.value) {
            error.value = `Provide at least ${OTHER_MIN_DETAILS_LENGTH} characters for "Other" details`
            return
        }
        error.value = 'Select a schedule, reason, and proposed change'
        return
    }
    if (!instructorId.value) {
        error.value = 'Select an instructor to continue'
        return
    }

    saving.value = true
    error.value = null
    try {
        const issueOption = resolveIssueOption(form.value.issue)
        const reasonCategory = issueOption?.category ?? 'OTHER'
        const reasonDetails = buildReasonDetails(form.value.issue, form.value.notes)

        await changeRequestsService.create({
            scheduleId: form.value.scheduleId,
            requestedByInstructorId: instructorId.value,
            requestedByRole: role.value === 'admin' ? 'ADMIN' : 'INSTRUCTOR',
            reasonCategory,
            reasonDetails,
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

onMounted(async () => {
    await loadData()
    applySchedulePrefill()
    applyIssuePrefill()
})

watch(() => route.query.scheduleId, () => {
    applySchedulePrefill()
})

watch(() => route.query.issue, () => {
    applyIssuePrefill()
})

watch(() => form.value.scheduleId, () => {
    suggestions.value = []
    selectedSuggestionId.value = null
    suggestionsError.value = null
})

watch(() => form.value.issue, issue => {
    suggestions.value = []
    selectedSuggestionId.value = null
    suggestionsError.value = null

    if (!issue) {
        lastAutoFilledTemplate.value = ''
        return
    }

    const template = buildReasonTemplate(issue)
    const notes = form.value.notes
    const canOverwriteNotes =
        notes.trim() === '' || notes === lastAutoFilledTemplate.value

    if (!canOverwriteNotes || template.length === 0) {
        return
    }

    form.value.notes = template
    lastAutoFilledTemplate.value = template
})

watch(roomScope, () => {
    suggestions.value = []
    selectedSuggestionId.value = null
    suggestionsError.value = null
})
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
                <div v-if="prefillError" class="mb-4 text-red-600">{{ prefillError }}</div>

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
                        <label class="block text-sm font-medium text-gray-700 mb-1">Why is this a problem?</label>
                        <select v-model="form.issue" class="w-full px-3 py-2 border border-gray-300 rounded">
                            <option value="" disabled>Select a reason</option>
                            <option v-for="option in changeRequestIssueOptions" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                    </div>

                    <div class="border border-gray-200 rounded p-4 space-y-3">
                        <div class="flex items-center justify-between">
                            <div class="text-sm font-medium text-gray-900">Solver suggestions</div>
                            <button :disabled="!canGenerateSuggestions || suggestionsLoading"
                                class="px-3 py-1.5 text-sm border border-gray-300 rounded disabled:opacity-50"
                                @click="generateSuggestions">
                                {{ suggestionsLoading ? 'Generating...' : 'Generate options' }}
                            </button>
                        </div>
                        <div class="text-sm text-gray-600">
                            Room scope
                            <div class="mt-2 space-y-2 text-sm text-gray-700">
                                <label class="flex items-start gap-2">
                                    <input type="radio" value="any" v-model="roomScope" class="mt-1" />
                                    <span>
                                        Any building (recommended)
                                        <span class="block text-xs text-gray-500">Best chance of a workable option.</span>
                                    </span>
                                </label>
                                <label class="flex items-start gap-2">
                                    <input type="radio" value="same-building" v-model="roomScope" class="mt-1" />
                                    <span>
                                        Same building only
                                        <span class="block text-xs text-gray-500">More likely to conflict.</span>
                                    </span>
                                </label>
                            </div>
                        </div>
                        <div class="border-t border-gray-200" />
                        <div v-if="suggestionsError" class="text-sm text-red-600">{{ suggestionsError }}</div>
                        <div v-else-if="suggestions.length === 0" class="text-sm text-gray-500">
                            Generate options to see suggestions.
                        </div>
                        <div v-else class="space-y-2">
                            <label v-for="suggestion in suggestions" :key="suggestion.id"
                                class="flex items-start gap-2 text-sm text-gray-700">
                                <input type="radio" class="mt-1" :value="suggestion.id" v-model="selectedSuggestionId"
                                    @change="applySuggestion(suggestion)" />
                                <div>
                                    <div class="font-medium text-gray-900">
                                        {{ timeslotsService.formatTimeSlot(suggestion.timeSlot) }} • {{ formatRoom(suggestion.room) }}
                                    </div>
                                    <div class="text-xs text-gray-500">
                                        {{ suggestionSummary(suggestion) }}
                                    </div>
                                </div>
                            </label>
                        </div>
                        <div v-if="selectedSuggestion" class="pt-2">
                            <div class="text-sm font-medium text-gray-900 mb-2">Calendar preview</div>
                            <ScheduleCalendar
                                :schedules="previewSchedules"
                                :height="520"
                                :ghost-schedules="previewGhostSchedules"
                                :moved-schedule-ids="previewMovedIds"
                                :arrow-schedule-id="previewArrowId"
                            />
                        </div>
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
                        <label class="block text-sm font-medium text-gray-700 mb-1">Additional details (optional)</label>
                        <textarea v-model="form.notes" rows="3"
                            class="w-full px-3 py-2 border border-gray-300 rounded"></textarea>
                        <div v-if="isOtherIssue" class="mt-1 flex items-center justify-between text-xs">
                            <span :class="isOtherDetailsTooShort ? 'text-amber-700' : 'text-gray-500'">
                                {{ notesLength }}/{{ OTHER_MIN_DETAILS_LENGTH }} minimum characters for "Other"
                            </span>
                            <span v-if="isOtherTemplateUnchanged" class="text-amber-700">
                                Replace the template with your own details
                            </span>
                        </div>
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
                    <div v-if="validationError" class="text-sm text-red-600">{{ validationError }}</div>

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
