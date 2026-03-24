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
							class="ml-2 inline-flex items-center rounded bg-blue-700 px-2 py-0.5 text-xs font-semibold text-white">
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

		<!-- Instructor dashboard -->
		<section v-else-if="isInstructor" class="space-y-6">
			<div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
				<div>
					<h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
					<p class="mt-1 text-sm text-gray-600">
						Instructor overview.
						<span v-if="instructorLabel" class="text-gray-500">
							<span class="mx-1">|</span>
							{{ instructorLabel }}
						</span>
						<span v-if="myRequestCounts.pending > 0" class="text-gray-500">
							<span class="mx-1">|</span>
							{{ myRequestCounts.pending }} pending request{{ myRequestCounts.pending === 1 ? '' : 's' }}
						</span>
					</p>
				</div>

				<div class="flex flex-wrap gap-2">
					<RouterLink to="/requests/new"
						class="inline-flex items-center justify-center rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
						Request Change
					</RouterLink>
					<RouterLink to="/schedules"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						My Schedule
					</RouterLink>
					<RouterLink to="/requests"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						Requests
					</RouterLink>
					<button v-if="INSTRUCTOR_FRICTION_MVP" @click="teachingPrefsOpen = true"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50">
						Class Preferences
					</button>
					<button @click="refreshInstructor" :disabled="instructorRefreshing"
						class="inline-flex items-center justify-center rounded border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-800 hover:bg-gray-50 disabled:opacity-50">
						{{ instructorRefreshing ? 'Refreshing...' : 'Refresh' }}
					</button>
				</div>
			</div>

			<div v-if="!instructorId" class="rounded border border-gray-200 bg-white p-6">
				<div class="text-sm font-medium text-gray-900">Select an instructor</div>
				<p class="mt-1 text-sm text-gray-600">
					Use the instructor selector in the top bar to load your schedule and requests.
				</p>
			</div>

			<template v-else>
				<!-- KPIs -->
				<div class="grid grid-cols-2 md:grid-cols-4 gap-4">
					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Classes</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
							<span v-if="mySchedulesLoading && mySchedules === null"
								class="inline-block h-7 w-10 rounded bg-gray-200 animate-pulse"></span>
							<span v-else>{{ mySchedules?.length ?? 0 }}</span>
						</div>
						<div class="mt-1 text-xs text-gray-500">Scheduled per week</div>
					</div>

					<RouterLink to="/requests"
						class="block rounded border border-gray-200 bg-white p-4 hover:border-gray-300 hover:bg-gray-50 transition-colors">
						<div class="text-sm font-medium text-gray-900">Pending</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
							<span v-if="myRequestsLoading && myRequests === null"
								class="inline-block h-7 w-10 rounded bg-gray-200 animate-pulse"></span>
							<span v-else>{{ myRequestCounts.pending }}</span>
						</div>
						<div class="mt-1 text-xs text-gray-500">Requests awaiting review</div>
					</RouterLink>

					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Approved</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
							<span v-if="myRequestsLoading && myRequests === null"
								class="inline-block h-7 w-10 rounded bg-gray-200 animate-pulse"></span>
							<span v-else>{{ myRequestCounts.approved }}</span>
						</div>
						<div class="mt-1 text-xs text-gray-500">Requests accepted</div>
					</div>

					<div class="rounded border border-gray-200 bg-white p-4">
						<div class="text-sm font-medium text-gray-900">Rejected</div>
						<div class="mt-2 text-2xl font-semibold text-gray-900 tabular-nums">
							<span v-if="myRequestsLoading && myRequests === null"
								class="inline-block h-7 w-10 rounded bg-gray-200 animate-pulse"></span>
							<span v-else>{{ myRequestCounts.rejected }}</span>
						</div>
						<div class="mt-1 text-xs text-gray-500">Requests declined</div>
					</div>
				</div>

				<div v-if="INSTRUCTOR_FRICTION_MVP" class="rounded border border-gray-200 bg-white overflow-hidden">
					<div class="flex items-center justify-between gap-4 border-b border-gray-200 p-4">
						<div>
							<h2 class="text-sm font-semibold text-gray-900">Schedule Issues</h2>
							<p class="mt-0.5 text-xs text-gray-500 slate:text-gray-600">
								Found for {{ activeInstructorSemester || 'current semester' }}.
							</p>
						</div>
						<RouterLink to="/schedules" class="text-sm font-medium text-blue-700 hover:underline">
							Open schedule
						</RouterLink>
					</div>

					<div v-if="frictionsLoading" class="p-4 space-y-2">
						<div v-for="i in 3" :key="i" class="h-4 w-48 rounded bg-gray-200 animate-pulse"></div>
					</div>
					<div v-else-if="frictionsError" class="p-4">
						<div class="text-sm text-red-700">{{ frictionsError }}</div>
						<button @click="fetchFrictions" class="mt-2 text-sm font-medium text-blue-700 hover:underline">
							Retry
						</button>
					</div>
					<div v-else-if="frictions.length === 0" class="p-4 text-sm text-gray-600">
						No schedule issues right now.
					</div>
					<ul v-else class="divide-y divide-gray-100">
						<li v-for="issue in frictions.slice(0, 5)" :key="issue.id" class="p-4 flex items-start gap-4">
							<div class="flex-1 min-w-0">
								<div class="flex flex-wrap items-center gap-2">
									<span class="text-xs px-2 py-0.5 rounded font-medium"
										:class="frictionSeverityClass(issue.severity)">
										{{ issue.severity }}
									</span>
									<span class="text-xs text-gray-500 slate:text-gray-600">{{
										formatFrictionType(issue.type) }}</span>
								</div>
								<p class="mt-2 text-sm text-gray-700">{{ issue.message }}</p>
							</div>
							<RouterLink
								:to="{ path: '/requests/new', query: { scheduleId: String(issue.scheduleId), issue: issue.recommendedIssue } }"
								class="shrink-0 inline-flex items-center justify-center rounded border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-800 hover:bg-gray-50">
								Fix
							</RouterLink>
						</li>
					</ul>
				</div>

				<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
					<!-- Schedule preview -->
					<div class="lg:col-span-2 rounded border border-gray-200 bg-white overflow-hidden">
						<div class="flex items-center justify-between gap-4 border-b border-gray-200 p-4">
							<div>
								<h2 class="text-sm font-semibold text-gray-900">My Schedule</h2>
								<p class="mt-0.5 text-xs text-gray-500">Weekly view (sorted by day and start time).</p>
							</div>
							<div class="flex items-center gap-2">
								<button @click="handleExportFullSemester" :disabled="!mySchedules?.length"
									v-tooltip="'Download your full class schedule as an .ics file'"
									class="inline-flex items-center gap-1 rounded border border-gray-300 bg-white px-2.5 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40">
									<svg xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5 text-gray-500"
										viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
										<path fill-rule="evenodd"
											d="M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z"
											clip-rule="evenodd" />
									</svg>
									Export to iCal
								</button>
								<RouterLink to="/schedules"
									class="inline-flex items-center rounded border border-gray-300 bg-white px-2.5 py-1 text-xs font-medium text-gray-700 hover:bg-gray-50">
									Open schedule
								</RouterLink>
							</div>
						</div>

						<div v-if="mySchedulesLoading && mySchedules === null" class="p-4 space-y-3">
							<div v-for="i in 5" :key="i" class="animate-pulse">
								<div class="h-4 w-56 rounded bg-gray-200"></div>
								<div class="mt-2 h-3 w-80 rounded bg-gray-100"></div>
							</div>
						</div>

						<div v-else-if="mySchedulesError" class="p-4">
							<div class="text-sm text-red-700">{{ mySchedulesError }}</div>
							<button @click="fetchMySchedules"
								class="mt-2 text-sm font-medium text-blue-700 hover:underline">
								Retry
							</button>
						</div>

						<div v-else-if="mySchedulePreview.length === 0" class="p-6">
							<div class="text-sm font-medium text-gray-900">No schedules assigned</div>
							<p class="mt-1 text-sm text-gray-600">
								Once you’re assigned to courses, they’ll show up here.
							</p>
						</div>

						<ul v-else class="divide-y divide-gray-100">
							<li v-for="schedule in mySchedulePreview" :key="schedule.id"
								class="p-4 flex items-start gap-4">
								<div class="flex-1 min-w-0">
									<div class="flex flex-wrap items-center gap-x-2 gap-y-1">
										<span class="font-semibold text-gray-900">{{ schedule.course.code }}</span>
										<span class="text-sm text-gray-600 truncate">{{ schedule.course.name }}</span>
									</div>
									<div class="mt-1 text-xs text-gray-500">
										<span>{{ formatTimeSlotLabel(schedule.timeSlot) }}</span>
										<span class="mx-1">|</span>
										<span>{{ schedule.room.buildingCode }} {{ schedule.room.roomNumber }}</span>
										<template v-if="showInstructorSemester">
											<span class="mx-1">|</span>
											<span>{{ schedule.semester }}</span>
										</template>
									</div>
								</div>
								<RouterLink to="/schedules"
									class="shrink-0 inline-flex items-center justify-center rounded border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-800 hover:bg-gray-50">
									View
								</RouterLink>
							</li>
						</ul>

						<div v-if="mySchedules && mySchedules.length > mySchedulePreview.length"
							class="border-t border-gray-200 p-4 text-sm">
							<RouterLink to="/schedules" class="font-medium text-blue-700 hover:underline">
								View {{ mySchedules.length - mySchedulePreview.length }} more class{{
									(mySchedules.length -
										mySchedulePreview.length) === 1 ? '' : 'es' }}
							</RouterLink>
						</div>
					</div>

					<!-- Requests preview -->
					<div class="rounded border border-gray-200 bg-white overflow-hidden">
						<div class="flex items-center justify-between gap-4 border-b border-gray-200 p-4">
							<div>
								<h2 class="text-sm font-semibold text-gray-900">My Requests</h2>
								<p class="mt-0.5 text-xs text-gray-500">
									Pending: {{ myRequestCounts.pending }} | Approved: {{ myRequestCounts.approved }} |
									Rejected: {{
										myRequestCounts.rejected }}
								</p>
							</div>
							<RouterLink to="/requests" class="text-sm font-medium text-blue-700 hover:underline">
								View all
							</RouterLink>
						</div>

						<div v-if="myRequestsLoading && myRequests === null" class="p-4 space-y-3">
							<div v-for="i in 5" :key="i" class="animate-pulse">
								<div class="h-4 w-40 rounded bg-gray-200"></div>
								<div class="mt-2 h-3 w-60 rounded bg-gray-100"></div>
							</div>
						</div>

						<div v-else-if="myRequestsError" class="p-4">
							<div class="text-sm text-red-700">{{ myRequestsError }}</div>
							<button @click="fetchMyRequests"
								class="mt-2 text-sm font-medium text-blue-700 hover:underline">
								Retry
							</button>
						</div>

						<div v-else-if="recentMyRequests.length === 0" class="p-6">
							<div class="text-sm font-medium text-gray-900">No requests yet</div>
							<p class="mt-1 text-sm text-gray-600">
								Submit a change request if something needs to move.
							</p>
							<RouterLink to="/requests/new"
								class="mt-3 inline-flex items-center justify-center rounded bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700">
								Request change
							</RouterLink>
						</div>

						<ul v-else class="divide-y divide-gray-100">
							<li v-for="request in recentMyRequests" :key="request.id" class="p-3">
								<div class="flex items-start justify-between gap-3">
									<div class="min-w-0">
										<div class="text-sm font-medium text-gray-900">
											{{ request.schedule.course.code }}
										</div>
										<div class="mt-0.5 text-xs text-gray-500 truncate">
											{{ formatReason(request.reasonCategory) }}
										</div>
									</div>
									<div class="text-right">
										<div class="text-xs font-medium text-gray-700">{{ request.status }}</div>
										<div class="mt-0.5 text-xs text-gray-500">{{ formatDate(request.createdAt) }}
										</div>
									</div>
								</div>
							</li>
						</ul>

						<div v-if="recentResolvedRequests.length" class="border-t border-gray-200 p-3">
							<div class="text-xs font-semibold uppercase tracking-wide text-gray-500 mb-2">Recent
								Decisions
							</div>
							<ul class="space-y-2">
								<li v-for="request in recentResolvedRequests" :key="`resolved-${request.id}`"
									class="text-xs text-gray-600">
									<span class="font-medium text-gray-800">{{ request.schedule.course.code }}</span>
									<span class="mx-1">-</span>
									<span>{{ request.status }}</span>
									<span v-if="request.decisionNote" class="mx-1">|</span>
									<span v-if="request.decisionNote" class="text-gray-500 truncate">{{
										request.decisionNote
									}}</span>
								</li>
							</ul>
						</div>
					</div>
				</div>
			</template>

			<BaseModal v-if="INSTRUCTOR_FRICTION_MVP" :model-value="teachingPrefsOpen" title="Class Preferences"
				@update:model-value="teachingPrefsOpen = $event">
				<div class="space-y-4">
					<div v-if="teachingPrefsError" class="text-sm text-red-700">{{ teachingPrefsError }}</div>
					<div v-if="teachingPrefsLoading" class="text-sm text-gray-500">Loading your preferences...</div>
					<template v-else>
						<div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
							<div>
								<label for="pref-start" class="block text-sm font-medium text-gray-700 mb-1">Earliest
									class start</label>
								<input id="pref-start" v-model="teachingPrefsForm.preferredStartTime" type="time"
									class="w-full px-3 py-2 border border-gray-300 rounded" />
							</div>
							<div>
								<label for="pref-end" class="block text-sm font-medium text-gray-700 mb-1">Latest class
									end</label>
								<input id="pref-end" v-model="teachingPrefsForm.preferredEndTime" type="time"
									class="w-full px-3 py-2 border border-gray-300 rounded" />
							</div>
						</div>

						<div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
							<div>
								<label for="pref-max-gap" class="block text-sm font-medium text-gray-700 mb-1">Longest
									break between classes (minutes)</label>
								<input id="pref-max-gap" v-model.number="teachingPrefsForm.maxGapMinutes" type="number"
									min="0" max="360" class="w-full px-3 py-2 border border-gray-300 rounded" />
							</div>
							<div>
								<label for="pref-travel" class="block text-sm font-medium text-gray-700 mb-1">Travel
									time between classes (minutes)</label>
								<input id="pref-travel" v-model.number="teachingPrefsForm.minTravelBufferMinutes"
									type="number" min="0" max="60"
									class="w-full px-3 py-2 border border-gray-300 rounded" />
							</div>
						</div>

						<label class="flex items-start gap-2">
							<input type="checkbox" v-model="teachingPrefsForm.avoidBuildingHops" class="mt-1" />
							<span class="text-sm text-gray-700">Avoid back-to-back classes in different buildings</span>
						</label>

						<div>
							<div class="flex items-center justify-between gap-3 mb-1">
								<div class="text-sm font-medium text-gray-700">Preferred buildings</div>
								<button type="button"
									class="text-xs text-blue-700 hover:text-blue-800 disabled:text-gray-400"
									:disabled="teachingPrefsForm.preferredBuildingIds.length === 0"
									@click="clearPreferredBuildings">
									Clear all
								</button>
							</div>
							<p class="text-xs text-gray-500 mb-2">Select buildings you want prioritized.</p>
							<div class="border border-gray-200 rounded p-3">
								<div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
									<label v-for="building in buildings ?? []" :key="building.id"
										class="flex items-start gap-2 text-sm text-gray-700">
										<input type="checkbox" :value="building.id"
											v-model="teachingPrefsForm.preferredBuildingIds" />
										<span>{{ building.code }} - {{ building.name }}</span>
									</label>
								</div>
							</div>
						</div>

						<div>
							<div class="flex items-center justify-between gap-3 mb-1">
								<div class="text-sm font-medium text-gray-700">Room must-haves</div>
								<button type="button"
									class="text-xs text-blue-700 hover:text-blue-800 disabled:text-gray-400"
									:disabled="teachingPrefsForm.requiredRoomFeatures.length === 0"
									@click="clearRequiredRoomFeatures">
									Clear all
								</button>
							</div>
							<p class="text-xs text-gray-500 mb-2">Select the setup your class needs.</p>

							<div v-if="roomFeatureOptionsLoading" class="text-sm text-gray-500">Loading room options...
							</div>
							<div v-else-if="roomFeatureOptionsError" class="text-sm text-red-700">
								Could not load room must-have options right now.
							</div>
							<div v-else class="border border-gray-200 rounded p-3">
								<div class="space-y-3">
									<div v-for="group in roomFeatureGroups" :key="group.category" class="space-y-2">
										<p class="text-xs font-semibold uppercase tracking-wide text-gray-500">{{
											group.category }}</p>
										<div class="grid grid-cols-1 sm:grid-cols-2 gap-2">
											<label v-for="option in group.options" :key="option.value"
												:for="`pref-feature-${toFeatureId(option.value)}`"
												class="flex items-start gap-2 text-sm text-gray-700">
												<input :id="`pref-feature-${toFeatureId(option.value)}`" type="checkbox"
													:value="option.value"
													v-model="teachingPrefsForm.requiredRoomFeatures" />
												<span>{{ option.label }}</span>
											</label>
										</div>
									</div>
								</div>
							</div>
						</div>
					</template>
				</div>
				<template #footer>
					<div class="flex justify-end gap-2">
						<button class="px-4 py-2 border border-gray-300 rounded" @click="teachingPrefsOpen = false">
							Cancel
						</button>
						<button :disabled="teachingPrefsLoading || teachingPrefsSaving"
							class="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
							@click="saveTeachingPreferences">
							{{ teachingPrefsSaving ? 'Saving...' : 'Save' }}
						</button>
					</div>
				</template>
			</BaseModal>
		</section>

		<!-- Student dashboard shell -->
		<section v-else-if="isStudent" class="space-y-6">
			<div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
				<div>
					<h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
					<p class="mt-1 text-sm text-gray-600">
						Read-only student shell.
						<span v-if="studentLabel" class="text-gray-500">
							<span class="mx-1">|</span>
							{{ studentLabel }}
						</span>
					</p>
				</div>
			</div>

			<div class="rounded border border-gray-200 bg-white p-6">
				<div v-if="!studentId" class="space-y-1">
					<div class="text-sm font-medium text-gray-900">Select a student</div>
					<p class="text-sm text-gray-600">
						Use the student selector in the top bar to choose which generated student you want to inspect.
					</p>
				</div>
				<div v-else class="space-y-1">
					<div class="text-sm font-medium text-gray-900">Student workspace is ready</div>
					<p class="text-sm text-gray-600">
						Role state, selector persistence, and route restrictions are active. Student schedule views land in
						the next feature slice.
					</p>
				</div>
			</div>
		</section>

		<!-- Fallback (unknown role) -->
		<section v-else class="space-y-4">
			<h1 class="text-2xl font-semibold text-gray-900">Dashboard</h1>
			<p class="text-sm text-gray-600">Use the sidebar to navigate.</p>
		</section>
	</div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRole } from '@/composables/useRole'
