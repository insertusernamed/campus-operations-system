<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { AxiosError } from 'axios'
import { RouterLink } from 'vue-router'
import { toast } from 'vue3-toastify'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useRole } from '@/composables/useRole'
import { coursesService, type Course } from '@/services/courses'
import {
	instructorInsightsService,
	type InstructorDepartmentLoad,
	type InstructorInsightsSummary,
	type InstructorOperationalStatus,
	type InstructorQueueFilter,
	type InstructorQueueRow,
} from '@/services/instructorInsights'
import { instructorsService } from '@/services/instructors'
import { DEFAULT_SEMESTER_DEFINITIONS, semestersService } from '@/services/semesters'
import { getDynamicSemesterOptions } from '@/utils/semester'

type SnapshotCard = {
	key: string
	title: string
	value: number
	hint: string
	filter: InstructorQueueFilter
	valueClass: string
}

const queueFilterLabels: Record<InstructorQueueFilter, string> = {
	all: 'All Instructors',
	'coverage-risk': 'Coverage Risk',
	overloaded: 'Overloaded',
	'under-utilized': 'Under-utilized',
	'preference-incomplete': 'Preference Incomplete',
	'friction-hotspots': 'Friction Hotspots',
	ready: 'Ready',
}

const queueFilterOptions = computed(() =>
	Object.entries(queueFilterLabels).map(([value, label]) => ({
		value: value as InstructorQueueFilter,
		label,
	})),
)

const queueFilter = ref<InstructorQueueFilter>('all')
const departmentFilter = ref('all')
const selectedSemester = ref('')
const semesterOptions = ref<string[]>([])

const loading = ref(false)
const error = ref<string | null>(null)
const auxiliaryError = ref<string | null>(null)

const summary = ref<InstructorInsightsSummary | null>(null)
const queueRows = ref<InstructorQueueRow[]>([])
const departmentLoad = ref<InstructorDepartmentLoad[]>([])
const openCourses = ref<Course[]>([])

const assignmentSelections = ref<Record<number, string>>({})
const actionLoadingByInstructor = ref<Record<number, boolean>>({})

const { isAdmin } = useRole()

function normalizeDepartment(value: string | null | undefined): string {
	const department = value?.trim()
	return department && department.length > 0 ? department : 'Undeclared'
}

const departmentOptions = computed(() => {
	return departmentLoad.value
		.map((row) => ({
			value: row.department,
			label: `${row.department} (${row.unfilledCourseCount} unfilled)`,
		}))
		.sort((a, b) => a.value.localeCompare(b.value))
})

watch(departmentOptions, (options) => {
	if (departmentFilter.value === 'all') return
	if (!options.some((option) => option.value === departmentFilter.value)) {
		departmentFilter.value = 'all'
	}
})

const snapshotCards = computed<SnapshotCard[]>(() => {
	const payload = summary.value
	if (!payload) {
		return [
			{
				key: 'no-assignment',
				title: 'No Assignment',
				value: 0,
				hint: 'No current teaching load',
				filter: 'under-utilized',
				valueClass: 'text-amber-800',
			},
		]
	}

	return [
		{
			key: 'no-assignment',
			title: 'No Assignment',
			value: payload.noCurrentAssignment,
			hint: 'No current teaching load',
			filter: 'under-utilized',
			valueClass: 'text-amber-800',
		},
		{
			key: 'overload-risk',
			title: 'Overload Risk',
			value: payload.overloadRisk,
			hint: 'Above policy threshold',
			filter: 'overloaded',
			valueClass: 'text-red-700',
		},
		{
			key: 'preference-incomplete',
			title: 'Preference Incomplete',
			value: payload.preferenceSetupIncomplete,
			hint: 'Needs profile setup',
			filter: 'preference-incomplete',
			valueClass: 'text-amber-800',
		},
		{
			key: 'friction-hotspots',
			title: 'Friction Hotspots',
			value: payload.frictionHotspots,
			hint: 'High-friction schedules',
			filter: 'friction-hotspots',
			valueClass: 'text-red-700',
		},
		{
			key: 'coverage-risk',
			title: 'Dept Coverage Risk',
			value: payload.departmentsWithCoverageRisk,
			hint: 'Departments with unfilled demand',
			filter: 'coverage-risk',
			valueClass: 'text-red-700',
		},
	]
})

