<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRole } from '@/composables/useRole'
import { changeRequestsService, type ScheduleChangeRequest } from '@/services/changeRequests'
import EmptyState from '@/components/common/EmptyState.vue'
import TableSkeleton from '@/components/common/TableSkeleton.vue'

const { instructorId } = useRole()
const loading = ref(false)
const error = ref<string | null>(null)
const items = ref<ScheduleChangeRequest[]>([])

async function loadData() {
	if (!instructorId.value) {
		items.value = []
		return
	}
	loading.value = true
	error.value = null
	try {
		items.value = await changeRequestsService.getAll({ instructorId: instructorId.value })
	} catch (e) {
		console.error(e)
		error.value = 'Failed to load requests'
	} finally {
		loading.value = false
	}
}

watch(instructorId, loadData)

onMounted(loadData)
</script>

<template>
	<div>
		<div class="flex justify-between items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">My Requests</h1>
			<RouterLink to="/requests/new" class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
				Request Change
			</RouterLink>
		</div>

		<TableSkeleton v-if="loading" :columns="5" :rows="6" />
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<EmptyState v-else-if="items.length === 0" title="No requests yet"
			description="Submit a change request to adjust your schedule." action-label="Request Change"
			action-route="/requests/new" />

		<div v-else class="overflow-x-auto">
			<table class="w-full bg-white border border-gray-200">
				<thead>
					<tr class="bg-gray-50 border-b border-gray-200">
						<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Course</th>
						<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Requested</th>
						<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Reason</th>
						<th class="text-left px-4 py-3 text-sm font-medium text-gray-700">Status</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="request in items" :key="request.id" class="border-b border-gray-100">
						<td class="px-4 py-3">
							<div class="text-sm text-gray-900">{{ request.schedule.course.code }}</div>
							<div class="text-xs text-gray-500">{{ request.schedule.course.name }}</div>
						</td>
						<td class="px-4 py-3 text-sm text-gray-600">
							{{ new Date(request.createdAt).toLocaleDateString() }}
						</td>
						<td class="px-4 py-3 text-sm text-gray-600">
							{{ request.reasonCategory.replace(/_/g, ' ') }}
						</td>
						<td class="px-4 py-3 text-sm">
							<span class="px-2 py-1 rounded bg-gray-100 text-gray-700">{{ request.status }}</span>
						</td>
					</tr>
				</tbody>
			</table>
		</div>
	</div>
</template>