import { useAsyncData } from '@/composables/useAsyncData'
import { generatorService, type UniversityStats } from '@/services/generator'
import { changeRequestsService, type ScheduleChangeRequest } from '@/services/changeRequests'
import { schedulesService, type Schedule } from '@/services/schedules'
import { timeslotsService, type TimeSlot } from '@/services/timeslots'
import { useInstructors } from '@/composables/useInstructors'
import { useStudents } from '@/composables/useStudents'
import { exportFullSemester } from '@/utils/icalExport'
import BaseModal from '@/components/common/BaseModal.vue'
import { buildingsService, type Building } from '@/services/buildings'
import {
	instructorPreferencesService,
	type RoomFeatureOption,
	type InstructorPreferenceUpdateRequest,
} from '@/services/instructorPreferences'
import {
	instructorInsightsService,
	type InstructorFrictionIssue,
} from '@/services/instructorInsights'
import { INSTRUCTOR_FRICTION_MVP } from '@/config/features'
import { formatFrictionType, frictionSeverityClass } from '@/utils/friction'

type Kpi = {
	label: string
	value: number | string
	sublabel?: string
	to: string
}

const { isAdmin, isInstructor, isStudent, instructorId, studentId } = useRole()
const { instructors } = useInstructors()
const { students } = useStudents()