const topCoverageDepartments = computed(() => {
	return departmentLoad.value
		.filter((row) => row.unfilledCredits > 0)
		.slice(0, 4)
})

const activeFilterSummary = computed(() => {
	const summaryParts: string[] = []

	if (queueFilter.value !== 'all') {
		summaryParts.push(`view: ${queueFilterLabels[queueFilter.value].toLowerCase()}`)
	}
	if (departmentFilter.value !== 'all') {
		summaryParts.push(`department: ${departmentFilter.value}`)
	}
	if (selectedSemester.value) {
		summaryParts.push(`semester: ${selectedSemester.value}`)
	}

	return summaryParts.length > 0 ? summaryParts.join(', ') : 'current filters'
})

function setQueueFilter(nextFilter: InstructorQueueFilter) {
	queueFilter.value = nextFilter
}

function resetFilters() {
	queueFilter.value = 'all'
	departmentFilter.value = 'all'
}

function setActionLoading(instructorId: number, nextValue: boolean) {
	actionLoadingByInstructor.value = {
		...actionLoadingByInstructor.value,
		[instructorId]: nextValue,
	}
}

function isActionLoading(instructorId: number): boolean {
	return Boolean(actionLoadingByInstructor.value[instructorId])
}

function getFrictionBadgeClass(item: InstructorQueueRow): string {
	switch (item.frictionSeverity) {
		case 'HIGH':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'MEDIUM':
			return 'border-amber-200 bg-amber-50 text-amber-800'
		default:
			return 'border-blue-200 bg-blue-50 text-blue-700'
	}
}

function getStatusLabel(status: InstructorOperationalStatus): string {
	switch (status) {
		case 'COVERAGE_RISK':
			return 'Coverage Risk'
		case 'OVERLOADED':
			return 'Overloaded'
		case 'UNDER_UTILIZED':
			return 'Under-utilized'
		case 'PREFERENCE_INCOMPLETE':
			return 'Preference Incomplete'
		case 'FRICTION_HOTSPOT':
			return 'Friction Hotspot'
		default:
			return 'Ready'
	}
}

function getStatusClass(status: InstructorOperationalStatus): string {
	switch (status) {
		case 'COVERAGE_RISK':
		case 'OVERLOADED':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'PREFERENCE_INCOMPLETE':
		case 'FRICTION_HOTSPOT':
		case 'UNDER_UTILIZED':
			return 'border-amber-200 bg-amber-50 text-amber-800'
		default:
			return 'border-green-200 bg-green-50 text-green-900'
	}
}

const columns: Column<InstructorQueueRow>[] = [
	{
		key: 'fullName',
		label: 'Instructor',
		render: (item) => `${item.fullName} ${item.department}`,
	},
	{
		key: 'assignedCoursesCount',
		label: 'Assigned Courses',
		render: (item) => String(item.assignedCoursesCount),
	},
	{
		key: 'assignedCredits',
		label: 'Credits / Target',
		render: (item) => `${item.assignedCredits} / ${item.targetCreditsMin}-${item.targetCreditsMax}`,
	},
	{
		key: 'frictionScore',
		label: 'Friction',
		render: (item) => `${item.frictionSeverity} (${item.frictionIssueCount})`,
	},
	{
		key: 'preferenceCompletenessPercent',
		label: 'Preferences',
		render: (item) => `${item.preferenceCompletenessPercent}%`,
	},
	{
		key: 'status',
		label: 'Status',
		render: (item) => getStatusLabel(item.status),
	},
]

function availableCoursesForInstructor(instructor: InstructorQueueRow): Course[] {
	const departmentMatches = openCourses.value.filter(
		(course) => normalizeDepartment(course.department) === instructor.department,
	)
	if (departmentMatches.length > 0) {
		return departmentMatches.sort((a, b) => b.credits - a.credits)
	}
	return [...openCourses.value].sort((a, b) => b.credits - a.credits)
}

