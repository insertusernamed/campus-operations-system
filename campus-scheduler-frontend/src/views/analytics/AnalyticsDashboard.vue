<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAsyncData } from '@/composables/useAsyncData'
import {
	analyticsService,
	type UtilizationSummary,
	type RoomUtilization,
	type BuildingUtilization,
	type PeakHours,
} from '@/services/analytics'
import { buildingsService, type Building } from '@/services/buildings'
import StatCard from '@/components/charts/StatCard.vue'
import BarChart, { type BarChartData } from '@/components/charts/BarChart.vue'
import HeatMap, { type HeatmapCell } from '@/components/charts/HeatMap.vue'

const selectedSemester = ref('Fall 2026')
const selectedBuildingId = ref<number | null>(null)
const semesters = ['Fall 2026', 'Spring 2026', 'Fall 2025', 'Summer 2025', 'Spring 2025']

const MAX_DISPLAYED_ROOMS = 15

const { data: buildings } = useAsyncData<Building[]>(() => buildingsService.getAll())

const {
	data: summary,
	loading: summaryLoading,
	error: summaryError,
	execute: fetchSummary,
} = useAsyncData<UtilizationSummary>(
	() => analyticsService.getUtilizationSummary(selectedSemester.value),
	{ immediate: false }
)

const {
	data: roomsUtilization,
	loading: roomsLoading,
	error: roomsError,
	execute: fetchRooms,
} = useAsyncData<RoomUtilization[]>(
	() => analyticsService.getAllRoomsUtilization(selectedSemester.value),
	{ immediate: false }
)

const {
	data: buildingsUtilization,
	loading: buildingsLoading,
	error: buildingsError,
	execute: fetchBuildings,
} = useAsyncData<BuildingUtilization[]>(
	() => analyticsService.getAllBuildingsUtilization(selectedSemester.value),
	{ immediate: false }
)

const {
	data: peakHours,
	loading: peakHoursLoading,
	error: peakHoursError,
	execute: fetchPeakHours,
} = useAsyncData<PeakHours[]>(
	() => analyticsService.getPeakHours(selectedSemester.value),
	{ immediate: false }
)

const isLoading = computed(
	() => summaryLoading.value || roomsLoading.value || buildingsLoading.value || peakHoursLoading.value
)

const filteredRooms = computed(() => {
	if (!roomsUtilization.value) return []
	if (!selectedBuildingId.value) return roomsUtilization.value
	const building = buildings.value?.find((b) => b.id === selectedBuildingId.value)
	if (!building) return roomsUtilization.value
	return roomsUtilization.value.filter((r) => r.buildingName === building.name)
})

const buildingChartData = computed<BarChartData[]>(() => {
	if (!buildingsUtilization.value) return []
	return buildingsUtilization.value
		.map((b) => ({
			label: b.buildingCode || b.buildingName,
			value: b.utilizationPercentage,
		}))
		.sort((a, b) => b.value - a.value)
})

const roomChartData = computed<BarChartData[]>(() => {
	return filteredRooms.value
		.sort((a, b) => b.utilizationPercentage - a.utilizationPercentage)
		.slice(0, MAX_DISPLAYED_ROOMS)
		.map((r) => ({
			label: `${r.buildingCode}-${r.roomNumber}`,
			value: r.utilizationPercentage,
		}))
})

const daysOfWeek = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri']

const timeSlots = computed(() => {
	if (!peakHours.value) return []
	const slots = new Set<string>()
	peakHours.value.forEach((ph) => slots.add(ph.startTime))
	return Array.from(slots).sort()
})

const heatmapData = computed<HeatmapCell[]>(() => {
	if (!peakHours.value) return []
	return peakHours.value.map((ph) => ({
		row: dayLabels[daysOfWeek.indexOf(ph.dayOfWeek)] || ph.dayOfWeek,
		col: ph.startTime,
		value: ph.bookingCount,
		label: `${ph.label}: ${ph.bookingCount} bookings`,
	}))
})

const formatPercent = (value: number) => `${value.toFixed(1)}%`

async function fetchAllData() {
	await Promise.allSettled([fetchSummary(), fetchRooms(), fetchBuildings(), fetchPeakHours()])
}

watch(selectedSemester, fetchAllData)
onMounted(fetchAllData)
</script>

