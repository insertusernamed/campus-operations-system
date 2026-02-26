<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRole, type Role } from '@/composables/useRole'
import { useInstructors } from '@/composables/useInstructors'
import { useTheme, type Theme } from '@/composables/useTheme'
import { useRoute, useRouter } from 'vue-router'

defineEmits<{
	(e: 'toggle-sidebar'): void
}>()

const { role, instructorId, setRole, setInstructorId } = useRole()
const { instructors, loading: loadingInstructors, loadInstructors } = useInstructors()
const { theme, setTheme } = useTheme()
const router = useRouter()
const route = useRoute()

const ADMIN_ONLY_ROUTE_PREFIXES = ['/analytics', '/buildings', '/rooms', '/instructors', '/courses', '/timeslots', '/solver']
const ADMIN_ONLY_ROUTES = ['/schedules/new', '/requests/admin']

function isAdminOnlyPath(path: string): boolean {
	if (ADMIN_ONLY_ROUTES.includes(path)) return true
	return ADMIN_ONLY_ROUTE_PREFIXES.some(prefix => path === prefix || path.startsWith(`${prefix}/`))
}

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
		const raw = String(value).trim()
		if (!raw) {
			setInstructorId(null)
			return
		}
		const parsed = Number(raw)
		setInstructorId(Number.isInteger(parsed) && parsed > 0 ? parsed : null)
	},
})

function hasValidInstructorSelection(selectedId: number | null): boolean {
	if (selectedId === null) return false
	return instructors.value.some(instructor => instructor.id === selectedId)
}

watch(
	role,
	nextRole => {
		if (nextRole !== 'instructor') return
		if (instructors.value.length > 0 || loadingInstructors.value) return
		void loadInstructors()
	},
	{ immediate: true }
)

watch(
	[role, instructors, instructorId],
	([nextRole, nextInstructors, nextInstructorId]) => {
		if (nextRole !== 'instructor') return
		if (nextInstructors.length === 0) {
			if (nextInstructorId !== null) {
				setInstructorId(null)
			}
			return
		}
		if (hasValidInstructorSelection(nextInstructorId)) return
		const fallbackInstructor = nextInstructors[0]
		if (fallbackInstructor) {
			setInstructorId(fallbackInstructor.id)
		}
	},
	{ immediate: true }
)

watch(role, nextRole => {
	if (nextRole === 'instructor' && isAdminOnlyPath(route.path)) {
		void router.replace('/')
		return
	}

	if (nextRole === 'admin' && route.path === '/requests/new') {
		void router.replace('/requests/admin')
	}
})

function handleDataRegenerated() {
	void loadInstructors()
}

onMounted(() => {
	if (instructors.value.length === 0 && !loadingInstructors.value) {
		void loadInstructors()
	}
	// Listen for data regeneration events
	window.addEventListener('data-regenerated', handleDataRegenerated)
})

onUnmounted(() => {
	window.removeEventListener('data-regenerated', handleDataRegenerated)
})
</script>

<template>
	<header class="h-14 bg-white border-b border-gray-200 flex items-center gap-2 px-3 sm:px-6">
		<!-- Hamburger — mobile only -->
		<button type="button"
			class="md:hidden p-2 -ml-1 rounded text-gray-600 hover:text-gray-900 hover:bg-gray-100 shrink-0"
			aria-label="Toggle navigation" @click="$emit('toggle-sidebar')">
			<svg class="w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
				<path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
			</svg>
		</button>

		<!-- Logo — clips to COS mark on mobile, full on md+ -->
		<RouterLink to="/" class="cursor-pointer shrink-0 overflow-hidden w-14 md:w-auto" aria-label="Home">
			<svg class="h-12 w-auto max-w-55" viewBox="0 0 420 92" fill="none" xmlns="http://www.w3.org/2000/svg"
				role="img" aria-label="Campus Operations System logo">
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

		<!-- Controls -->
		<div class="ml-auto flex items-center gap-2 sm:gap-4 text-sm text-gray-600">
			<div class="flex items-center gap-1.5 sm:gap-2">
				<span class="hidden sm:inline text-xs uppercase tracking-wide text-gray-500">Theme</span>
				<select v-model="themeModel" aria-label="Theme"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700 text-xs sm:text-sm">
					<option value="snow-storm">Frost</option>
					<option value="slate">Slate</option>
				</select>
			</div>
			<div class="flex items-center gap-1.5 sm:gap-2">
				<span class="hidden sm:inline text-xs uppercase tracking-wide text-gray-500">Role</span>
				<select v-model="roleModel" aria-label="Role"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700 text-xs sm:text-sm">
					<option value="admin">Admin</option>
					<option value="instructor">Instructor</option>
				</select>
			</div>
			<div v-if="role !== 'admin'" class="flex items-center gap-1.5 sm:gap-2">
				<span class="hidden sm:inline text-xs uppercase tracking-wide text-gray-500">Instructor</span>
				<select v-model="instructorModel" aria-label="Instructor"
					class="px-2 py-1 border border-gray-300 rounded bg-white text-gray-700 text-xs sm:text-sm max-w-32 sm:max-w-none">
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
