<template>
	<div class="space-y-6">
		<!-- Admin dashboard -->
		<section v-if="isAdmin" class="space-y-6">
			<div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
				<div>
					<h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
					<p class="mt-1 text-sm text-gray-600">
						Admin overview and approvals.
						<span v-if="pendingRequests !== null" class="text-gray-500">
							<span class="mx-1">|</span>
							<span v-if="pendingCount > 0">{{ pendingCount }} pending request{{ pendingCount === 1 ? '' :
								's' }}</span>
							<span v-else>No pending requests</span>
						</span>
					</p>
				</div>

				<div class="flex flex-wrap gap-2">
					<RouterLink to="/requests/admin"
						class="inline-flex items-center justify-center rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
						Change Requests
						<span v-if="pendingCount > 0"
							class="ml-2 inline-flex items-center rounded bg-blue-500 px-2 py-0.5 text-xs font-semibold text-white">
							{{ pendingCount }}
						</span>
					</RouterLink>
					<RouterLink to="/schedules/new"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						Add Schedule
					</RouterLink>
					<RouterLink to="/solver"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						Solver
					</RouterLink>
					<RouterLink to="/analytics"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						Analytics
					</RouterLink>
					<button @click="refresh" :disabled="isRefreshing"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50 disabled:opacity-50">
						{{ isRefreshing ? 'Refreshing...' : 'Refresh' }}
					</button>
				</div>
			</div>

			<div v-if="hasNoData" class="rounded-lg border border-amber-200 bg-amber-50 p-4 text-amber-900">
				<div class="flex flex-wrap items-center justify-between gap-3">
					<div>
						<div class="text-sm font-medium">No demo data yet</div>
						<p class="mt-0.5 text-sm text-amber-800">
							Generate a dataset to explore analytics, run the solver, and create schedules.
						</p>
					</div>
					<RouterLink to="/solver"
						class="inline-flex items-center justify-center rounded bg-amber-900 px-4 py-2 text-sm font-medium text-white hover:bg-amber-950">
						Generate Demo Data
					</RouterLink>
				</div>
			</div>

			<!-- KPIs -->
			<div class="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-6 gap-4">
				<RouterLink v-for="kpi in kpis" :key="kpi.label" :to="kpi.to"
					class="group rounded border border-gray-200 bg-white p-4 hover:border-gray-300 hover:bg-gray-50 transition-colors">
					<div class="text-sm font-medium text-gray-900">{{ kpi.label }}</div>
					<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
						<span v-if="statsLoading && stats === null"
							class="inline-block h-7 w-10 rounded bg-gray-200 animate-pulse"></span>
						<span v-else>{{ kpi.value }}</span>
					</div>
					<div v-if="kpi.sublabel" class="mt-1 text-xs text-gray-500">{{ kpi.sublabel }}</div>
				</RouterLink>
			</div>

			<!-- Main panels -->
			<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
				<div class="lg:col-span-2 rounded border border-gray-200 bg-white overflow-hidden">
					<div class="flex items-center justify-between gap-4 border-b border-gray-200 p-4">
						<div>
							<h2 class="text-sm font-semibold text-gray-900">Change Requests</h2>
							<p class="mt-0.5 text-xs text-gray-500">Pending items that need review.</p>
						</div>
						<RouterLink to="/requests/admin" class="text-sm font-medium text-blue-700 hover:underline">
							View all
						</RouterLink>
					</div>

					<div v-if="pendingLoading && pendingRequests === null" class="p-4 space-y-3">
						<div v-for="i in 5" :key="i" class="animate-pulse">
							<div class="h-4 w-56 rounded bg-gray-200"></div>
							<div class="mt-2 h-3 w-80 rounded bg-gray-100"></div>
						</div>
					</div>

					<div v-else-if="pendingError" class="p-4">
						<div class="text-sm text-red-700">{{ pendingError }}</div>
						<button @click="fetchPending" class="mt-2 text-sm font-medium text-blue-700 hover:underline">
							Retry
						</button>
					</div>

					<div v-else-if="pendingCount === 0" class="p-6">
						<div class="text-sm font-medium text-gray-900">No pending requests</div>
						<p class="mt-1 text-sm text-gray-600">New instructor requests will appear here.</p>
					</div>

					<ul v-else class="divide-y divide-gray-100">
						<li v-for="request in pendingPreview" :key="request.id" class="p-4 flex items-start gap-4">
							<div class="flex-1 min-w-0">
								<div class="flex flex-wrap items-center gap-x-2 gap-y-1">
									<span class="font-semibold text-gray-900">{{ request.schedule.course.code }}</span>
									<span class="text-sm text-gray-600 truncate">{{ request.schedule.course.name
										}}</span>
								</div>
								<div class="mt-1 text-xs text-gray-500">
									<span class="font-medium text-gray-700">
										{{ request.requestedByInstructor.firstName }} {{
											request.requestedByInstructor.lastName }}
									</span>
									<span class="mx-1">|</span>
									<span>{{ formatReason(request.reasonCategory) }}</span>
									<span class="mx-1">|</span>
									<span>{{ formatDate(request.createdAt) }}</span>
								</div>
								<div class="mt-2 text-xs text-gray-500">
									Current: {{ request.schedule.room.buildingCode }} {{
										request.schedule.room.roomNumber }}
									<span class="mx-1">/</span>
									{{ formatTimeSlotLabel(request.schedule.timeSlot) }}
								</div>
							</div>
							<RouterLink to="/requests/admin"
								class="shrink-0 inline-flex items-center justify-center rounded border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-800 hover:bg-gray-50">
								Review
							</RouterLink>
						</li>
					</ul>

					<div v-if="pendingCount > pendingPreview.length" class="border-t border-gray-200 p-4 text-sm">
						<RouterLink to="/requests/admin" class="font-medium text-blue-700 hover:underline">
							View {{ pendingCount - pendingPreview.length }} more pending request{{ (pendingCount -
								pendingPreview.length) === 1 ? '' : 's' }}
						</RouterLink>
					</div>
				</div>

				<div class="rounded border border-gray-200 bg-white overflow-hidden">
					<div class="border-b border-gray-200 p-4">
						<h2 class="text-sm font-semibold text-gray-900">Shortcuts</h2>
						<p class="mt-0.5 text-xs text-gray-500">Common admin pages.</p>
					</div>

					<div class="divide-y divide-gray-100">
						<RouterLink v-for="item in quickLinks" :key="item.to" :to="item.to"
							class="block p-3 hover:bg-gray-50 transition-colors">
							<div class="flex items-center justify-between gap-3">
								<div class="text-sm font-medium text-gray-900">{{ item.label }}</div>
								<div class="text-xs text-gray-500">Open</div>
							</div>
							<div class="mt-1 text-xs text-gray-500">{{ item.description }}</div>
						</RouterLink>
					</div>

					<div v-if="statsError" class="border-t border-gray-200 p-4">
						<div class="text-sm text-red-700">{{ statsError }}</div>
						<button @click="fetchStats" class="mt-2 text-sm font-medium text-blue-700 hover:underline">
							Retry
						</button>
					</div>
				</div>
			</div>
		</section>

		<!-- Fallback (non-admin for now) -->
		<section v-else class="space-y-4">
			<h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>

			<div class="grid grid-cols-2 md:grid-cols-3 gap-4">
				<RouterLink to="/schedules" class="block p-4 border hover:bg-gray-50 bg-white rounded">
					<div class="font-medium">Schedules</div>
					<div class="text-sm text-gray-500">View your class schedule</div>
				</RouterLink>
				<RouterLink to="/requests" class="block p-4 border hover:bg-gray-50 bg-white rounded">
					<div class="font-medium">Requests</div>
					<div class="text-sm text-gray-500">Submit or track change requests</div>
				</RouterLink>
			</div>
		</section>
	</div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRole } from '@/composables/useRole'
