<script setup lang="ts">
import { computed, watch } from 'vue'
import { ScheduleXCalendar } from '@schedule-x/vue'
import { createCalendar, createViewWeek } from '@schedule-x/calendar'
import { createEventsServicePlugin } from '@schedule-x/events-service'
import '@schedule-x/theme-default/dist/index.css'
import 'temporal-polyfill/global'
import type { Schedule } from '@/services/schedules'
import type { DayOfWeek } from '@/services/timeslots'

const props = defineProps<{
	schedules: Schedule[]
}>()

const DAY_INDEX: Record<DayOfWeek, number> = {
	MONDAY: 0,
	TUESDAY: 1,
	WEDNESDAY: 2,
	THURSDAY: 3,
	FRIDAY: 4,
	SATURDAY: 5,
	SUNDAY: 6,
}

// Color palette for buildings (colorName as used by Schedule X)
const BUILDING_COLORS = [
	'#3b82f6', // blue
	'#10b981', // emerald
	'#f59e0b', // amber
	'#ef4444', // red
	'#8b5cf6', // violet
	'#ec4899', // pink
	'#06b6d4', // cyan
	'#84cc16', // lime
]

// Get a reference Monday (current week)
function getReferenceMonday(): Temporal.PlainDate {
	const today = Temporal.Now.plainDateISO()
	const daysSinceMonday = (today.dayOfWeek - 1)
	return today.subtract({ days: daysSinceMonday })
}

const referenceMonday = getReferenceMonday()
const timezone = Temporal.Now.timeZoneId()

// Build a map of building codes to color indices
const buildingColorMap = computed(() => {
	const map = new Map<string, number>()
	let colorIndex = 0
	for (const schedule of props.schedules) {
		const buildingCode = schedule.room.buildingCode || 'Unknown'
		if (!map.has(buildingCode)) {
			map.set(buildingCode, colorIndex % BUILDING_COLORS.length)
			colorIndex++
		}
	}
	return map
})

// Generate calendars config for Schedule X
const calendarsConfig = computed(() => {
	const config: Record<string, { colorName: string; lightColors: { main: string; container: string; onContainer: string } }> = {}
	for (const [buildingCode, colorIndex] of buildingColorMap.value.entries()) {
		const color = BUILDING_COLORS[colorIndex] ?? '#6b7280'
		config[buildingCode] = {
			colorName: buildingCode,
			lightColors: {
				main: color,
				container: color + '80', // 50% opacity for better visibility
				onContainer: '#ffffff',
			},
		}
	}
	return config
})

// Convert schedule to ScheduleX event format with proper Temporal types
function scheduleToEvent(schedule: Schedule) {
	const dayOffset = DAY_INDEX[schedule.timeSlot.dayOfWeek]
	const eventDate = referenceMonday.add({ days: dayOffset })

	// Create ZonedDateTime for start and end
	const [startHour, startMin] = schedule.timeSlot.startTime.split(':').map(Number)
	const [endHour, endMin] = schedule.timeSlot.endTime.split(':').map(Number)

	const start = eventDate.toZonedDateTime({
		timeZone: timezone,
		plainTime: Temporal.PlainTime.from({ hour: startHour, minute: startMin })
	})

	const end = eventDate.toZonedDateTime({
		timeZone: timezone,
		plainTime: Temporal.PlainTime.from({ hour: endHour, minute: endMin })
	})

	return {
		id: schedule.id,
		title: schedule.course.code,
		start,
		end,
		description: `${schedule.course.name}\n${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
		location: `${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
		calendarId: schedule.room.buildingCode || 'Unknown',
	}
}

const events = computed(() => props.schedules.map(scheduleToEvent))

// Create events service plugin
const eventsService = createEventsServicePlugin()

// Create calendar with weekly view configuration
const calendarApp = createCalendar({
	selectedDate: referenceMonday,
	views: [createViewWeek()],
	defaultView: 'week',
	dayBoundaries: {
		start: '07:00',
		end: '22:00',
	},
	weekOptions: {
		nDays: 5,
		gridHeight: 700,
		eventWidth: 95,
	},
	firstDayOfWeek: 1, // Monday
	isResponsive: true,
	events: events.value,
	calendars: calendarsConfig.value,
}, [eventsService])

// Update events when schedules change
watch(events, (newEvents) => {
	eventsService.set(newEvents)
}, { deep: true })
</script>

<template>
	<div class="schedule-calendar-wrapper">
		<ScheduleXCalendar :calendar-app="calendarApp" />
	</div>
</template>

<style scoped>
.schedule-calendar-wrapper {
	width: 100%;
	height: 700px;
	max-height: 80vh;
}

.schedule-calendar-wrapper :deep(.sx__calendar-wrapper) {
	height: 100%;
}
</style>
