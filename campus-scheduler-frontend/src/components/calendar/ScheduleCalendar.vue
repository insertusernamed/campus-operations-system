<script setup lang="ts">
import { computed, watch, shallowRef, ref, onMounted, onUnmounted } from 'vue'
import { ScheduleXCalendar } from '@schedule-x/vue'
import { createCalendar, createViewDay, createViewWeek } from '@schedule-x/calendar'
import { createEventsServicePlugin } from '@schedule-x/events-service'
import '@schedule-x/theme-default/dist/index.css'
import 'temporal-polyfill/global'
import type { Schedule } from '@/services/schedules'
import type { DayOfWeek } from '@/services/timeslots'
import {
	DEFAULT_SEMESTER_DEFINITIONS,
	semestersService,
	type SemesterDefinition,
} from '@/services/semesters'
import { parseSemesterLabel, resolveSemesterDateRange } from '@/utils/semester'
import { useTheme } from '@/composables/useTheme'

const props = defineProps<{
	schedules: Schedule[]
	height?: number
	viewMode?: 'week' | 'day'
	weekDays?: number
	dayStart?: string
	dayEnd?: string
	gridStep?: 180 | 120 | 60 | 30 | 15
	eventWidth?: number
	ghostSchedules?: Schedule[]
	movedScheduleIds?: number[]
	arrowScheduleId?: number | null
}>()

const calendarHeight = computed(() => props.height ?? 800)
const calendarViewMode = computed(() => props.viewMode ?? 'week')
const weekDaysToShow = computed(() => {
	const raw = props.weekDays ?? 5
	return Math.max(1, Math.min(7, raw))
})
const dayStart = computed(() => props.dayStart ?? '07:00')
const dayEnd = computed(() => props.dayEnd ?? '23:00')
const gridStep = computed(() => props.gridStep ?? 60)
const eventWidth = computed(() => {
	const raw = props.eventWidth ?? 95
	return Math.max(1, Math.min(100, raw))
})
const { theme } = useTheme()