const teachingPrefsOpen = ref(false)
const teachingPrefsLoading = ref(false)
const teachingPrefsSaving = ref(false)
const teachingPrefsError = ref<string | null>(null)

const teachingPrefsForm = ref<InstructorPreferenceUpdateRequest>({
	preferredStartTime: '08:00',
	preferredEndTime: '18:00',
	maxGapMinutes: 120,
	minTravelBufferMinutes: 15,
	avoidBuildingHops: true,
	preferredBuildingIds: [],
	requiredRoomFeatures: [],
})

const frictions = ref<InstructorFrictionIssue[]>([])
const frictionsLoading = ref(false)
const frictionsError = ref<string | null>(null)

const {
	data: buildings,
	execute: fetchBuildings,
} = useAsyncData<Building[]>(() => buildingsService.getAll(), { immediate: false })

const {
	data: roomFeatureOptions,
	loading: roomFeatureOptionsLoading,
	error: roomFeatureOptionsError,
	execute: fetchRoomFeatureOptions,
} = useAsyncData<RoomFeatureOption[]>(
	() => instructorPreferencesService.getRoomFeatureOptions(),
	{ immediate: false }
)

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

type RoomFeatureGroup = {
	category: string
	options: RoomFeatureOption[]
}

const roomFeatureGroups = computed<RoomFeatureGroup[]>(() => {
	const groups = new Map<string, RoomFeatureOption[]>()
	for (const option of roomFeatureOptions.value ?? []) {
		const items = groups.get(option.category) ?? []
		items.push(option)
		groups.set(option.category, items)
	}

	return Array.from(groups.entries()).map(([category, options]) => ({ category, options }))
})

