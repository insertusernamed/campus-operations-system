<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRole, type Role } from '@/composables/useRole'
import { useInstructors } from '@/composables/useInstructors'
import { useTheme, type Theme } from '@/composables/useTheme'

const { role, instructorId, setRole, setInstructorId } = useRole()
const { instructors, loading: loadingInstructors, loadInstructors } = useInstructors()
const { theme, setTheme } = useTheme()

const roleModel = computed({
	get: () => role.value,
	set: value => setRole(value as Role),
})

const themeModel = computed({
	get: () => theme.value,
	set: value => setTheme(value as Theme),
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
		<RouterLink to="/" class="cursor-pointer">
			<svg class="h-12 w-auto max-w-55 shrink-0" viewBox="0 0 420 92" fill="none"
				xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Campus Operations System logo">
				<path class="logo-primary-stroke" d="M12 22V13H84V22" stroke-width="3.8" stroke-linecap="square" />
				<path class="logo-primary-stroke" d="M12 70V79H84V70" stroke-width="3.8" stroke-linecap="square" />
				<text x="48" y="61" text-anchor="middle"
					font-family="'Montserrat', 'Helvetica Neue', Helvetica, Arial, sans-serif" font-size="42"
					font-weight="700" letter-spacing="-0.7" class="logo-primary-fill">COS</text>
				<text x="266" y="39" text-anchor="middle"
					font-family="'Montserrat', 'Helvetica Neue', Helvetica, Arial, sans-serif" font-size="22"
					font-weight="500" letter-spacing="2.0" class="logo-secondary-fill">CAMPUS OPERATIONS</text>
				<text x="266" y="68" text-anchor="middle"
					font-family="'Montserrat', 'Helvetica Neue', Helvetica, Arial, sans-serif" font-size="22"
					font-weight="500" letter-spacing="3.1" class="logo-secondary-fill">SYSTEM</text>
			</svg>
		</RouterLink>
		<div class="ml-auto flex items-center gap-4 text-sm text-gray-600">
			<div class="flex items-center gap-2">
				<span class="text-xs uppercase tracking-wide text-gray-500">Theme</span>
				<select v-model="themeModel" aria-label="Theme"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700">
					<option value="snow-storm">Frost</option>
					<option value="slate">Slate</option>
				</select>
			</div>
			<div class="flex items-center gap-2">
				<span class="text-xs uppercase tracking-wide text-gray-500">Role</span>
				<select v-model="roleModel" aria-label="Role"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700">
					<option value="admin">Admin</option>
					<option value="instructor">Instructor</option>
					<!-- TODO(student-role): add Student back once it's implemented end-to-end -->
					<!-- <option value="student">Student</option> -->
				</select>
			</div>
			<div v-if="role !== 'admin'" class="flex items-center gap-2">
				<span class="text-xs uppercase tracking-wide text-gray-500">Instructor</span>
				<select v-model="instructorModel" aria-label="Instructor"
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

<style scoped>
.logo-primary-stroke {
	stroke: var(--brand-logo-primary);
}

.logo-primary-fill {
	fill: var(--brand-logo-primary);
}

.logo-secondary-fill {
	fill: var(--brand-logo-secondary);
}
</style>
