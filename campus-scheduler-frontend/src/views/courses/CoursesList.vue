<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { AxiosError } from 'axios'
import { RouterLink } from 'vue-router'
import { toast } from 'vue3-toastify'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCrud } from '@/composables/useCrud'
import { coursesService, type Course } from '@/services/courses'
import { instructorsService, type Instructor } from '@/services/instructors'
import { roomsService, type Room } from '@/services/rooms'

type QueueFilter = 'all' | 'blocking' | 'needs-instructor' | 'capacity-risk' | 'ready'
type CourseStatus = 'ready' | 'needs-instructor' | 'capacity-risk' | 'needs-instructor-and-capacity-risk'

const queueFilterLabels: Record<QueueFilter, string> = {
	all: 'All Courses',
	blocking: 'Blocking Issues',
	'needs-instructor': 'Needs Instructor',
	'capacity-risk': 'Capacity Risk',
	ready: 'Ready',
}

const queueFilter = ref<QueueFilter>('all')
const departmentFilter = ref('all')
const rooms = ref<Room[]>([])
const instructors = ref<Instructor[]>([])
const capacityCheckAvailable = ref(false)
const auxiliaryError = ref<string | null>(null)
const assignmentSelections = ref<Record<number, string>>({})
const assignmentLoadingByCourse = ref<Record<number, boolean>>({})

const { items, loading, error, fetchAll, handleDelete } = useCrud<Course, never>({
	getAll: coursesService.getAll,
	deleteItem: coursesService.delete,
	listRoute: '/courses',
	deleteConfirm: 'Are you sure you want to delete this course?',
})

function getDepartment(course: Course): string {
	const department = course.department?.trim()
	return department && department.length > 0 ? department : 'Undeclared'
}

function getInstructorName(course: Course): string {
	if (!course.instructor) return 'Unassigned'
	return `${course.instructor.firstName} ${course.instructor.lastName}`
}

function formatEnrollmentCapacity(course: Course): string {
	return `${course.enrollmentCapacity} seats`
}

function hasInstructorGap(course: Course): boolean {
	return !course.instructor
}

function hasCapacityRisk(course: Course): boolean {
	if (!capacityCheckAvailable.value) return false
	return !rooms.value.some((room) => room.capacity >= course.enrollmentCapacity)
}

function isBlockingCourse(course: Course): boolean {
	return hasInstructorGap(course) || hasCapacityRisk(course)
}

function isReadyCourse(course: Course): boolean {
	return !isBlockingCourse(course)
}

function getCourseStatus(course: Course): CourseStatus {
	const instructorGap = hasInstructorGap(course)
	const capacityRisk = hasCapacityRisk(course)

	if (instructorGap && capacityRisk) return 'needs-instructor-and-capacity-risk'
	if (instructorGap) return 'needs-instructor'
	if (capacityRisk) return 'capacity-risk'
	return 'ready'
}

function getCourseStatusLabel(course: Course): string {
	switch (getCourseStatus(course)) {
		case 'needs-instructor-and-capacity-risk':
			return 'Instructor + Capacity Risk'
		case 'needs-instructor':
			return 'Needs Instructor'
		case 'capacity-risk':
			return 'Capacity Risk'
		default:
			return 'Ready'
	}
}

function getCourseStatusClass(course: Course): string {
	switch (getCourseStatus(course)) {
		case 'needs-instructor-and-capacity-risk':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'needs-instructor':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'capacity-risk':
			return 'border-amber-200 bg-amber-50 text-amber-800'
		default:
			return 'border-green-200 bg-green-50 text-green-900'
	}
}

const largestRoomCapacity = computed<number | null>(() => {
	if (!capacityCheckAvailable.value || rooms.value.length === 0) return null
	return Math.max(...rooms.value.map((room) => room.capacity))
})

function getCourseStatusHint(course: Course): string | null {
	if (!hasCapacityRisk(course)) return null
	if (largestRoomCapacity.value == null) return 'No rooms available'
	return `Largest room: ${largestRoomCapacity.value} seats`
}

function getSeverityRank(course: Course): number {
	const status = getCourseStatus(course)
	if (status === 'needs-instructor-and-capacity-risk') return 0
	if (status === 'needs-instructor') return 1
	if (status === 'capacity-risk') return 2
	return 3
}