const emit = defineEmits<{
	(e: 'event-click', scheduleId: number): void
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

const semesterDefinitions = ref<SemesterDefinition[]>(DEFAULT_SEMESTER_DEFINITIONS)

const EVENT_COLORS_LIGHT = [
	'#3b82f6', // blue
	'#10b981', // emerald
	'#f59e0b', // amber
	'#ef4444', // red
	'#8b5cf6', // violet
	'#ec4899', // pink
	'#06b6d4', // cyan
	'#84cc16', // lime
]

const EVENT_COLORS_SLATE = [
	'#5e81ac', // frost blue
	'#6f9a7f', // muted green
	'#b88f53', // muted amber
	'#a65b66', // muted red
	'#8a78ad', // muted violet
	'#a57696', // muted pink
	'#5e99a8', // muted cyan
	'#8ba35c', // muted lime
]

const eventColors = computed(() =>
	theme.value === 'slate' ? EVENT_COLORS_SLATE : EVENT_COLORS_LIGHT
)

function normalizeHexColor(input: string): string | null {
	const trimmed = input.trim()
	const match = trimmed.match(/^#([0-9a-fA-F]{6})$/)
	return match ? `#${match[1]}` : null
}

function getReadableTextColor(backgroundColor: string): string {
	const hex = normalizeHexColor(backgroundColor)
	if (!hex) {
		return '#f8fafc'
	}

	const red = Number.parseInt(hex.slice(1, 3), 16) / 255
	const green = Number.parseInt(hex.slice(3, 5), 16) / 255
	const blue = Number.parseInt(hex.slice(5, 7), 16) / 255
	const toLinear = (channel: number) =>
		channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4

	const luminance =
		0.2126 * toLinear(red) + 0.7152 * toLinear(green) + 0.0722 * toLinear(blue)

	return luminance > 0.45 ? '#1f2937' : '#f8fafc'
}

function getReferenceMonday(inputDate?: Temporal.PlainDate): Temporal.PlainDate {
	const today = inputDate ?? Temporal.Now.plainDateISO()
	const daysSinceMonday = today.dayOfWeek - 1
	return today.subtract({ days: daysSinceMonday })
}

function parseSemesterWindow(semester: string): { start: Temporal.PlainDate; end: Temporal.PlainDate } | null {
	const parsed = parseSemesterLabel(semester, semesterDefinitions.value)
	if (!parsed) {
		return null
	}

	const window = resolveSemesterDateRange(parsed.definition, parsed.year)
	const start = Temporal.PlainDate.from(window.start)
	const end = Temporal.PlainDate.from(window.end)

	return { start, end }
}

function parseTimeParts(time: string): { hour: number; minute: number } {
	const [rawHour = '0', rawMinute = '0'] = time.split(':')
	const hour = Number(rawHour)
	const minute = Number(rawMinute)

	return {
		hour: Number.isNaN(hour) ? 0 : hour,
		minute: Number.isNaN(minute) ? 0 : minute,
	}
}

function getFirstClassDateForDay(start: Temporal.PlainDate, dayOfWeek: DayOfWeek): Temporal.PlainDate {
	const targetIsoDay = DAY_INDEX[dayOfWeek] + 1
	const delta = (targetIsoDay - start.dayOfWeek + 7) % 7
	return start.add({ days: delta })
}

function getClassDatesForSemester(schedule: Schedule): Temporal.PlainDate[] {
	const parsedWindow = parseSemesterWindow(schedule.semester)
	if (!parsedWindow) {
		const fallbackMonday = getReferenceMonday()
		return [fallbackMonday.add({ days: DAY_INDEX[schedule.timeSlot.dayOfWeek] })]
	}

	const dates: Temporal.PlainDate[] = []
	let nextDate = getFirstClassDateForDay(parsedWindow.start, schedule.timeSlot.dayOfWeek)
	while (Temporal.PlainDate.compare(nextDate, parsedWindow.end) <= 0) {
		dates.push(nextDate)
		nextDate = nextDate.add({ weeks: 1 })
	}
	return dates
}

const selectedDate = ref<Temporal.PlainDate>(getReferenceMonday())
watch(calendarViewMode, (mode, previousMode) => {
	if (!previousMode) {
		selectedDate.value = mode === 'day' ? Temporal.Now.plainDateISO() : getReferenceMonday()
	}
}, { immediate: true })

function parseScheduleIdFromEventId(eventId: string): number | null {
	const match = eventId.match(/^(\d+)(?:-|$)/)
	if (!match) {
		return null
	}
	const parsed = Number(match[1])
	return Number.isNaN(parsed) ? null : parsed
}

function getMovedSelector(scheduleId: number): string {
	return `[data-event-id^="${scheduleId}-"]`
}

function getGhostSelector(scheduleId: number): string {
	return `[data-event-id^="ghost-${scheduleId}-"]`
}

const timezone = Temporal.Now.timeZoneId()

// Sanitize key for Schedule X
function sanitizeKey(key: string): string {
	return key.replace(/[^a-zA-Z0-9-]/g, '-')
}

// Build a map of departments to color indices
const departmentColorMap = computed(() => {
	const map = new Map<string, number>()
	let colorIndex = 0
	for (const schedule of props.schedules) {
		const rawDept = schedule.course.department || 'General'
		const deptKey = sanitizeKey(rawDept)

		if (!map.has(deptKey)) {
			map.set(deptKey, colorIndex % eventColors.value.length)
			colorIndex++
		}
	}
	return map
})

// Generate calendars config for Schedule X
const calendarsConfig = computed(() => {
	const config: Record<string, { colorName: string; lightColors: { main: string; container: string; onContainer: string } }> = {}
	for (const [deptKey, colorIndex] of departmentColorMap.value.entries()) {
		const color = eventColors.value[colorIndex] ?? '#6b7280'
		config[deptKey] = {
			colorName: deptKey, // This label is for internal use mostly
			lightColors: {
				main: color,
				container: color,
				onContainer: getReadableTextColor(color),
			},
		}
	}
	return config
})

const calendarsSignature = computed(() =>
	Object.entries(calendarsConfig.value)
		.map(([deptKey, config]) => `${deptKey}:${config.lightColors.main}`)
		.sort()
		.join('|')
)

// Convert schedule to ScheduleX event format with proper Temporal types
function scheduleToEvents(
	schedule: Schedule,
	options?: { eventId?: string; additionalClasses?: string[] }
) {
	const { hour: startHour, minute: startMin } = parseTimeParts(schedule.timeSlot.startTime)
	const { hour: endHour, minute: endMin } = parseTimeParts(schedule.timeSlot.endTime)

	const deptKey = sanitizeKey(schedule.course.department || 'General')
	const occurrenceDates = getClassDatesForSemester(schedule)

	return occurrenceDates.map(date => {
		const start = date.toZonedDateTime({
			timeZone: timezone,
			plainTime: Temporal.PlainTime.from({ hour: startHour, minute: startMin })
		})

		const end = date.toZonedDateTime({
			timeZone: timezone,
			plainTime: Temporal.PlainTime.from({ hour: endHour, minute: endMin })
		})

		return {
			id: options?.eventId
				? `${options.eventId}-${date.toString()}`
				: `${schedule.id}-${date.toString()}`,
			title: schedule.course.code,
			start,
			end,
			description: `${schedule.course.name}\n${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
			location: `${schedule.room.buildingCode} ${schedule.room.roomNumber}`,
			calendarId: deptKey,
			_options: options?.additionalClasses ? { additionalClasses: options.additionalClasses } : undefined,
		}
	})
}

const movedIds = computed(() => new Set(props.movedScheduleIds ?? []))
const baseEvents = computed(() =>
	props.schedules.flatMap(schedule =>
		scheduleToEvents(schedule, movedIds.value.has(schedule.id) ? { additionalClasses: ['cs-moved'] } : undefined)
	)
)
const ghostEvents = computed(() =>
	(props.ghostSchedules ?? []).flatMap(schedule =>
		scheduleToEvents(schedule, { eventId: `ghost-${schedule.id}`, additionalClasses: ['cs-ghost'] })
	)
)
const events = computed(() => [...baseEvents.value, ...ghostEvents.value])

// Reactive calendar app instance
const calendarApp = shallowRef<any>(null)
const calendarKey = shallowRef(0)
let eventsService: any = null
const containerRef = ref<HTMLDivElement | null>(null)
const arrow = ref<{ x1: number; y1: number; x2: number; y2: number; width: number; height: number } | null>(null)
let arrowFrame: number | null = null

function initCalendar() {
	eventsService = createEventsServicePlugin()

	calendarApp.value = createCalendar({
		selectedDate: selectedDate.value,
		views: [createViewDay(), createViewWeek()],
		defaultView: calendarViewMode.value,
		dayBoundaries: {
			start: dayStart.value,
			end: dayEnd.value,
		},
		weekOptions: {
			nDays: calendarViewMode.value === 'day' ? 1 : weekDaysToShow.value,
			gridHeight: calendarHeight.value,
			eventWidth: eventWidth.value,
			gridStep: gridStep.value,
		},
		firstDayOfWeek: 1,
		isResponsive: true,
		events: events.value,
		calendars: calendarsConfig.value,
		callbacks: {
			onSelectedDateUpdate: (date) => {
				selectedDate.value = date
			},
			onEventClick: (event) => {
				const eventId = String(event.id)
				if (eventId.startsWith('ghost-')) {
					return
				}
				const parsedId = parseScheduleIdFromEventId(eventId)
				if (parsedId !== null) {
					emit('event-click', parsedId)
				}
			},
		},
	}, [eventsService])

	calendarKey.value++
}

function updateArrow() {
	const scheduleId = props.arrowScheduleId
	if (!scheduleId || !containerRef.value) {
		arrow.value = null
		return
	}

	const container = containerRef.value
	const ghostEl = container.querySelector<HTMLElement>(getGhostSelector(scheduleId))
	const movedEl = container.querySelector<HTMLElement>(getMovedSelector(scheduleId))

	if (!ghostEl || !movedEl) {
		arrow.value = null
		return
	}

	const containerRect = container.getBoundingClientRect()
	const ghostRect = ghostEl.getBoundingClientRect()
	const movedRect = movedEl.getBoundingClientRect()
	const scrollLeft = container.scrollLeft
	const scrollTop = container.scrollTop

	// Centers of each box relative to the container
	const cx1 = ghostRect.left - containerRect.left + scrollLeft + ghostRect.width / 2
	const cy1 = ghostRect.top - containerRect.top + scrollTop + ghostRect.height / 2
	const cx2 = movedRect.left - containerRect.left + scrollLeft + movedRect.width / 2
	const cy2 = movedRect.top - containerRect.top + scrollTop + movedRect.height / 2

	const dx = cx2 - cx1
	const dy = cy2 - cy1
	const length = Math.hypot(dx, dy)

	if (length === 0) {
		arrow.value = null
		return
	}

	// Find where the line from center exits the rectangle border
	function borderIntersect(hw: number, hh: number, dirX: number, dirY: number): { x: number; y: number } {
		// hw = half-width, hh = half-height of the box
		// dirX, dirY = direction vector (from center outward)
		if (dirX === 0 && dirY === 0) return { x: 0, y: 0 }
		// Scale factor to reach each edge
		const sx = dirX !== 0 ? Math.abs(hw / dirX) : Infinity
		const sy = dirY !== 0 ? Math.abs(hh / dirY) : Infinity
		const s = Math.min(sx, sy)
		return { x: dirX * s, y: dirY * s }
	}

	const ghostOffset = borderIntersect(ghostRect.width / 2, ghostRect.height / 2, dx, dy)
	const movedOffset = borderIntersect(movedRect.width / 2, movedRect.height / 2, -dx, -dy)

	const ux = dx / length
	const uy = dy / length

	const x1 = cx1 + ghostOffset.x + ux
	const y1 = cy1 + ghostOffset.y + uy
	const x2 = cx2 + movedOffset.x - ux
	const y2 = cy2 + movedOffset.y - uy

	// Check the arrow still points in the right direction (boxes aren't overlapping)
	const arrowDx = x2 - x1
	const arrowDy = y2 - y1
	if (arrowDx * dx + arrowDy * dy <= 0) {
		arrow.value = null
		return
	}

	arrow.value = {
		x1,
		y1,
		x2,
		y2,
		width: container.scrollWidth,
		height: container.scrollHeight,
	}
}

function scheduleArrowUpdate() {
	if (arrowFrame !== null) {
		return
	}
	arrowFrame = requestAnimationFrame(() => {
		arrowFrame = null
		updateArrow()
	})
}

async function loadSemesterDefinitions() {
	semesterDefinitions.value = await semestersService.getDefinitions()
}

// Initialize on mount
initCalendar()
onMounted(() => {
	void loadSemesterDefinitions()
	scheduleArrowUpdate()
	window.addEventListener('resize', scheduleArrowUpdate)
	containerRef.value?.addEventListener('scroll', scheduleArrowUpdate)
})

onUnmounted(() => {
	window.removeEventListener('resize', scheduleArrowUpdate)
	containerRef.value?.removeEventListener('scroll', scheduleArrowUpdate)
	if (arrowFrame !== null) {
		cancelAnimationFrame(arrowFrame)
		arrowFrame = null
	}
})

// Recreate calendar when building colors/config changes
watch([
	calendarsSignature,
	calendarHeight,
	calendarViewMode,
	weekDaysToShow,
	dayStart,
	dayEnd,
	gridStep,
	eventWidth,
], () => {
	initCalendar()
})

// Update events when schedules change (using current service instance)
watch(events, (newEvents) => {
	// Only update events if we didn't just recreate the calendar
	// (initCalendar uses the latest events already)
	if (eventsService) {
		eventsService.set(newEvents)
	}
	scheduleArrowUpdate()
}, { deep: true })

watch(() => props.arrowScheduleId, () => {
	scheduleArrowUpdate()
})
</script>

<template>
	<div ref="containerRef" class="schedule-calendar-wrapper" :class="{ 'is-dark': theme === 'slate' }"
		:style="{ height: `${calendarHeight}px`, minHeight: `${calendarHeight}px` }">
		<svg v-if="arrow" class="cs-move-arrow" :width="arrow.width" :height="arrow.height"
			:viewBox="`0 0 ${arrow.width} ${arrow.height}`">
			<defs>
				<marker id="cs-arrow-head" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
					<path d="M0,0 L0,6 L6,3 z" fill="currentColor" />
				</marker>
			</defs>
			<path :d="`M ${arrow.x1} ${arrow.y1} L ${arrow.x2} ${arrow.y2}`" stroke="currentColor" stroke-width="2"
				fill="none" marker-end="url(#cs-arrow-head)" />
		</svg>
		<ScheduleXCalendar v-if="calendarApp" :key="calendarKey" :calendar-app="calendarApp" />
	</div>
</template>

<style scoped>
.schedule-calendar-wrapper {
	width: 100%;
	height: 800px;
	max-height: 85vh;
	position: relative;
	overflow: auto;
	--sx-color-primary: var(--app-blue-600);
	--sx-color-on-primary: #ffffff;
	--sx-color-primary-container: var(--app-blue-50);
	--sx-color-on-primary-container: var(--app-gray-900);
	--sx-color-surface: var(--app-surface);
	--sx-color-surface-dim: var(--app-gray-100);
	--sx-color-surface-bright: var(--app-surface);
	--sx-color-on-surface: var(--app-gray-900);
	--sx-color-surface-container: var(--app-gray-100);
	--sx-color-surface-container-low: var(--app-gray-50);
	--sx-color-surface-container-high: var(--app-gray-200);
	--sx-color-background: var(--app-surface);
	--sx-color-on-background: var(--app-gray-900);
	--sx-color-outline: var(--app-gray-500);
	--sx-color-outline-variant: var(--app-gray-300);
	--sx-color-neutral: var(--app-gray-600);
	--sx-color-neutral-variant: var(--app-gray-500);
	--sx-internal-color-gray-ripple-background: var(--app-gray-200);
	--sx-internal-color-light-gray: var(--app-gray-50);
	--sx-internal-color-text: var(--app-gray-900);
	--sx-border: 1px solid var(--app-gray-300);
}

.schedule-calendar-wrapper.is-dark {
	--sx-color-primary: var(--app-blue-500);
	--sx-color-on-primary: #1f2937;
	--sx-color-primary-container: var(--app-blue-700);
	--sx-color-on-primary-container: #eff5fd;
	--sx-color-surface: var(--app-gray-100);
	--sx-color-surface-dim: var(--app-gray-200);
	--sx-color-surface-bright: var(--app-gray-100);
	--sx-color-on-surface: var(--app-gray-900);
	--sx-color-surface-container: var(--app-gray-200);
	--sx-color-surface-container-low: var(--app-gray-100);
	--sx-color-surface-container-high: var(--app-gray-300);
	--sx-color-background: var(--app-gray-100);
	--sx-color-on-background: var(--app-gray-900);
	--sx-color-outline: var(--app-gray-500);
	--sx-color-outline-variant: var(--app-gray-300);
	--sx-color-neutral: var(--app-gray-700);
	--sx-color-neutral-variant: var(--app-gray-600);
	--sx-internal-color-gray-ripple-background: var(--app-gray-300);
	--sx-internal-color-light-gray: var(--app-gray-200);
	--sx-internal-color-text: var(--app-gray-900);
	--sx-border: 1px solid var(--app-gray-300);
}

.schedule-calendar-wrapper :deep(.sx__calendar-wrapper) {
	height: 100%;
}

.schedule-calendar-wrapper :deep(.cs-ghost) {
	opacity: 0.35;
	border: 1px dashed var(--color-border);
	pointer-events: none;
	position: relative;
}

.schedule-calendar-wrapper :deep(.cs-moved) {
	position: relative;
}

.schedule-calendar-wrapper :deep(.cs-ghost::after),
.schedule-calendar-wrapper :deep(.cs-moved::after) {
	position: absolute;
	top: 2px;
	right: 4px;
	font-size: 9px;
	font-weight: 600;
	text-transform: uppercase;
	letter-spacing: 0.02em;
	padding: 1px 4px;
	border-radius: 999px;
	background: var(--schedule-badge-bg);
	color: var(--schedule-badge-text);
}

.schedule-calendar-wrapper :deep(.cs-ghost::after) {
	content: 'Original';
}

.schedule-calendar-wrapper :deep(.cs-moved::after) {
	content: 'Moved';
}

.cs-move-arrow {
	position: absolute;
	top: 0;
	left: 0;
	pointer-events: none;
	z-index: 3;
	color: var(--schedule-arrow-color);
}
</style>