const instructorLabel = computed(() => {
	if (!instructorId.value) return ''
	const match = instructors.value.find(i => i.id === instructorId.value)
	if (!match) return `Instructor #${instructorId.value}`
	return `${match.firstName} ${match.lastName}`.trim()
})

const studentLabel = computed(() => {
	if (!studentId.value) return ''
	const match = students.value.find(student => student.id === studentId.value)
	if (!match) return `Student #${studentId.value}`
	return `${match.firstName} ${match.lastName}`.trim()
})

const {
	data: mySchedules,
	loading: mySchedulesLoading,
	error: mySchedulesError,
	execute: fetchMySchedules,
	reset: resetMySchedules,
} = useAsyncData<Schedule[]>(
	() => (instructorId.value ? schedulesService.getAll({ instructorId: instructorId.value }) : Promise.resolve([])),
	{ immediate: false }
)

const {
	data: myRequests,
	loading: myRequestsLoading,
	error: myRequestsError,
	execute: fetchMyRequests,
	reset: resetMyRequests,
} = useAsyncData<ScheduleChangeRequest[]>(
	() => (instructorId.value ? changeRequestsService.getAll({ instructorId: instructorId.value }) : Promise.resolve([])),
	{ immediate: false }
)

const dayRank: Record<string, number> = {
	MONDAY: 1,
	TUESDAY: 2,
	WEDNESDAY: 3,
	THURSDAY: 4,
	FRIDAY: 5,
	SATURDAY: 6,
	SUNDAY: 7,
}

