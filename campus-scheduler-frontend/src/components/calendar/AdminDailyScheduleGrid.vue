<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Schedule } from '@/services/schedules'
import type { Building } from '@/services/buildings'
import type { Room } from '@/services/rooms'
import type { DayOfWeek } from '@/services/timeslots'

const props = defineProps<{
	schedules: Schedule[]
	buildings: Building[]
	rooms: Room[]
	selectedBuildingId: number | null
	selectedRoomId: number | null
}>()

const emit = defineEmits<{
	(e: 'event-click', scheduleId: number): void
	(e: 'building-drilldown', buildingId: number): void
}>()

const dayOrder: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY']
const dayLabels: Record<DayOfWeek, string> = {
	MONDAY: 'Mon',
	TUESDAY: 'Tue',
	WEDNESDAY: 'Wed',
	THURSDAY: 'Thu',
	FRIDAY: 'Fri',
	SATURDAY: 'Sat',
	SUNDAY: 'Sun',
}

type GridColumn = {
	id: string
	label: string
	buildingId: number | null
	roomId: number | null
}

type TimeRow = {
	id: string
	start: string
	end: string
}

type GridCell = {
	key: string
	column: GridColumn
	schedules: Schedule[]
	count: number
	uniqueRoomCount: number
	previewText: string
	overflowCount: number
}

type MatrixRow = {
	row: TimeRow
	cells: GridCell[]
}

const selectedDay = ref<DayOfWeek>('MONDAY')

const isAggregateMode = computed(() => !props.selectedBuildingId && !props.selectedRoomId)

const daySchedules = computed(() =>
	props.schedules.filter(schedule => schedule.timeSlot.dayOfWeek === selectedDay.value)
)

watch(
	() => props.schedules,
	() => {
		const hasSchedulesForSelectedDay = props.schedules.some(
			schedule => schedule.timeSlot.dayOfWeek === selectedDay.value
		)
		if (hasSchedulesForSelectedDay) {
			return
		}
		const firstDayWithData = dayOrder.find(day =>
			props.schedules.some(schedule => schedule.timeSlot.dayOfWeek === day)
		)
		if (firstDayWithData) {
			selectedDay.value = firstDayWithData
		}
	},
	{ immediate: true }
)

function toMinutes(time: string): number {
	const [rawHour = '0', rawMinute = '0'] = time.split(':')
	const hour = Number(rawHour)
	const minute = Number(rawMinute)
	if (Number.isNaN(hour) || Number.isNaN(minute)) {
		return 0
	}
	return hour * 60 + minute
}

function formatTime(time: string): string {
	const [rawHour = '0', rawMinute = '0'] = time.split(':')
	const hour24 = Number(rawHour)
	const minute = Number(rawMinute)
	const normalizedHour = Number.isNaN(hour24) ? 0 : hour24
	const normalizedMinute = Number.isNaN(minute) ? 0 : minute
	const period = normalizedHour >= 12 ? 'PM' : 'AM'
	const hour12 = normalizedHour % 12 || 12
	return `${hour12}:${String(normalizedMinute).padStart(2, '0')} ${period}`
}

function formatTimeRange(start: string, end: string): string {
	return `${formatTime(start)} - ${formatTime(end)}`
}

function pluralize(label: string, count: number): string {
	if (count === 1) {
		return label
	}
	return label.endsWith('s') ? `${label}es` : `${label}s`
}

function getPreview(schedules: Schedule[], limit = 2): { text: string; overflow: number } {
	if (schedules.length === 0) {
		return { text: '', overflow: 0 }
	}
	const text = schedules
		.slice(0, limit)
		.map(schedule => schedule.course.code)
		.join(' • ')
	return { text, overflow: Math.max(0, schedules.length - limit) }
}

function getInstructorLabel(schedule: Schedule | undefined): string {
	const instructor = schedule?.course.instructor
	if (!instructor) {
		return 'Unassigned'
	}
	return `${instructor.firstName} ${instructor.lastName}`
}

const columns = computed<GridColumn[]>(() => {
	if (props.selectedRoomId) {
		const room = props.rooms.find(candidate => candidate.id === props.selectedRoomId)
		if (!room) return []
		return [{
			id: `room-${room.id}`,
			label: `${room.buildingCode} ${room.roomNumber}`,
			buildingId: room.buildingId,
			roomId: room.id,
		}]
	}

	if (props.selectedBuildingId) {
		return props.rooms
			.filter(room => room.buildingId === props.selectedBuildingId)
			.sort((a, b) => a.roomNumber.localeCompare(b.roomNumber))
			.map(room => ({
				id: `room-${room.id}`,
				label: room.roomNumber,
				buildingId: room.buildingId,
				roomId: room.id,
			}))
	}

	const activeBuildingIds = new Set(
		daySchedules.value
			.map(schedule => schedule.room.buildingId)
			.filter((id): id is number => !!id)
	)

	return props.buildings
		.filter(building => activeBuildingIds.has(building.id))
		.sort((a, b) => a.code.localeCompare(b.code))
		.map(building => ({
			id: `building-${building.id}`,
			label: building.code,
			buildingId: building.id,
			roomId: null,
		}))
})

