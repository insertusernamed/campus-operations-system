<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import type { AxiosError } from 'axios'
import {
	instructorInsightsService,
	type InstructorAssignedCourseContext,
	type InstructorWorkbench,
} from '@/services/instructorInsights'
import { DEFAULT_SEMESTER_DEFINITIONS, semestersService } from '@/services/semesters'
import { getDynamicSemesterOptions } from '@/utils/semester'
import { formatFrictionType } from '@/utils/friction'

const route = useRoute()
const router = useRouter()

const instructorId = computed(() => Number(route.params.id))
const selectedSemester = ref('')
const semesterOptions = ref<string[]>([])

const workbench = ref<InstructorWorkbench | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

const issuesSectionRef = ref<HTMLElement | null>(null)

const focusTarget = computed(() => {
	const value = route.query.focus
	return typeof value === 'string' ? value : ''
})

const frictionGroups = computed(() => {
	const issues = workbench.value?.frictionIssues ?? []
	return {
		HIGH: issues.filter((issue) => issue.severity === 'HIGH'),
		MEDIUM: issues.filter((issue) => issue.severity === 'MEDIUM'),
		LOW: issues.filter((issue) => issue.severity === 'LOW'),
	}
})

const maxDensityMinutes = computed(() => {
	const density = workbench.value?.weeklyDensity ?? []
	if (density.length === 0) return 1
	return Math.max(...density.map((row) => row.totalMinutes), 1)
})

const loadUtilizationPercent = computed(() => {
	if (!workbench.value || workbench.value.targetCreditsMax <= 0) return 0
	const ratio = (workbench.value.assignedCredits / workbench.value.targetCreditsMax) * 100
	return Math.max(0, Math.min(130, Math.round(ratio)))
})

function densityBarPercent(totalMinutes: number): number {
	if (maxDensityMinutes.value <= 0) return 0
	return Math.round((totalMinutes / maxDensityMinutes.value) * 100)
}

function getLoadStatusClass(status: InstructorWorkbench['loadStatus']): string {
	switch (status) {
		case 'OVER':
			return 'border-red-200 bg-red-50 text-red-700'
		case 'UNDER':
			return 'border-amber-200 bg-amber-50 text-amber-800'
		default:
			return 'border-green-200 bg-green-50 text-green-900'
	}
}

function getTrendClass(direction: InstructorWorkbench['loadTrend']['direction']): string {
	switch (direction) {
		case 'UP':
			return 'text-red-700'
		case 'DOWN':
			return 'text-blue-700'
		default:
			return 'text-green-700'
	}
}

function getTrendLabel(direction: InstructorWorkbench['loadTrend']['direction']): string {
	switch (direction) {
		case 'UP':
			return 'Above baseline'
		case 'DOWN':
			return 'Below baseline'
		default:
			return 'Near baseline'
	}
}

function sortAssignedCourses(items: InstructorAssignedCourseContext[]): InstructorAssignedCourseContext[] {
	return [...items].sort((a, b) => {
		if (a.scheduled !== b.scheduled) return a.scheduled ? -1 : 1
		const dayA = a.dayOfWeek ?? 'ZZZ'
		const dayB = b.dayOfWeek ?? 'ZZZ'
		if (dayA !== dayB) return dayA.localeCompare(dayB)
		const startA = a.startTime ?? '99:99'
		const startB = b.startTime ?? '99:99'
		if (startA !== startB) return startA.localeCompare(startB)
		return a.code.localeCompare(b.code)
	})
}

const assignedCourses = computed(() => sortAssignedCourses(workbench.value?.assignedCourses ?? []))

async function fetchWorkbench() {
	if (!instructorId.value || Number.isNaN(instructorId.value) || !selectedSemester.value) return

	loading.value = true
	error.value = null

	try {
		workbench.value = await instructorInsightsService.getWorkbench(instructorId.value, selectedSemester.value)
		await maybeScrollToFocus()
	} catch (fetchError) {
		const status = (fetchError as AxiosError).response?.status
		if (status === 404) {
			error.value = 'Instructor not found.'
		} else {
			const message = (fetchError as AxiosError<{ message?: string }>).response?.data?.message
				|| 'Could not load instructor workbench.'
			error.value = message
		}
		workbench.value = null
	} finally {
		loading.value = false
	}
}

