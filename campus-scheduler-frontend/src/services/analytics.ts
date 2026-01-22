import api from './api'

export interface RoomUtilization {
    roomId: number
    roomNumber: string
    buildingName: string
    buildingCode: string
    capacity: number
    scheduledSlots: number
    totalSlots: number
    utilizationPercentage: number
}

export interface BuildingUtilization {
    buildingId: number
    buildingName: string
    buildingCode: string
    roomCount: number
    scheduledSlots: number
    totalSlots: number
    utilizationPercentage: number
}

export interface PeakHours {
    timeSlotId: number
    dayOfWeek: string
    startTime: string
    endTime: string
    label: string
    bookingCount: number
}

export interface UtilizationSummary {
    semester: string
    totalRooms: number
    totalBuildings: number
    totalScheduledSlots: number
    totalAvailableSlots: number
    overallUtilizationPercentage: number
    topUtilizedRooms: RoomUtilization[]
    leastUtilizedRooms: RoomUtilization[]
}

export const analyticsService = {
    async getAllRoomsUtilization(semester: string): Promise<RoomUtilization[]> {
        const response = await api.get<RoomUtilization[]>('/analytics/rooms', {
            params: { semester },
        })
        return response.data
    },

    async getRoomUtilization(id: number, semester: string): Promise<RoomUtilization> {
        const response = await api.get<RoomUtilization>(`/analytics/rooms/${id}`, {
            params: { semester },
        })
        return response.data
    },

    async getAllBuildingsUtilization(semester: string): Promise<BuildingUtilization[]> {
        const response = await api.get<BuildingUtilization[]>('/analytics/buildings', {
            params: { semester },
        })
        return response.data
    },

    async getBuildingUtilization(id: number, semester: string): Promise<BuildingUtilization> {
        const response = await api.get<BuildingUtilization>(`/analytics/buildings/${id}`, {
            params: { semester },
        })
        return response.data
    },

    async getPeakHours(semester: string): Promise<PeakHours[]> {
        const response = await api.get<PeakHours[]>('/analytics/peak-hours', {
            params: { semester },
        })
        return response.data
    },

    async getUnderusedRooms(semester: string, threshold = 30): Promise<RoomUtilization[]> {
        const response = await api.get<RoomUtilization[]>('/analytics/underused', {
            params: { semester, threshold },
        })
        return response.data
    },

    async getUtilizationSummary(semester: string): Promise<UtilizationSummary> {
        const response = await api.get<UtilizationSummary>('/analytics/summary', {
            params: { semester },
        })
        return response.data
    },
}
