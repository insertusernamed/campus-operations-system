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

// Get a reference Monday (current week)
function getReferenceMonday(): Temporal.PlainDate {
	const today = Temporal.Now.plainDateISO()
	const daysSinceMonday = (today.dayOfWeek - 1)
	return today.subtract({ days: daysSinceMonday })
}

const referenceMonday = getReferenceMonday()
const timezone = Temporal.Now.timeZoneId()

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