const unassignedCoursesCount = computed(() => items.value.filter((course) => !course.instructor).length)
const capacityRiskCount = computed(() => {
	if (!capacityCheckAvailable.value) return 0
	return items.value.filter(hasCapacityRisk).length
})
const blockingCount = computed(() => items.value.filter(isBlockingCourse).length)
const readyCount = computed(() => items.value.filter(isReadyCourse).length)

const averageCredits = computed(() => {
	if (items.value.length === 0) return '0.0'
	const total = items.value.reduce((sum, course) => sum + course.credits, 0)
	return (total / items.value.length).toFixed(1)
})

const departmentOptions = computed(() => {
	const counts = new Map<string, number>()
	for (const course of items.value) {
		const department = getDepartment(course)
		counts.set(department, (counts.get(department) || 0) + 1)
	}

	return Array.from(counts.entries())
		.sort((a, b) => a[0].localeCompare(b[0]))
		.map(([department, count]) => ({
			value: department,
			label: `${department} (${count})`,
		}))
})

const prioritizedCourses = computed(() => {
	return [...items.value].sort((a, b) => {
		const severityDiff = getSeverityRank(a) - getSeverityRank(b)
		if (severityDiff !== 0) return severityDiff

		const enrollmentDiff = b.enrollmentCapacity - a.enrollmentCapacity
		if (enrollmentDiff !== 0) return enrollmentDiff

		return a.code.localeCompare(b.code)
	})
})

function matchesQueueFilter(course: Course): boolean {
	switch (queueFilter.value) {
		case 'blocking':
			return isBlockingCourse(course)
		case 'needs-instructor':
			return hasInstructorGap(course)
		case 'capacity-risk':
			return hasCapacityRisk(course)
		case 'ready':
			return isReadyCourse(course)
		default:
			return true
	}
}

const filteredCourses = computed(() => {
	return prioritizedCourses.value.filter((course) => {
		if (!matchesQueueFilter(course)) return false

		if (departmentFilter.value !== 'all' && getDepartment(course) !== departmentFilter.value) return false

		return true
	})
})

const activeFilterSummary = computed(() => {
	const summary: string[] = []

	if (queueFilter.value !== 'all') summary.push(`view: ${queueFilterLabels[queueFilter.value].toLowerCase()}`)
	if (departmentFilter.value !== 'all') summary.push(`department: ${departmentFilter.value}`)

	return summary.length > 0 ? summary.join(', ') : 'current filters'
})

function resetFilters() {
	queueFilter.value = 'all'
	departmentFilter.value = 'all'
}

function setQueueFilter(filter: QueueFilter) {
	queueFilter.value = filter
}

function isAssigning(courseId: number): boolean {
	return Boolean(assignmentLoadingByCourse.value[courseId])
}

