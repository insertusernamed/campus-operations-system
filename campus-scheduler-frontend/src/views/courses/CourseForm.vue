<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { coursesService, type CreateCourseRequest } from '@/services/courses'
import { instructorsService, type Instructor } from '@/services/instructors'
import FormSkeleton from '@/components/common/FormSkeleton.vue'
import type { AxiosError } from 'axios'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.params.id !== undefined)
const courseId = computed(() => Number(route.params.id))

const form = ref<CreateCourseRequest>({
	code: '',
	name: '',
	description: '',
	credits: 3,
	enrollmentCapacity: 30,
	department: '',
})

const selectedInstructorId = ref<number | null>(null)
const instructors = ref<Instructor[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
	try {
		instructors.value = await instructorsService.getAll()
	} catch {
		error.value = 'Failed to load instructors'
	}

	if (isEdit.value) {
		loading.value = true
		try {
			const course = await coursesService.getById(courseId.value)
			form.value = {
				code: course.code,
				name: course.name,
				description: course.description || '',
				credits: course.credits,
				enrollmentCapacity: course.enrollmentCapacity,
				department: course.department || '',
			}
			selectedInstructorId.value = course.instructor?.id || null
		} catch {
			error.value = 'Failed to load course'
		} finally {
			loading.value = false
		}
	}
})

async function handleSubmit() {
	saving.value = true
	error.value = null
	try {
		if (isEdit.value) {
			await coursesService.update(courseId.value, form.value)
			if (selectedInstructorId.value) {
				await coursesService.assignInstructor(courseId.value, selectedInstructorId.value)
			}
		} else {
			if (selectedInstructorId.value) {
				await coursesService.createWithInstructor(selectedInstructorId.value, form.value)
			} else {
				await coursesService.create(form.value)
			}
		}
		router.push('/courses')
	} catch (e) {
		error.value = (e as AxiosError<{ message?: string }>).response?.data?.message || 'Failed to save'
	} finally {
		saving.value = false
	}
}
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink to="/courses" class="text-blue-600 hover:underline text-sm">Back to Courses</RouterLink>
		</div>

		<div class="bg-white border border-gray-200 p-6 max-w-xl">
			<h1 class="text-2xl font-semibold text-gray-900 mb-6">{{ isEdit ? 'Edit Course' : 'New Course' }}</h1>

			<FormSkeleton v-if="loading" :fields="6" />

			<form v-else @submit.prevent="handleSubmit" class="space-y-4">
				<div v-if="error" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded">{{ error }}</div>

				<div class="grid grid-cols-2 gap-4">
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Code <span
								class="text-red-500">*</span></label>
						<input v-model="form.code" type="text" required maxlength="20"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500"
							placeholder="e.g., COMP 4431" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Name <span
								class="text-red-500">*</span></label>
						<input v-model="form.name" type="text" required maxlength="100"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Credits <span
								class="text-red-500">*</span></label>
						<input v-model.number="form.credits" type="number" required min="1" max="12"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">
							Enrollment Capacity <span class="text-red-500">*</span>
							<span
								v-tooltip="'Maximum students enrolled. The scheduler will only assign this course to rooms with capacity ≥ this number.'"
								class="ml-1 cursor-help text-gray-400 hover:text-gray-600">ⓘ</span>
						</label>
						<input v-model.number="form.enrollmentCapacity" type="number" required min="1"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Department</label>
						<input v-model="form.department" type="text" maxlength="50"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500" />
					</div>
					<div>
						<label class="block text-sm font-medium text-gray-700 mb-1">Instructor</label>
						<select v-model="selectedInstructorId"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500">
							<option :value="null">-- None --</option>
							<option v-for="i in instructors" :key="i.id" :value="i.id">{{ i.lastName }}, {{ i.firstName
							}}</option>
						</select>
					</div>
					<div class="col-span-2">
						<label class="block text-sm font-medium text-gray-700 mb-1">Description</label>
						<textarea v-model="form.description" rows="3" maxlength="500"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500"></textarea>
					</div>
				</div>

				<div class="flex gap-4 pt-4">
					<button type="submit" :disabled="saving"
						class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">{{ saving
							? 'Saving...' : (isEdit ? 'Update' : 'Create') }}</button>
					<RouterLink to="/courses" class="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">Cancel
					</RouterLink>
				</div>
			</form>
		</div>
	</div>
</template>
