<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useRole, type Role } from '@/composables/useRole'

const props = defineProps<{
	isOpen: boolean
}>()

const emit = defineEmits<{
	(e: 'close'): void
}>()

function handleNavClick() {
	// Close the drawer on mobile after navigating
	emit('close')
}

const route = useRoute()
const { role } = useRole()

interface NavItem {
	name: string
	path: string
	pathByRole?: Partial<Record<Role, string>>
	icon: string
	roles?: Role[]
}

interface NavGroup {
	title: string
	items: NavItem[]
}

const navigation: NavGroup[] = [
	{
		title: 'Overview',
		items: [
			{ name: 'Dashboard', path: '/', icon: 'home', roles: ['admin', 'instructor', 'student'] },
			{ name: 'Analytics', path: '/analytics', icon: 'chart', roles: ['admin'] },
		],
	},
	{
		title: 'Scheduling',
		items: [
			{ name: 'Solver', path: '/solver', icon: 'bolt', roles: ['admin'] },
			{ name: 'Schedules', path: '/schedules', icon: 'calendar', roles: ['admin', 'instructor'] },
			{
				name: 'Requests',
				path: '/requests',
				pathByRole: { admin: '/requests/admin', instructor: '/requests' },
				icon: 'arrows',
				roles: ['admin', 'instructor'],
			},
			{ name: 'Time Slots', path: '/timeslots', icon: 'clock', roles: ['admin'] },
		],
	},
	{
		title: 'Resources',
		items: [
			{ name: 'Buildings', path: '/buildings', icon: 'building', roles: ['admin'] },
			{ name: 'Rooms', path: '/rooms', icon: 'door', roles: ['admin'] },
		],
	},
	{
		title: 'Academics',
		items: [
			{ name: 'Courses', path: '/courses', icon: 'book', roles: ['admin'] },
			{ name: 'Instructors', path: '/instructors', icon: 'user', roles: ['admin'] },
		],
	},
]

const visibleNavigation = computed(() =>
	navigation
		.map(group => ({
			...group,
			items: group.items.filter(item => !item.roles || item.roles.includes(role.value)),
		}))
		.filter(group => group.items.length > 0)
)

function isActive(path: string): boolean {
	if (path === '/') return route.path === '/'
	return route.path.startsWith(path)
}

function getNavPath(item: NavItem): string {
	return item.pathByRole?.[role.value] ?? item.path
}
</script>

<template>
	<aside :class="[
		'w-56 flex flex-col bg-white border-r border-gray-200',
		'fixed top-14 bottom-0 left-0 z-40 transition-transform duration-300 ease-in-out',
		'md:relative md:top-auto md:bottom-auto md:z-auto',
		isOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0',
	]">
		<nav class="flex-1 py-4 overflow-y-auto">
			<div v-for="group in visibleNavigation" :key="group.title" class="mb-6">
				<h3 class="px-4 mb-1 text-[11px] font-semibold text-gray-500 uppercase tracking-wide">
					{{ group.title }}
				</h3>
				<ul>
					<li v-for="item in group.items" :key="item.name">
						<RouterLink :to="getNavPath(item)" @click="handleNavClick"
							class="flex items-center gap-3 mx-2 px-3 py-1.5 min-h-11 text-sm rounded transition-colors"
							:class="isActive(getNavPath(item))
								? 'bg-gray-100 text-gray-900 font-medium'
								: 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
								">
							<!-- Icons -->
							<svg v-if="item.icon === 'home'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M2.25 12l8.954-8.955a1.126 1.126 0 011.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
							</svg>
							<svg v-else-if="item.icon === 'chart'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
							</svg>
							<svg v-else-if="item.icon === 'bolt'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z" />
							</svg>
							<svg v-else-if="item.icon === 'calendar'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" />
							</svg>
							<svg v-else-if="item.icon === 'arrows'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M7.5 21 3 16.5m0 0L7.5 12M3 16.5h13.5m0-13.5L21 7.5m0 0L16.5 12M21 7.5H7.5" />
							</svg>
							<svg v-else-if="item.icon === 'clock'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
							</svg>
							<svg v-else-if="item.icon === 'building'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M2.25 21h19.5m-18-18v18m10.5-18v18m6-13.5V21M6.75 6.75h.75m-.75 3h.75m-.75 3h.75m3-6h.75m-.75 3h.75m-.75 3h.75M6.75 21v-3.375c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21M3 3h12m-.75 4.5H21m-3.75 3H21m-3.75 3H21" />
							</svg>
							<svg v-else-if="item.icon === 'door'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M3.75 21h16.5M4.5 3h15M5.25 3v18m13.5-18v18M9 6.75h1.5m-1.5 3h1.5m-1.5 3h1.5m3-6H15m-1.5 3H15m-1.5 3H15M9 21v-3.375c0-.621.504-1.125 1.125-1.125h3.75c.621 0 1.125.504 1.125 1.125V21" />
							</svg>
							<svg v-else-if="item.icon === 'book'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
							</svg>
							<svg v-else-if="item.icon === 'user'" class="w-4 h-4" fill="none" stroke="currentColor"
								stroke-width="1.5" viewBox="0 0 24 24">
								<path stroke-linecap="round" stroke-linejoin="round"
									d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
							</svg>
							<span>{{ item.name }}</span>
						</RouterLink>
					</li>
				</ul>
			</div>
		</nav>
	</aside>
</template>