const mySchedulePreview = computed(() => {
	const items = mySchedules.value ?? []
	return [...items]
		.sort((a, b) => {
			const aRank = dayRank[a.timeSlot.dayOfWeek] ?? 99
			const bRank = dayRank[b.timeSlot.dayOfWeek] ?? 99
			if (aRank !== bRank) return aRank - bRank
			return String(a.timeSlot.startTime).localeCompare(String(b.timeSlot.startTime))
		})
		.slice(0, 7)
})

const showInstructorSemester = computed(() => {
	const items = mySchedules.value ?? []
	return new Set(items.map(item => item.semester)).size > 1
})

const myRequestCounts = computed(() => {
	const items = myRequests.value ?? []
	let pending = 0
	let approved = 0
	let rejected = 0
	for (const item of items) {
		if (item.status === 'PENDING') pending++
		else if (item.status === 'APPROVED') approved++
		else if (item.status === 'REJECTED') rejected++
	}
	return { pending, approved, rejected }
})

const recentMyRequests = computed(() => {
	const items = myRequests.value ?? []
	return [...items]
		.sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))
		.slice(0, 6)
})

const recentResolvedRequests = computed(() => {
	const items = myRequests.value ?? []
	return [...items]
		.filter(item => item.status !== 'PENDING')
		.sort((a, b) => String(b.reviewedAt ?? b.createdAt).localeCompare(String(a.reviewedAt ?? a.createdAt)))
		.slice(0, 4)
})

