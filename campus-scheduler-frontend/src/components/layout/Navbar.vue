<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRole, type Role } from '@/composables/useRole'
import { instructorsService, type Instructor } from '@/services/instructors'

const { role, instructorId, setRole, setInstructorId } = useRole()
const instructors = ref<Instructor[]>([])
const loadingInstructors = ref(false)

const roleModel = computed({
	get: () => role.value,
	set: value => setRole(value as Role),
})

const instructorModel = computed({
	get: () => instructorId.value ?? '',
	set: value => {
		const parsed = Number(value)
		setInstructorId(Number.isNaN(parsed) ? null : parsed)
	},
})

async function loadInstructors() {
	loadingInstructors.value = true
	try {
		instructors.value = await instructorsService.getAll()
	} catch (error) {
		console.error('Failed to load instructors', error)
	} finally {
		loadingInstructors.value = false
	}
}

watch([role, instructors], () => {
	if ((role.value === 'instructor' || role.value === 'student') && !instructorId.value) {
		if (instructors.value.length > 0) {
			setInstructorId(instructors.value[0].id)
		}
	}
})

onMounted(loadInstructors)
</script>

<template>
	<header class="h-14 bg-white border-b border-gray-200 flex items-center px-6">
		<h1 class="text-lg font-semibold text-gray-900">Campus Scheduler</h1>
		<div class="ml-auto flex items-center gap-4 text-sm text-gray-600">
			<div class="flex items-center gap-2">
				<span class="text-xs uppercase tracking-wide text-gray-500">Role</span>
				<select v-model="roleModel" class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700">
					<option value="admin">Admin</option>
					<option value="instructor">Instructor</option>
					<option value="student">Student</option>
				</select>
			</div>
			<div v-if="role !== 'admin'" class="flex items-center gap-2">
				<span class="text-xs uppercase tracking-wide text-gray-500">Instructor</span>
				<select v-model="instructorModel"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700">
					<option value="" disabled>
						{{ loadingInstructors ? 'Loading...' : 'Select instructor' }}
					</option>
					<option v-for="instructor in instructors" :key="instructor.id" :value="instructor.id">
						{{ instructor.firstName }} {{ instructor.lastName }}
					</option>
				</select>
			</div>
		</div>
	</header>
</template>
