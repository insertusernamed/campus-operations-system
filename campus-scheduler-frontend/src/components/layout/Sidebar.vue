<script setup lang="ts">
import { RouterLink } from 'vue-router'

const navItems = [
	{ name: 'Dashboard', path: '/' },
	{ name: 'Rooms', path: '/rooms' },
	{ name: 'Courses', path: '/courses' },
	{ name: 'Instructors', path: '/instructors' },
	{ name: 'Schedules', path: '/schedules' },
]
</script>

<template>
	<aside class="w-56 bg-white border-r border-gray-200 p-4">
		<nav>
			<ul class="space-y-1">
				<li v-for="item in navItems" :key="item.path">
					<RouterLink :to="item.path" class="block px-3 py-2 text-sm text-gray-700 rounded hover:bg-gray-100"
						active-class="bg-gray-100 font-medium">
						{{ item.name }}
					</RouterLink>
				</li>
			</ul>
		</nav>
	</aside>
</template>