function parseSemesterForSort(semester: string): { year: number; termRank: number } {
	const match = semester.match(/([A-Za-z]+)\s+(\d{4})/)
	if (!match) return { year: 0, termRank: 0 }

	const term = (match[1] ?? '').toUpperCase()
	const year = Number(match[2] ?? 0)
	const termOrder: Record<string, number> = {
		WINTER: 1,
		SPRING: 2,
		SUMMER: 3,
		FALL: 4,
	}

	return {
		year: Number.isNaN(year) ? 0 : year,
		termRank: termOrder[term] ?? 0,
	}
}

const activeInstructorSemester = computed(() => {
	const uniqueSemesters = Array.from(new Set((mySchedules.value ?? []).map(item => item.semester)))
	uniqueSemesters.sort((a, b) => {
		const parsedA = parseSemesterForSort(a)
		const parsedB = parseSemesterForSort(b)
		if (parsedA.year !== parsedB.year) return parsedB.year - parsedA.year
		if (parsedA.termRank !== parsedB.termRank) return parsedB.termRank - parsedA.termRank
		return a.localeCompare(b)
	})
	return uniqueSemesters[0] ?? ''
})

const isRefreshing = computed(() => statsLoading.value || pendingLoading.value)
const instructorRefreshing = computed(() => mySchedulesLoading.value || myRequestsLoading.value)

