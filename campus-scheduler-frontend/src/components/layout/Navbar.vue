<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRole, type Role } from '@/composables/useRole'
import { useInstructors } from '@/composables/useInstructors'

const { role, instructorId, setRole, setInstructorId } = useRole()
const { instructors, loading: loadingInstructors, loadInstructors } = useInstructors()

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

watch([role, instructors], () => {
	if (role.value === 'instructor' && !instructorId.value) {
		if (instructors.value.length > 0 && instructors.value[0]) {
			setInstructorId(instructors.value[0].id)
		}
	}
})

onMounted(() => {
	loadInstructors()
	// Listen for data regeneration events
	window.addEventListener('data-regenerated', loadInstructors)
})
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
					<!-- TODO(student-role): add Student back once it's implemented end-to-end -->
					<!-- <option value="student">Student</option> -->
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