const timeRows = computed<TimeRow[]>(() => {
	const rowsById = new Map<string, TimeRow>()
	for (const schedule of daySchedules.value) {
		const rowId = `${schedule.timeSlot.startTime}-${schedule.timeSlot.endTime}`
		if (!rowsById.has(rowId)) {
			rowsById.set(rowId, {
				id: rowId,
				start: schedule.timeSlot.startTime,
				end: schedule.timeSlot.endTime,
			})
		}
	}
	return Array.from(rowsById.values()).sort((a, b) => toMinutes(a.start) - toMinutes(b.start))
})

const cellMap = computed(() => {
	const map = new Map<string, Schedule[]>()
	for (const schedule of daySchedules.value) {
		const rowId = `${schedule.timeSlot.startTime}-${schedule.timeSlot.endTime}`
		const colId = props.selectedBuildingId || props.selectedRoomId
			? `room-${schedule.room.id}`
			: `building-${schedule.room.buildingId}`
		const key = `${rowId}|${colId}`
		const current = map.get(key) ?? []
		current.push(schedule)
		map.set(key, current)
	}
	return map
})

function getCellSchedules(rowId: string, colId: string): Schedule[] {
	return cellMap.value.get(`${rowId}|${colId}`) ?? []
}

function getUniqueRoomCount(schedules: Schedule[]): number {
	return new Set(schedules.map(schedule => schedule.room.id)).size
}

function getDensityClass(count: number): string {
	if (count <= 0) return 'density-empty'
	if (count === 1) return 'density-low'
	if (count <= 3) return 'density-medium'
	return 'density-high'
}

const matrixRows = computed<MatrixRow[]>(() =>
	timeRows.value.map(row => ({
		row,
		cells: columns.value.map(column => {
			const schedules = getCellSchedules(row.id, column.id)
			const preview = getPreview(schedules)
			return {
				key: `${row.id}-${column.id}`,
				column,
				schedules,
				count: schedules.length,
				uniqueRoomCount: getUniqueRoomCount(schedules),
				previewText: preview.text,
				overflowCount: preview.overflow,
			}
		}),
	}))
)

const scheduleCount = computed(() => daySchedules.value.length)

const peakRowSummary = computed(() => {
	if (timeRows.value.length === 0) {
		return null
	}
	let bestRow: TimeRow | null = null
	let bestCount = 0
	for (const row of timeRows.value) {
		let count = 0
		for (const column of columns.value) {
			count += getCellSchedules(row.id, column.id).length
		}
		if (count > bestCount) {
			bestCount = count
			bestRow = row
		}
	}
	if (!bestRow) return null
	return {
		label: formatTimeRange(bestRow.start, bestRow.end),
		count: bestCount,
	}
})

const selectedBuildingName = computed(() => {
	if (!props.selectedBuildingId) return null
	const building = props.buildings.find(candidate => candidate.id === props.selectedBuildingId)
	return building ? `${building.code} - ${building.name}` : null
})

const interactionHint = computed(() =>
	isAggregateMode.value
		? 'Select any box with classes to view rooms in that building.'
		: 'Select a class box to view schedule details.'
)

function handleCellClick(cell: GridCell): void {
	if (cell.count === 0) {
		return
	}

	if (isAggregateMode.value && cell.column.buildingId) {
		emit('building-drilldown', cell.column.buildingId)
		return
	}

	const firstSchedule = cell.schedules[0]
	if (firstSchedule) {
		emit('event-click', firstSchedule.id)
	}
}
</script>