async function quickAssignOpenCourse(instructor: InstructorQueueRow) {
	if (!isAdmin.value) return

	const selectedCourseId = Number(assignmentSelections.value[instructor.id] ?? '')
	if (!selectedCourseId || Number.isNaN(selectedCourseId)) {
		toast.error('Select an open course before assigning.')
		return
	}

	setActionLoading(instructor.id, true)
	try {
		await coursesService.assignInstructor(selectedCourseId, instructor.id)
		assignmentSelections.value = {
			...assignmentSelections.value,
			[instructor.id]: '',
		}
		toast.success(`Assigned open course to ${instructor.fullName}`)
		await Promise.all([refreshInsights(), refreshOpenCourses()])
	} catch (assignError) {
		const message = (assignError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Failed to assign course'
		toast.error(message)
	} finally {
		setActionLoading(instructor.id, false)
	}
}

async function quickRebalance(instructor: InstructorQueueRow) {
	if (!isAdmin.value) return

	const candidateTarget = queueRows.value
		.filter((row) =>
			row.id !== instructor.id
			&& row.department === instructor.department
			&& row.loadStatus === 'UNDER',
		)
		.sort((a, b) => b.underUtilizedCredits - a.underUtilizedCredits)[0]

	if (!candidateTarget) {
		toast.info('No under-utilized instructor found in this department.')
		return
	}

	setActionLoading(instructor.id, true)
	try {
		const assignedCourses = await coursesService.getByInstructor(instructor.id)
		if (assignedCourses.length === 0) {
			toast.info('No assigned courses available to rebalance.')
			return
		}

		const courseToMove = [...assignedCourses].sort((a, b) => b.credits - a.credits)[0]
		if (!courseToMove) {
			toast.info('No assigned courses available to rebalance.')
			return
		}
		const confirmed = window.confirm(
			`Move ${courseToMove.code} from ${instructor.fullName} to ${candidateTarget.fullName}?`,
		)
		if (!confirmed) return

		await coursesService.assignInstructor(courseToMove.id, candidateTarget.id)
		toast.success(`Rebalanced ${courseToMove.code} to ${candidateTarget.fullName}`)
		await refreshInsights()
	} catch (rebalanceError) {
		const message = (rebalanceError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Failed to rebalance load'
		toast.error(message)
	} finally {
		setActionLoading(instructor.id, false)
	}
}

function requestPreferenceUpdate(instructor: InstructorQueueRow) {
	const subject = encodeURIComponent('Teaching preference profile update request')
	const body = encodeURIComponent(
		`Hi ${instructor.firstName},%0D%0A%0D%0APlease review and update your scheduling preferences so we can reduce friction and improve assignment quality for ${selectedSemester.value}.%0D%0A%0D%0AThanks.`,
	)
	window.open(`mailto:${instructor.email}?subject=${subject}&body=${body}`, '_blank', 'noopener')
}

async function handleDelete(instructorId: number) {
	if (!isAdmin.value) return

	const confirmed = window.confirm('Are you sure you want to delete this instructor?')
	if (!confirmed) return

	try {
		await instructorsService.delete(instructorId)
		toast.success('Instructor deleted')
		await refreshInsights()
	} catch (deleteError) {
		const message = (deleteError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Failed to delete instructor'
		toast.error(message)
	}
}

async function refreshOpenCourses() {
	try {
		const courses = await coursesService.getAll()
		openCourses.value = courses.filter((course) => !course.instructor)
	} catch (fetchError) {
		const message = (fetchError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Could not load open courses for quick assign.'
		auxiliaryError.value = message
	}
}

async function refreshInsights() {
	if (!selectedSemester.value) return

	loading.value = true
	error.value = null
	auxiliaryError.value = null

	try {
		const [summaryPayload, queuePayload, loadPayload] = await Promise.all([
			instructorInsightsService.getSummary(selectedSemester.value),
			instructorInsightsService.getQueue({
				semester: selectedSemester.value,
				filter: queueFilter.value,
				department: departmentFilter.value === 'all' ? undefined : departmentFilter.value,
			}),
			instructorInsightsService.getLoadDistribution(selectedSemester.value),
		])

		summary.value = summaryPayload
		queueRows.value = queuePayload
		departmentLoad.value = loadPayload.departments
	} catch (fetchError) {
		const message = (fetchError as AxiosError<{ message?: string }>).response?.data?.message
			|| 'Could not load instructor operations data.'
		error.value = message
		queueRows.value = []
		summary.value = null
		departmentLoad.value = []
	} finally {
		loading.value = false
	}
}

watch([queueFilter, departmentFilter, selectedSemester], async ([, , semester], [, , oldSemester]) => {
	if (!semester) return
	if (semester !== oldSemester) {
		await Promise.all([refreshInsights(), refreshOpenCourses()])
		return
	}
	await refreshInsights()
})

onMounted(async () => {
	const definitions = await semestersService.getDefinitions().catch(() => DEFAULT_SEMESTER_DEFINITIONS)
	const options = getDynamicSemesterOptions(definitions, new Date(), 6)
	semesterOptions.value = options.length > 0 ? options : ['Fall 2026']
	selectedSemester.value = semesterOptions.value[0] ?? 'Fall 2026'

	await Promise.all([refreshInsights(), refreshOpenCourses()])
})
</script>

<template>
	<DataTable title="Instructor Operations" :items="queueRows" :columns="columns" :loading="loading" :error="error"
		:create-route="isAdmin ? '/instructors/new' : undefined" create-label="Add Instructor"
		:edit-route="(item) => `/instructors/${item.id}/edit`"
		search-placeholder="Search by instructor, department, status, load, or friction">
		<template #filters>
			<select v-model="selectedSemester" aria-label="Semester filter"
				class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
				<option v-for="semester in semesterOptions" :key="semester" :value="semester">
					{{ semester }}
				</option>
			</select>

			<select v-model="departmentFilter" aria-label="Department filter"
				class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
				<option value="all">All Departments</option>
				<option v-for="option in departmentOptions" :key="option.value" :value="option.value">
					{{ option.label }}
				</option>
			</select>
		</template>

		<template #metrics>
			<div class="space-y-4">
				<div class="rounded border border-gray-200 bg-white p-4">
					<div class="flex flex-wrap items-start justify-between gap-3">
						<div>
							<div class="text-sm font-semibold text-gray-900">Instructor Operations Snapshot</div>
							<div class="mt-0.5 text-xs text-gray-500">Click a card to jump to the matching queue view.</div>
						</div>
						<button type="button"
							class="rounded border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50"
							@click="resetFilters">
							Clear Filters
						</button>
					</div>

					<div class="mt-3 grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
						<button v-for="card in snapshotCards" :key="card.key" type="button"
							class="rounded border p-3 text-left transition-colors" :class="queueFilter === card.filter
								? 'border-blue-600 ring-2 ring-blue-200'
								: 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'" @click="setQueueFilter(card.filter)">
							<div class="text-[11px] font-medium uppercase tracking-wide text-gray-500">{{ card.title }}</div>
							<div class="mt-1 text-2xl font-semibold tabular-nums" :class="card.valueClass">{{ card.value }}</div>
							<div class="mt-1 text-xs text-gray-500">{{ card.hint }}</div>
						</button>
					</div>
				</div>

				<div class="grid gap-4 xl:grid-cols-[2fr_1fr]">
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-semibold text-gray-900">Action Queue</div>
						<div class="mt-0.5 text-xs text-gray-500">Primary triage controls for staffing risk and load balance.</div>

						<div class="mt-3 flex flex-wrap gap-2">
							<button v-for="option in queueFilterOptions" :key="option.value" type="button"
								class="rounded-full border px-3 py-1.5 text-xs font-medium transition-colors" :class="queueFilter === option.value
									? 'border-blue-600 bg-blue-50 text-blue-700'
									: 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'" @click="setQueueFilter(option.value)">
								{{ option.label }}
							</button>
						</div>

						<div v-if="auxiliaryError"
							class="mt-3 rounded border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900">
							{{ auxiliaryError }}
						</div>
					</div>

					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-semibold text-gray-900">Coverage Panel</div>
						<div v-if="topCoverageDepartments.length === 0" class="mt-3 text-sm text-gray-600">
							No department has unfilled demand in this view.
						</div>
						<div v-else class="mt-3 space-y-2">
							<div v-for="row in topCoverageDepartments" :key="row.department"
								class="rounded border border-gray-200 px-3 py-2">
								<div class="flex items-center justify-between gap-2">
									<div class="text-sm font-medium text-gray-900">{{ row.department }}</div>
									<div class="text-xs text-gray-500">{{ row.unfilledCourseCount }} unfilled</div>
								</div>
								<div class="mt-1 text-xs text-gray-600">{{ row.unfilledCredits }} credits unassigned</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</template>

		<template #cell="{ item, column, value }">
			<div v-if="column.key === 'fullName'" class="space-y-1">
				<RouterLink :to="`/instructors/${item.id}?semester=${encodeURIComponent(selectedSemester)}`"
					class="font-medium text-blue-600 hover:underline">
					{{ item.fullName }}
				</RouterLink>
				<div class="text-xs text-gray-500">{{ item.department }}</div>
			</div>

			<div v-else-if="column.key === 'assignedCoursesCount'" class="space-y-1">
				<div class="font-medium tabular-nums text-gray-900">{{ item.assignedCoursesCount }}</div>
				<div class="text-xs text-gray-500">{{ item.assignedCredits }} credits total</div>
			</div>

			<div v-else-if="column.key === 'assignedCredits'" class="space-y-1">
				<div class="font-medium tabular-nums text-gray-900">{{ item.assignedCredits }} / {{ item.targetCreditsMin }}-{{
					item.targetCreditsMax }}</div>
				<div class="text-xs text-gray-500">{{ item.loadStatus }}</div>
			</div>

			<div v-else-if="column.key === 'frictionScore'" class="space-y-1">
				<span class="inline-flex items-center rounded border px-2 py-1 text-xs font-medium"
					:class="getFrictionBadgeClass(item)">
					{{ item.frictionSeverity }}
				</span>
				<div class="text-xs text-gray-500">{{ item.frictionIssueCount }} issue{{ item.frictionIssueCount === 1 ? '' : 's'
				}}</div>
			</div>

			<div v-else-if="column.key === 'preferenceCompletenessPercent'" class="space-y-1">
				<div class="font-medium tabular-nums text-gray-900">{{ item.preferenceCompletenessPercent }}%</div>
				<div class="h-1.5 w-24 overflow-hidden rounded bg-gray-100">
					<div class="h-full rounded bg-blue-500" :style="{ width: `${item.preferenceCompletenessPercent}%` }"></div>
				</div>
			</div>

			<div v-else-if="column.key === 'status'" class="space-y-1">
				<span class="inline-flex items-center rounded border px-2 py-1 text-xs font-medium"
					:class="getStatusClass(item.status)">
					{{ getStatusLabel(item.status) }}
				</span>
			</div>

			<span v-else class="text-gray-600">{{ value }}</span>
		</template>

		<template #actions="{ item }">
			<div class="flex flex-wrap items-center gap-2">
				<template v-if="isAdmin">
					<select v-model="assignmentSelections[item.id]" :aria-label="`Assign open course to ${item.fullName}`"
						class="h-8 rounded border border-gray-300 bg-white px-2 text-xs text-gray-700">
						<option value="">Assign to open course...</option>
						<option v-for="course in availableCoursesForInstructor(item)" :key="course.id" :value="String(course.id)">
							{{ course.code }} ({{ course.credits }}cr)
						</option>
					</select>
					<button type="button"
						class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
						:disabled="isActionLoading(item.id) || !assignmentSelections[item.id]"
						@click="quickAssignOpenCourse(item)">
						{{ isActionLoading(item.id) ? 'Assigning...' : 'Assign' }}
					</button>

					<button type="button"
						class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
						:disabled="isActionLoading(item.id)" @click="quickRebalance(item)">
						Rebalance
					</button>
				</template>

				<button type="button"
					class="h-8 rounded border border-gray-300 bg-white px-2 text-xs font-medium text-gray-700 hover:bg-gray-50"
					@click="requestPreferenceUpdate(item)">
					Request Prefs
				</button>

				<RouterLink
					:to="`/instructors/${item.id}?semester=${encodeURIComponent(selectedSemester)}&focus=issues`"
					class="text-blue-600 hover:underline">
					Open Issues
				</RouterLink>

				<RouterLink v-if="isAdmin" :to="`/instructors/${item.id}/edit`" class="text-blue-600 hover:underline">
					Edit
				</RouterLink>
				<button v-if="isAdmin" type="button" class="text-red-600 hover:underline" @click="handleDelete(item.id)">
					Delete
				</button>
			</div>
		</template>

		<template #filtered-empty>
			<div class="rounded border border-gray-200 bg-white p-4 text-sm text-gray-700">
				<div class="font-medium">No instructors match {{ activeFilterSummary }}.</div>
				<div class="mt-1 text-gray-600">Adjust filters or search to broaden results.</div>
				<button type="button"
					class="mt-3 rounded border border-gray-300 px-3 py-1.5 text-xs text-gray-700 hover:bg-gray-50"
					@click="resetFilters">
					Reset Filters
				</button>
			</div>
		</template>

		<template #empty>
			<EmptyState title="No instructors yet"
				description="Instructors power staffing and scheduling readiness. Add instructors to begin load planning and risk triage."
				action-label="Add Instructor" action-route="/instructors/new" />
		</template>
	</DataTable>
</template>
