<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { timeslotsService, type CreateTimeSlotRequest, DAY_OF_WEEK_OPTIONS } from '@/services/timeslots'
import FormSkeleton from '@/components/common/FormSkeleton.vue'
import type { AxiosError } from 'axios'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => route.params.id !== undefined)
const timeslotId = computed(() => Number(route.params.id))

const form = ref<CreateTimeSlotRequest>({ dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00', label: '' })
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
	if (isEdit.value) {
		loading.value = true
		try {
			const ts = await timeslotsService.getById(timeslotId.value)
			form.value = { dayOfWeek: ts.dayOfWeek, startTime: ts.startTime, endTime: ts.endTime, label: ts.label || '' }
		} catch {
			error.value = 'Failed to load time slot'
		} finally {
			loading.value = false
		}
	}
})

async function handleSubmit() {
	if (form.value.startTime >= form.value.endTime) { error.value = 'Start must be before end'; return }
	saving.value = true
	error.value = null
	try {
		if (isEdit.value) await timeslotsService.update(timeslotId.value, form.value)
		else await timeslotsService.create(form.value)
		router.push('/timeslots')
	} catch (e) { error.value = (e as AxiosError<{ message?: string }>).response?.data?.message || 'Failed to save time slot' }
	finally { saving.value = false }
}
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink to="/timeslots" class="text-blue-600 hover:underline text-sm">Back to Time Slots</RouterLink>
		</div>
		<div class="bg-white border border-gray-200 p-6 max-w-xl">
			<h1 class="text-2xl font-semibold text-gray-900 mb-6">{{ isEdit ? 'Edit Time Slot' : 'New Time Slot' }}</h1>
			<FormSkeleton v-if="loading" :fields="4" />
			<form v-else @submit.prevent="handleSubmit" class="space-y-4">
				<div v-if="error" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded">{{ error }}</div>
				<div class="grid grid-cols-2 gap-4">
					<div class="col-span-2">
						<label class="block text-sm font-medium text-gray-700 mb-1">Day <span
								class="text-red-500">*</span></label>
						<select v-model="form.dayOfWeek" required aria-label="Day of Week"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
							<option v-for="d in DAY_OF_WEEK_OPTIONS" :key="d.value" :value="d.value">{{ d.label }}
							</option>
						</select>
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Start <span
								class="text-red-500">*</span></label>
						<input v-model="form.startTime" type="time" required
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">End <span
								class="text-red-500">*</span></label>
						<input v-model="form.endTime" type="time" required
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div class="col-span-2">
						<label class="block text-sm font-medium text-gray-700 mb-1">Label</label>
						<input v-model="form.label" type="text" maxlength="50"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500"
							placeholder="e.g., Period 1" />
					</div>
				</div>
				<div class="flex gap-4 pt-4">
					<button type="submit" :disabled="saving"
						class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">{{ saving
							? 'Saving...' : (isEdit ? 'Update' : 'Create') }}</button>
					<RouterLink to="/timeslots" class="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">Cancel
					</RouterLink>
				</div>
			</form>
		</div>
	</div>
</template>