async function refresh() {
	await Promise.allSettled([fetchStats(), fetchPending()])
}

async function refreshInstructor() {
	if (!instructorId.value) return
	await Promise.allSettled([fetchMySchedules(), fetchMyRequests()])
	if (INSTRUCTOR_FRICTION_MVP) {
		await fetchFrictions()
	}
}

function handleExportFullSemester() {
	if (!mySchedules.value?.length) return
	exportFullSemester(mySchedules.value)
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

function toFeatureId(value: string): string {
	return value.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
}

function clearPreferredBuildings() {
	teachingPrefsForm.value.preferredBuildingIds = []
}

function clearRequiredRoomFeatures() {
	teachingPrefsForm.value.requiredRoomFeatures = []
}

async function loadTeachingPreferences() {
	if (!instructorId.value || !INSTRUCTOR_FRICTION_MVP) return
	teachingPrefsLoading.value = true
	teachingPrefsError.value = null
	try {
		const prefs = await instructorPreferencesService.getByInstructorId(instructorId.value)
		teachingPrefsForm.value = {
			preferredStartTime: prefs.preferredStartTime,
			preferredEndTime: prefs.preferredEndTime,
			maxGapMinutes: prefs.maxGapMinutes,
			minTravelBufferMinutes: prefs.minTravelBufferMinutes,
			avoidBuildingHops: prefs.avoidBuildingHops,
			preferredBuildingIds: prefs.preferredBuildingIds,
			requiredRoomFeatures: prefs.requiredRoomFeatures,
		}
	} catch (error) {
		console.error(error)
		teachingPrefsError.value = 'Could not load your class preferences'
	} finally {
		teachingPrefsLoading.value = false
	}
}

async function saveTeachingPreferences() {
	if (!instructorId.value || !INSTRUCTOR_FRICTION_MVP) return

	teachingPrefsError.value = null
	const start = teachingPrefsForm.value.preferredStartTime
	const end = teachingPrefsForm.value.preferredEndTime
	if (start && end && start >= end) {
		teachingPrefsError.value = 'Start time should be earlier than end time'
		return
	}

	teachingPrefsSaving.value = true
	try {
		const payload: InstructorPreferenceUpdateRequest = {
			...teachingPrefsForm.value,
			preferredBuildingIds: [...new Set(teachingPrefsForm.value.preferredBuildingIds)]
				.map(value => Number(value))
				.filter(value => Number.isFinite(value) && value > 0),
			requiredRoomFeatures: [...new Set(teachingPrefsForm.value.requiredRoomFeatures)],
		}
		await instructorPreferencesService.upsert(instructorId.value, payload)
		await loadTeachingPreferences()
		await fetchFrictions()
		teachingPrefsOpen.value = false
	} catch (error: any) {
		console.error(error)
		teachingPrefsError.value = error?.response?.data?.error || 'Could not save your class preferences'
	} finally {
		teachingPrefsSaving.value = false
	}
}

async function fetchFrictions() {
	if (!INSTRUCTOR_FRICTION_MVP) return
	if (!instructorId.value || !activeInstructorSemester.value) {
		frictions.value = []
		frictionsError.value = null
		return
	}

	frictionsLoading.value = true
	frictionsError.value = null
	try {
		frictions.value = await instructorInsightsService.getFrictions(
			instructorId.value,
			activeInstructorSemester.value,
		)
	} catch (error) {
		console.error(error)
		frictionsError.value = 'Could not load schedule issues'
		frictions.value = []
	} finally {
		frictionsLoading.value = false
	}
}

watch(
	isAdmin,
	(value) => {
		if (!value) return
		void refresh()
	},
	{ immediate: true }
)

watch(
	[isInstructor, instructorId],
	([enabled, nextInstructorId], prev = [false, null]) => {
		const [prevEnabled, prevInstructorId] = prev
		if (!enabled) {
			if (prevEnabled) {
				resetMySchedules()
				resetMyRequests()
			}
			frictions.value = []
			teachingPrefsOpen.value = false
			return
		}
		if (!nextInstructorId) {
			resetMySchedules()
			resetMyRequests()
			frictions.value = []
			return
		}
		if (nextInstructorId !== prevInstructorId) {
			resetMySchedules()
			resetMyRequests()
			frictions.value = []
			if (INSTRUCTOR_FRICTION_MVP) {
				void loadTeachingPreferences()
			}
		}
		if (INSTRUCTOR_FRICTION_MVP) {
			void fetchBuildings()
			void fetchRoomFeatureOptions()
		}
		void refreshInstructor()
	},
	{ immediate: true }
)

watch(teachingPrefsOpen, (open) => {
	if (!open || !INSTRUCTOR_FRICTION_MVP) return
	if ((roomFeatureOptions.value ?? []).length > 0) return
	void fetchRoomFeatureOptions()
})

watch(
	[activeInstructorSemester, instructorId],
	([semester, nextInstructorId]) => {
		if (!INSTRUCTOR_FRICTION_MVP || !nextInstructorId || !semester) {
			return
		}
		void fetchFrictions()
	},
	{ immediate: true }
)
</script>