async function maybeScrollToFocus() {
	if (focusTarget.value !== 'issues') return
	await nextTick()
	issuesSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(selectedSemester, async (semester, previous) => {
	if (!semester) return
	if (semester !== previous) {
		await router.replace({
			query: {
				...route.query,
				semester,
			},
		})
	}
	await fetchWorkbench()
})

watch(focusTarget, async () => {
	await maybeScrollToFocus()
})

onMounted(async () => {
	const definitions = await semestersService.getDefinitions().catch(() => DEFAULT_SEMESTER_DEFINITIONS)
	const options = getDynamicSemesterOptions(definitions, new Date(), 6)
	semesterOptions.value = options.length > 0 ? options : ['Fall 2026']

	const querySemester = typeof route.query.semester === 'string' ? route.query.semester : ''
	selectedSemester.value = semesterOptions.value.includes(querySemester)
		? querySemester
		: (semesterOptions.value[0] ?? 'Fall 2026')

	await fetchWorkbench()
})
</script>

<template>
	<div>
		<div class="mb-6 flex items-center justify-between gap-3">
			<RouterLink to="/instructors" class="text-sm text-blue-600 hover:underline">
				Back to Instructors
			</RouterLink>

			<select v-model="selectedSemester" aria-label="Semester selector"
				class="h-10 rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700">
				<option v-for="semester in semesterOptions" :key="semester" :value="semester">
					{{ semester }}
				</option>
			</select>
		</div>

		<div v-if="loading" class="rounded border border-gray-200 bg-white p-6 text-sm text-gray-600">
			Loading instructor workbench...
		</div>
		<div v-else-if="error" class="rounded border border-red-200 bg-red-50 p-6 text-sm text-red-700">
			{{ error }}
		</div>

		<template v-else-if="workbench">
			<section class="rounded border border-gray-200 bg-white p-6">
				<div class="flex flex-wrap items-start justify-between gap-3">
					<div>
						<h1 class="text-2xl font-semibold text-gray-900">{{ workbench.firstName }} {{ workbench.lastName }}</h1>
						<div class="mt-1 text-sm text-gray-600">
							{{ workbench.department }}
							<span v-if="workbench.officeNumber"> • {{ workbench.officeNumber }}</span>
						</div>
					</div>
					<div class="flex items-center gap-2">
						<span class="inline-flex items-center rounded border px-2 py-1 text-xs font-medium"
							:class="getLoadStatusClass(workbench.loadStatus)">
							{{ workbench.loadStatus }}
						</span>
						<RouterLink :to="`/instructors/${workbench.instructorId}/edit`"
							class="rounded border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50">
							Edit Profile
						</RouterLink>
					</div>
				</div>

				<div class="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
					<div class="rounded border border-gray-200 p-3">
						<div class="text-xs text-gray-500">Assigned Credits</div>
						<div class="mt-1 text-xl font-semibold tabular-nums text-gray-900">{{ workbench.assignedCredits }}</div>
						<div class="mt-1 text-xs text-gray-500">Target {{ workbench.targetCreditsMin }}-{{ workbench.targetCreditsMax }}</div>
					</div>
					<div class="rounded border border-gray-200 p-3">
						<div class="text-xs text-gray-500">Preference Setup</div>
						<div class="mt-1 text-xl font-semibold tabular-nums text-gray-900">{{ workbench.preferenceCompletenessPercent }}%</div>
						<div class="mt-2 h-1.5 w-full overflow-hidden rounded bg-gray-100">
							<div class="h-full rounded bg-blue-500" :style="{ width: `${workbench.preferenceCompletenessPercent}%` }"></div>
						</div>
					</div>
					<div class="rounded border border-gray-200 p-3">
						<div class="text-xs text-gray-500">Friction Score</div>
						<div class="mt-1 text-xl font-semibold tabular-nums text-gray-900">{{ workbench.frictionScore }}</div>
						<div class="mt-1 text-xs text-gray-500">{{ workbench.frictionSummary.total }} active issues</div>
					</div>
					<div class="rounded border border-gray-200 p-3">
						<div class="text-xs text-gray-500">Assigned Courses</div>
						<div class="mt-1 text-xl font-semibold tabular-nums text-gray-900">{{ workbench.assignedCoursesCount }}</div>
						<div class="mt-1 text-xs text-gray-500">in {{ workbench.semester }}</div>
					</div>
				</div>
			</section>

			<section class="mt-4 grid gap-4 lg:grid-cols-[1.5fr_1fr]">
				<div class="rounded border border-gray-200 bg-white p-4">
					<div class="text-sm font-semibold text-gray-900">Teaching Load Trend</div>
					<div class="mt-2 flex items-end gap-4">
						<div>
							<div class="text-xs text-gray-500">Current Term</div>
							<div class="text-2xl font-semibold tabular-nums text-gray-900">{{ workbench.loadTrend.currentCredits }}</div>
						</div>
						<div>
							<div class="text-xs text-gray-500">Baseline</div>
							<div class="text-2xl font-semibold tabular-nums text-gray-900">{{ workbench.loadTrend.baselineCredits.toFixed(1) }}</div>
						</div>
						<div>
							<div class="text-xs text-gray-500">Delta</div>
							<div class="text-2xl font-semibold tabular-nums" :class="getTrendClass(workbench.loadTrend.direction)">
								{{ workbench.loadTrend.deltaCredits > 0 ? '+' : '' }}{{ workbench.loadTrend.deltaCredits.toFixed(1) }}
							</div>
						</div>
					</div>
					<div class="mt-2 text-xs" :class="getTrendClass(workbench.loadTrend.direction)">
						{{ getTrendLabel(workbench.loadTrend.direction) }}
					</div>
					<div class="mt-3 h-2 w-full overflow-hidden rounded bg-gray-100">
						<div class="h-full rounded" :class="workbench.loadStatus === 'OVER'
							? 'bg-red-500'
							: (workbench.loadStatus === 'UNDER' ? 'bg-amber-500' : 'bg-green-500')" :style="{ width: `${loadUtilizationPercent}%` }"></div>
					</div>
				</div>

				<div class="rounded border border-gray-200 bg-white p-4">
					<div class="text-sm font-semibold text-gray-900">Recommended Actions</div>
					<ul class="mt-3 space-y-2">
						<li v-for="action in workbench.recommendedActions" :key="action"
							class="rounded border border-gray-200 bg-gray-50 px-3 py-2 text-sm text-gray-700">
							{{ action }}
						</li>
					</ul>
				</div>
			</section>

			<section class="mt-4 rounded border border-gray-200 bg-white p-4">
				<div class="text-sm font-semibold text-gray-900">Weekly Schedule Density</div>
				<div class="mt-3 space-y-2">
					<div v-for="row in workbench.weeklyDensity" :key="row.dayOfWeek" class="grid grid-cols-[70px_1fr_70px] items-center gap-3">
						<div class="text-sm text-gray-700">{{ row.dayOfWeek }}</div>
						<div class="h-2 w-full overflow-hidden rounded bg-gray-100">
							<div class="h-full rounded bg-blue-500" :style="{ width: `${densityBarPercent(row.totalMinutes)}%` }"></div>
						</div>
						<div class="text-right text-xs text-gray-500">{{ row.classCount }} class{{ row.classCount === 1 ? '' : 'es' }}</div>
					</div>
				</div>
			</section>

			<section class="mt-4 rounded border border-gray-200 bg-white p-4">
				<div class="text-sm font-semibold text-gray-900">Assigned Courses Context</div>
				<div class="mt-3 overflow-x-auto">
					<table class="w-full border border-gray-200">
						<thead>
							<tr class="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
								<th class="px-3 py-2">Course</th>
								<th class="px-3 py-2">Credits</th>
								<th class="px-3 py-2">Capacity</th>
								<th class="px-3 py-2">Timeslot</th>
								<th class="px-3 py-2">Room</th>
							</tr>
						</thead>
						<tbody>
							<tr v-for="course in assignedCourses" :key="`${course.courseId}-${course.dayOfWeek ?? 'unscheduled'}`"
								class="border-t border-gray-100 text-sm">
								<td class="px-3 py-2">
									<div class="font-medium text-gray-900">{{ course.code }}</div>
									<div class="text-xs text-gray-500">{{ course.name }}</div>
								</td>
								<td class="px-3 py-2 text-gray-700 tabular-nums">{{ course.credits }}</td>
								<td class="px-3 py-2 text-gray-700 tabular-nums">{{ course.enrollmentCapacity }}</td>
								<td class="px-3 py-2 text-gray-700">
									<span v-if="course.scheduled">
										{{ course.dayOfWeek }} {{ course.startTime }}-{{ course.endTime }}
									</span>
									<span v-else class="text-amber-700">Unscheduled</span>
								</td>
								<td class="px-3 py-2 text-gray-700">{{ course.roomLabel ?? 'TBD' }}</td>
							</tr>
						</tbody>
					</table>
				</div>
			</section>

			<section ref="issuesSectionRef" class="mt-4 rounded border border-gray-200 bg-white p-4">
				<div class="flex items-start justify-between gap-3">
					<div>
						<div class="text-sm font-semibold text-gray-900">Friction Issues by Severity</div>
						<div class="mt-0.5 text-xs text-gray-500">Grouped operational issues for targeted triage.</div>
					</div>
					<div class="text-xs text-gray-500">{{ workbench.frictionSummary.total }} total</div>
				</div>

				<div class="mt-3 grid gap-3 lg:grid-cols-3">
					<div class="rounded border border-red-200 bg-red-50 p-3">
						<div class="text-xs font-semibold uppercase tracking-wide text-red-700">High</div>
						<div v-if="frictionGroups.HIGH.length === 0" class="mt-2 text-xs text-red-700/80">No high-severity issues.</div>
						<ul v-else class="mt-2 space-y-2">
							<li v-for="issue in frictionGroups.HIGH" :key="issue.id" class="rounded border border-red-200 bg-white p-2">
								<div class="text-xs font-medium text-red-700">{{ formatFrictionType(issue.type) }}</div>
								<div class="mt-1 text-xs text-gray-700">{{ issue.message }}</div>
							</li>
						</ul>
					</div>

					<div class="rounded border border-amber-200 bg-amber-50 p-3">
						<div class="text-xs font-semibold uppercase tracking-wide text-amber-800">Medium</div>
						<div v-if="frictionGroups.MEDIUM.length === 0" class="mt-2 text-xs text-amber-800/80">No medium-severity issues.</div>
						<ul v-else class="mt-2 space-y-2">
							<li v-for="issue in frictionGroups.MEDIUM" :key="issue.id" class="rounded border border-amber-200 bg-white p-2">
								<div class="text-xs font-medium text-amber-800">{{ formatFrictionType(issue.type) }}</div>
								<div class="mt-1 text-xs text-gray-700">{{ issue.message }}</div>
							</li>
						</ul>
					</div>

					<div class="rounded border border-blue-200 bg-blue-50 p-3">
						<div class="text-xs font-semibold uppercase tracking-wide text-blue-700">Low</div>
						<div v-if="frictionGroups.LOW.length === 0" class="mt-2 text-xs text-blue-700/80">No low-severity issues.</div>
						<ul v-else class="mt-2 space-y-2">
							<li v-for="issue in frictionGroups.LOW" :key="issue.id" class="rounded border border-blue-200 bg-white p-2">
								<div class="text-xs font-medium text-blue-700">{{ formatFrictionType(issue.type) }}</div>
								<div class="mt-1 text-xs text-gray-700">{{ issue.message }}</div>
							</li>
						</ul>
					</div>
				</div>
			</section>

			<section class="mt-4 rounded border border-gray-200 bg-white p-4">
				<div class="text-sm font-semibold text-gray-900">What Changed</div>
				<div v-if="workbench.recentChanges.length === 0" class="mt-3 text-sm text-gray-600">
					No recent assignment change traces for this semester.
				</div>
				<ul v-else class="mt-3 space-y-2">
					<li v-for="change in workbench.recentChanges" :key="`${change.timestamp}-${change.label}`"
						class="rounded border border-gray-200 bg-gray-50 px-3 py-2">
						<div class="text-xs font-medium uppercase tracking-wide text-gray-500">{{ change.source }}</div>
						<div class="mt-1 text-sm text-gray-700">{{ change.label }}</div>
						<div class="mt-1 text-xs text-gray-500">{{ change.timestamp }}</div>
					</li>
				</ul>
			</section>
		</template>
	</div>
</template>
