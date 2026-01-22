import api from './api'

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface TimeSlot {
    id: number
    dayOfWeek: DayOfWeek
    startTime: string // HH:mm format
    endTime: string // HH:mm format
    label: string | null
}

export interface CreateTimeSlotRequest {
    dayOfWeek: DayOfWeek
    startTime: string
    endTime: string
    label?: string
}

export interface UpdateTimeSlotRequest {
    dayOfWeek?: DayOfWeek
    startTime?: string
    endTime?: string
    label?: string
}

export const DAY_OF_WEEK_OPTIONS: { value: DayOfWeek; label: string }[] = [
    { value: 'MONDAY', label: 'Monday' },
    { value: 'TUESDAY', label: 'Tuesday' },
    { value: 'WEDNESDAY', label: 'Wednesday' },
    { value: 'THURSDAY', label: 'Thursday' },
    { value: 'FRIDAY', label: 'Friday' },
    { value: 'SATURDAY', label: 'Saturday' },
    { value: 'SUNDAY', label: 'Sunday' },
]

export const timeslotsService = {
    async getAll(dayOfWeek?: DayOfWeek): Promise<TimeSlot[]> {
        const params = dayOfWeek ? `?dayOfWeek=${dayOfWeek}` : ''
        const response = await api.get<TimeSlot[]>(`/timeslots${params}`)
        return response.data
    },

    async getById(id: number): Promise<TimeSlot> {
        const response = await api.get<TimeSlot>(`/timeslots/${id}`)
        return response.data
    },

    async create(timeslot: CreateTimeSlotRequest): Promise<TimeSlot> {
        const response = await api.post<TimeSlot>('/timeslots', timeslot)
        return response.data
    },

    async update(id: number, timeslot: UpdateTimeSlotRequest): Promise<TimeSlot> {
        const response = await api.put<TimeSlot>(`/timeslots/${id}`, timeslot)
        return response.data
    },

    async delete(id: number): Promise<void> {
        await api.delete(`/timeslots/${id}`)
    },

    formatTimeSlot(slot: TimeSlot): string {
        const day = DAY_OF_WEEK_OPTIONS.find(d => d.value === slot.dayOfWeek)?.label || slot.dayOfWeek
        return `${day} ${slot.startTime} - ${slot.endTime}`
    },
}