<template>
	<div>
		<h1 class="text-2xl font-bold mb-4">Analytics</h1>

		<!-- Filters -->
		<div class="border p-4 mb-6 flex flex-wrap gap-4">
			<div>
				<label for="semester" class="text-sm mr-2">Semester:</label>
				<select id="semester" v-model="selectedSemester" class="border px-2 py-1">
					<option v-for="sem in semesters" :key="sem" :value="sem">{{ sem }}</option>
				</select>
			</div>
			<div>
				<label for="building" class="text-sm mr-2">Building:</label>
				<select id="building" v-model="selectedBuildingId" class="border px-2 py-1">
					<option :value="null">All</option>
					<option v-for="b in buildings" :key="b.id" :value="b.id">{{ b.name }}</option>
				</select>
			</div>
		</div>

		<div v-if="isLoading" class="py-12 text-center text-gray-500">Loading...</div>

		<div v-else-if="summaryError || roomsError || buildingsError || peakHoursError"
			class="border border-red-300 p-4 text-red-600">
			<p v-if="summaryError">{{ summaryError }}</p>
			<p v-if="roomsError">Room data error: {{ roomsError }}</p>
			<p v-if="buildingsError">Building data error: {{ buildingsError }}</p>
			<p v-if="peakHoursError">Peak hours error: {{ peakHoursError }}</p>
			<button @click="fetchAllData" class="mt-2 underline">Retry</button>
		</div>

		<template v-else-if="summary">
			<!-- Summary -->
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
				<StatCard label="Overall Utilization" :value="formatPercent(summary.overallUtilizationPercentage)">
					<template #labelInfo>
						<span
							v-tooltip="'Percentage of available room-time slots that are booked. Higher utilization means resources are being used more efficiently.'"
							class="cursor-help text-gray-400 hover:text-gray-600">ⓘ</span>
					</template>
				</StatCard>
				<StatCard label="Total Rooms" :value="summary.totalRooms" />
				<StatCard label="Buildings" :value="summary.totalBuildings" />
				<StatCard label="Scheduled Slots"
					:value="`${summary.totalScheduledSlots}/${summary.totalAvailableSlots}`" />
			</div>

			<!-- Charts -->
			<div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
				<div class="border p-4">
					<BarChart title="Building Utilization" :data="buildingChartData" :height="280" :max-value="100"
						:value-formatter="formatPercent" />
				</div>
				<div class="border p-4">
					<BarChart title="Room Utilization" :data="roomChartData" :height="280" :max-value="100"
						:value-formatter="formatPercent" />
				</div>
			</div>

			<!-- Heatmap -->
			<div class="border p-4 mb-6">
				<HeatMap title="Schedule Heatmap" :data="heatmapData" :rows="dayLabels" :cols="timeSlots"
					:cell-width="70" :cell-height="40" />
			</div>

			<!-- Room Lists -->
			<div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
				<div class="border p-4">
					<h3 class="font-semibold mb-3">Top Utilized Rooms</h3>
					<table class="w-full text-sm">
						<tbody>
							<tr v-for="room in summary.topUtilizedRooms" :key="room.roomId" class="border-b">
								<td class="py-2">{{ room.buildingCode }}-{{ room.roomNumber }}</td>
								<td class="py-2 text-gray-500">{{ room.buildingName }}</td>
								<td class="py-2 text-right">{{ formatPercent(room.utilizationPercentage) }}</td>
							</tr>
						</tbody>
					</table>
					<p v-if="!summary.topUtilizedRooms.length" class="text-gray-500 py-4">No data</p>
				</div>

				<div class="border p-4">
					<h3 class="font-semibold mb-3">Least Utilized Rooms</h3>
					<table class="w-full text-sm">
						<tbody>
							<tr v-for="room in summary.leastUtilizedRooms" :key="room.roomId" class="border-b">
								<td class="py-2">{{ room.buildingCode }}-{{ room.roomNumber }}</td>
								<td class="py-2 text-gray-500">{{ room.buildingName }}</td>
								<td class="py-2 text-right">{{ formatPercent(room.utilizationPercentage) }}</td>
							</tr>
						</tbody>
					</table>
					<p v-if="!summary.leastUtilizedRooms.length" class="text-gray-500 py-4">No data</p>
				</div>
			</div>
		</template>

		<div v-else class="border p-8 text-center text-gray-500">
			No analytics data available for the selected semester.
		</div>
	</div>
</template>