<template>
	<div class="space-y-3">
		<div class="flex flex-wrap items-start gap-y-2 justify-between">
			<div class="flex flex-wrap items-center gap-2">
				<button v-for="day in dayOrder" :key="day" @click="selectedDay = day" :class="[
					'px-3 py-1.5 text-sm border rounded',
					selectedDay === day
						? 'bg-blue-600 text-white border-blue-600'
						: 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50',
				]">
					{{ dayLabels[day] }}
				</button>
			</div>
			<div class="text-sm text-gray-600">
				<span class="font-medium text-gray-900">{{ scheduleCount }}</span> classes
				<span v-if="peakRowSummary" class="ml-3">Busiest: <span class="font-medium text-gray-900">{{
					peakRowSummary.label }}</span> ({{ peakRowSummary.count }})</span>
				<span v-if="selectedBuildingName" class="ml-3">Viewing: <span class="font-medium text-gray-900">{{
					selectedBuildingName }}</span></span>
			</div>
		</div>

		<div class="text-xs text-gray-500">
			{{ interactionHint }}
		</div>

		<div v-if="columns.length === 0 || timeRows.length === 0"
			class="text-sm text-gray-500 border border-gray-200 rounded p-4 bg-white">
			No classes for {{ dayLabels[selectedDay] }} with the current filters.
		</div>

		<div v-else class="border border-gray-200 rounded bg-white overflow-auto max-h-[76vh]">
			<table class="border-collapse min-w-max w-full">
				<thead class="sticky top-0 z-10 bg-gray-50">
					<tr>
						<th
							class="sticky left-0 z-20 bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-600 border-b border-r border-gray-200 px-3 py-2 min-w-44">
							Time
						</th>
						<th v-for="column in columns" :key="column.id"
							class="text-left text-xs uppercase tracking-wide text-gray-600 border-b border-r border-gray-200 px-3 py-2 min-w-44">
							{{ column.label }}
						</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="matrixRow in matrixRows" :key="matrixRow.row.id">
						<th
							class="sticky left-0 z-10 bg-white text-left text-xs text-gray-700 border-b border-r border-gray-200 px-3 py-2 align-top whitespace-nowrap">
							{{ formatTimeRange(matrixRow.row.start, matrixRow.row.end) }}
						</th>
						<td v-for="cell in matrixRow.cells" :key="cell.key"
							class="border-b border-r border-gray-200 p-1 align-top">
							<button v-if="cell.count > 0"
								class="density-button w-full h-full min-h-20 rounded p-2 text-left transition-colors cursor-pointer"
								:class="getDensityClass(cell.count)" @click="handleCellClick(cell)">
								<template v-if="isAggregateMode">
									<div class="flex items-center justify-between gap-2">
										<span class="text-xs font-semibold cell-primary">{{ cell.count }} {{
											pluralize('class', cell.count) }}</span>
										<span class="text-[11px] cell-secondary">{{ cell.uniqueRoomCount }} {{
											pluralize('room', cell.uniqueRoomCount) }}</span>
									</div>
									<div v-if="cell.previewText" class="mt-1 text-[11px] cell-secondary truncate">
										{{ cell.previewText }}
									</div>
									<div v-if="cell.overflowCount > 0" class="mt-1 text-[11px] cell-tertiary">
										+{{ cell.overflowCount }} more
									</div>
								</template>
								<template v-else>
									<template v-if="cell.count === 1">
										<div class="text-xs font-semibold cell-primary truncate">
											{{ cell.schedules[0]?.course.code }}
										</div>
										<div class="mt-1 text-[11px] cell-secondary truncate">
											{{ getInstructorLabel(cell.schedules[0]) }}
										</div>
									</template>
									<template v-else>
										<div class="flex items-center justify-between gap-2">
											<span class="text-xs font-semibold cell-primary">{{ cell.count }} {{
												pluralize('class', cell.count) }}</span>
											<span
												class="text-[10px] uppercase tracking-wide cell-overlap">Overlap</span>
										</div>
										<div v-if="cell.previewText" class="mt-1 text-[11px] cell-secondary truncate">
											{{ cell.previewText }}
										</div>
										<div v-if="cell.overflowCount > 0" class="mt-1 text-[11px] cell-tertiary">
											+{{ cell.overflowCount }} more
										</div>
									</template>
								</template>
							</button>
							<div v-else class="min-h-20 rounded density-empty-swatch" />
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
</template>

<style scoped>
.density-button {
	border: 1px solid transparent;
	--cell-primary: var(--app-gray-900);
	--cell-secondary: var(--app-gray-700);
	--cell-tertiary: var(--app-gray-600);
	--cell-overlap: var(--app-red-700);
}

.density-button:hover {
	filter: brightness(var(--density-hover-brightness));
}

.cell-primary {
	color: var(--cell-primary);
}

.cell-secondary {
	color: var(--cell-secondary);
}

.cell-tertiary {
	color: var(--cell-tertiary);
}

.cell-overlap {
	color: var(--cell-overlap);
	font-weight: 700;
}

.density-empty {
	background: var(--app-gray-50);
	border-color: var(--app-gray-200);
}

.density-empty-swatch {
	background: var(--density-empty-bg);
}

.density-low {
	background: var(--density-low-bg);
	border-color: var(--density-low-border);
	--cell-primary: var(--density-low-text);
	--cell-secondary: var(--density-low-subtext);
	--cell-tertiary: var(--density-low-muted);
}

.density-medium {
	background: var(--density-medium-bg);
	border-color: var(--density-medium-border);
	--cell-primary: var(--density-medium-text);
	--cell-secondary: var(--density-medium-subtext);
	--cell-tertiary: var(--density-medium-muted);
	--cell-overlap: var(--density-medium-overlap);
}

.density-high {
	background: var(--density-high-bg);
	border-color: var(--density-high-border);
	--cell-primary: var(--density-high-text);
	--cell-secondary: var(--density-high-subtext);
	--cell-tertiary: var(--density-high-muted);
}
</style>