import { useAsyncData } from '@/composables/useAsyncData'
import { generatorService, type UniversityStats } from '@/services/generator'
import { changeRequestsService, type ScheduleChangeRequest } from '@/services/changeRequests'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'

type Kpi = {
	label: string
	value: number | string
	sublabel?: string
	to: string
}

const { isAdmin } = useRole()

const {
	data: stats,
	loading: statsLoading,
	error: statsError,
	execute: fetchStats,
} = useAsyncData<UniversityStats>(() => generatorService.getStats(), { immediate: false })

const {
	data: pendingRequests,
	loading: pendingLoading,
	error: pendingError,
	execute: fetchPending,
} = useAsyncData<ScheduleChangeRequest[]>(
	() => changeRequestsService.getAll({ status: 'PENDING' }),
	{ immediate: false }
)

const pendingCount = computed(() => pendingRequests.value?.length ?? 0)
const pendingPreview = computed(() => (pendingRequests.value ?? []).slice(0, 6))

const hasNoData = computed(() => {
	if (!stats.value) return false
	return stats.value.buildings === 0 && stats.value.rooms === 0 && stats.value.courses === 0
})

const kpis = computed<Kpi[]>(() => {
	const s = stats.value
	return [
		{ label: 'Buildings', value: s?.buildings ?? 0, sublabel: 'Inventory', to: '/buildings' },
		{ label: 'Rooms', value: s?.rooms ?? 0, sublabel: 'Spaces', to: '/rooms' },
		{ label: 'Courses', value: s?.courses ?? 0, sublabel: 'Offerings', to: '/courses' },
		{ label: 'Instructors', value: s?.instructors ?? 0, sublabel: 'Staff', to: '/instructors' },
		{ label: 'Schedules', value: s?.schedules ?? 0, sublabel: 'Booked', to: '/schedules' },
		{ label: 'Pending', value: pendingCount.value, sublabel: 'Requests', to: '/requests/admin' },
	]
})

const quickLinks = computed(() => {
	return [
		{
			to: '/requests/admin',
			label: 'Change Requests',
			description: 'Review and approve instructor requests.',
		},
		{
			to: '/schedules',
			label: 'Schedules',
			description: 'Browse and manage all scheduled classes.',
		},
		{
			to: '/solver',
			label: 'Solver',
			description: 'Generate optimized schedules and demo data.',
		},
		{
			to: '/analytics',
			label: 'Analytics',
			description: 'Room utilization and peak usage patterns.',
		},
	]
})

const isRefreshing = computed(() => statsLoading.value || pendingLoading.value)

async function refresh() {
	await Promise.allSettled([fetchStats(), fetchPending()])
}

function formatDate(value: string): string {
	const date = new Date(value)
	if (Number.isNaN(date.getTime())) return value
	return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
}

function formatReason(value: string): string {
	const words = value
		.replace(/_/g, ' ')
		.toLowerCase()
		.split(' ')
		.filter(Boolean)
	return words.map(w => w.slice(0, 1).toUpperCase() + w.slice(1)).join(' ')
}

function formatTimeSlotLabel(slot: TimeSlot): string {
	return timeslotsService.formatTimeSlot(slot)
}

watch(
	isAdmin,
	(value) => {
		if (!value) return
		void refresh()
	},
	{ immediate: true }
)
</script>