async function quickAssignInstructor(course: Course) {
	const selectedInstructorId = assignmentSelections.value[course.id]
	if (!selectedInstructorId) {
		toast.error('Select an instructor before assigning.')
		return
	}

	const instructorId = Number(selectedInstructorId)
	if (Number.isNaN(instructorId)) {
		toast.error('Selected instructor is invalid.')
		return
	}

	assignmentLoadingByCourse.value = {
		...assignmentLoadingByCourse.value,
		[course.id]: true,
	}

	try {
		const updatedCourse = await coursesService.assignInstructor(course.id, instructorId)
		const courseIndex = items.value.findIndex((item) => item.id === updatedCourse.id)
		if (courseIndex >= 0) {
			items.value[courseIndex] = updatedCourse
		}
		assignmentSelections.value = {
			...assignmentSelections.value,
			[course.id]: '',
		}
		toast.success(`Assigned instructor to ${updatedCourse.code}`)
	} catch (assignError) {
		const message = (assignError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Failed to assign instructor'
		toast.error(message)
	} finally {
		assignmentLoadingByCourse.value = {
			...assignmentLoadingByCourse.value,
			[course.id]: false,
		}
	}
}

async function loadAuxiliaryData() {
	auxiliaryError.value = null

	const [instructorsResult, roomsResult] = await Promise.allSettled([
		instructorsService.getAll(),
		roomsService.getAll(),
	])

	const failedParts: string[] = []

	if (instructorsResult.status === 'fulfilled') {
		instructors.value = instructorsResult.value
	} else {
		instructors.value = []
		failedParts.push('instructor list')
	}

	if (roomsResult.status === 'fulfilled') {
		rooms.value = roomsResult.value
		capacityCheckAvailable.value = true
	} else {
		rooms.value = []
		capacityCheckAvailable.value = false
		failedParts.push('capacity checks')
	}

	if (failedParts.length > 0) {
		auxiliaryError.value = `Could not load ${failedParts.join(' and ')}.`
	}
}

const columns: Column<Course>[] = [
	{ key: 'code', label: 'Code', render: (course) => course.code },
	{ key: 'name', label: 'Course', render: (course) => course.name },
	{ key: 'credits', label: 'Credits', render: (course) => String(course.credits) },
	{
		key: 'enrollmentCapacity',
		label: 'Enrollment Cap',
		render: formatEnrollmentCapacity,
	},
	{ key: 'department', label: 'Department', render: getDepartment },
	{ key: 'instructor', label: 'Instructor', render: getInstructorName },
	{ key: 'status', label: 'Status', render: getCourseStatusLabel },
]

watch(departmentOptions, (options) => {
	if (departmentFilter.value === 'all') return
	if (!options.some((option) => option.value === departmentFilter.value)) {
		departmentFilter.value = 'all'
	}
})

onMounted(async () => {
	await Promise.all([
		fetchAll(),
		loadAuxiliaryData(),
	])
})
</script>

<template>
	<DataTable title="Courses" :items="filteredCourses" :columns="columns" :loading="loading" :error="error"
		create-route="/courses/new" create-label="Add Course" :edit-route="(course) => `/courses/${course.id}/edit`"
		:on-delete="handleDelete" search-placeholder="Search by code, name, department, instructor, or status">
		<template #filters>
			<select v-model="departmentFilter" aria-label="Department filter"
				class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
				<option value="all">All Departments</option>
				<option v-for="option in departmentOptions" :key="option.value" :value="option.value">
					{{ option.label }}
				</option>
			</select>
		</template>

		<template #metrics="{ filteredItems }">
			<div class="space-y-4">
				<div class="rounded border border-gray-200 bg-white p-4">
					<div class="flex flex-wrap items-start justify-between gap-3">
						<div>
							<div class="text-sm font-semibold text-gray-900">Action Queue</div>
							<div class="mt-0.5 text-xs text-gray-500">Prioritized issues that block scheduling
								readiness.</div>
						</div>
						<button type="button"
							class="rounded border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
							@click="resetFilters">
							Clear Filters
						</button>
					</div>

					<div class="mt-3 grid grid-cols-2 gap-3 xl:grid-cols-4">
						<button type="button" class="rounded border p-3 text-left transition-colors" :class="queueFilter === 'blocking'
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="setQueueFilter('blocking')">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Blocking</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-red-700">{{ blockingCount }}</div>
							<div class="mt-1 text-xs text-gray-500">Fix before solver run</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="queueFilter === 'needs-instructor'
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="setQueueFilter('needs-instructor')">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Needs Instructor
							</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-red-700">{{ unassignedCoursesCount
								}}</div>
							<div class="mt-1 text-xs text-gray-500">Assignable from this page</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="queueFilter === 'capacity-risk'
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="setQueueFilter('capacity-risk')">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Capacity Risk
							</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-amber-800">{{ capacityRiskCount }}
							</div>
							<div class="mt-1 text-xs text-gray-500">No room currently fits</div>
						</button>

						<button type="button" class="rounded border p-3 text-left transition-colors" :class="queueFilter === 'ready'
							? 'border-blue-600 ring-2 ring-blue-200'
							: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="setQueueFilter('ready')">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">Ready</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums text-green-800">{{ readyCount }}</div>
							<div class="mt-1 text-xs text-gray-500">No blocking issues</div>
						</button>
					</div>

					<div v-if="auxiliaryError"
						class="mt-3 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
						{{ auxiliaryError }}
					</div>
				</div>

				<div class="grid grid-cols-2 gap-4 md:grid-cols-4">
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Total Courses</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">{{ items.length }}</div>
						<div class="mt-1 text-xs text-gray-500">Catalog inventory</div>
					</div>
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Shown</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">{{ filteredItems.length }}
						</div>
						<div class="mt-1 text-xs text-gray-500">After active filters</div>
					</div>
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Largest Room</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
							{{ largestRoomCapacity == null ? '-' : `${largestRoomCapacity}` }}
						</div>
						<div class="mt-1 text-xs text-gray-500">Capacity ceiling (seats)</div>
					</div>
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Avg Credits</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">{{ averageCredits }}</div>
						<div class="mt-1 text-xs text-gray-500">Credits per course</div>
					</div>
				</div>
			</div>
		</template>

		<template #cell="{ item, column, value }">
			<RouterLink v-if="column.key === 'code'" :to="`/courses/${item.id}`"
				class="font-medium tracking-wide text-blue-600 hover:underline">
				{{ item.code }}
			</RouterLink>

			<span v-else-if="column.key === 'name'" class="text-gray-800">{{ item.name }}</span>

			<span v-else-if="column.key === 'credits'" class="text-gray-700 tabular-nums">{{ item.credits }}</span>

			<span v-else-if="column.key === 'enrollmentCapacity'" class="text-gray-700 tabular-nums">
				{{ formatEnrollmentCapacity(item) }}
			</span>

			<span v-else-if="column.key === 'department'" class="text-gray-700">{{ getDepartment(item) }}</span>

			<span v-else-if="column.key === 'instructor' && !item.instructor" class="font-medium text-red-700">
				Unassigned
			</span>

			<span v-else-if="column.key === 'instructor'" class="text-gray-700">{{ getInstructorName(item) }}</span>

			<div v-else-if="column.key === 'status'" class="space-y-1">
				<span class="inline-flex items-center rounded border px-2 py-1 text-xs font-medium"
					:class="getCourseStatusClass(item)">
					{{ getCourseStatusLabel(item) }}
				</span>
				<div v-if="getCourseStatusHint(item)" class="text-xs text-gray-500">{{ getCourseStatusHint(item) }}
				</div>
			</div>

			<span v-else class="text-gray-600">{{ value }}</span>
		</template>

		<template #actions="{ item }">
			<div class="flex flex-wrap items-center gap-2">
				<template v-if="!item.instructor">
					<select v-model="assignmentSelections[item.id]" :aria-label="`Assign instructor for ${item.code}`"
						class="h-8 rounded border border-gray-300 bg-white px-2 text-xs text-gray-700">
						<option value="">Assign instructor...</option>
						<option v-for="instructor in instructors" :key="instructor.id" :value="String(instructor.id)">
							{{ instructor.lastName }}, {{ instructor.firstName }}
						</option>
					</select>
					<button type="button"
						class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
						:disabled="isAssigning(item.id) || !assignmentSelections[item.id]"
						@click="quickAssignInstructor(item)">
						{{ isAssigning(item.id) ? 'Assigning...' : 'Assign' }}
					</button>
				</template>
				<RouterLink :to="`/courses/${item.id}/edit`" class="text-blue-600 hover:underline">
					Edit
				</RouterLink>
				<button type="button" class="text-red-600 hover:underline" @click="handleDelete(item.id)">
					Delete
				</button>
			</div>
		</template>

		<template #filtered-empty>
			<div class="rounded border border-gray-200 bg-white p-4 text-sm text-gray-700">
				<div class="font-medium">No courses match {{ activeFilterSummary }}.</div>
				<div class="mt-1 text-gray-600">Adjust filters or search to broaden results.</div>
				<button type="button"
					class="mt-3 rounded border border-gray-300 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
					@click="resetFilters">
					Reset Filters
				</button>
			</div>
		</template>

		<template #empty>
			<EmptyState title="No courses yet"
				description="Courses are the classes that need to be scheduled into rooms. Add courses with enrollment counts and room requirements."
				action-label="Add Course" action-route="/courses/new"
				hint="Each course specifies how many students it has and what type of room it needs." />
		</template>
	</DataTable>
</template>
