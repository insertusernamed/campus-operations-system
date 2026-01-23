<template>
	<main>
		<h1 class="text-2xl font-bold mb-6">Campus Scheduler</h1>

		<div class="grid grid-cols-2 md:grid-cols-3 gap-4">
			<RouterLink to="/solver" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Auto-Scheduler</div>
				<div class="text-sm text-gray-500">Generate optimized schedules</div>
			</RouterLink>

			<RouterLink to="/analytics" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Analytics</div>
				<div class="text-sm text-gray-500">View utilization stats</div>
			</RouterLink>

			<RouterLink to="/schedules" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Schedules</div>
				<div class="text-sm text-gray-500">Manage class schedules</div>
			</RouterLink>

			<RouterLink to="/rooms" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Rooms</div>
				<div class="text-sm text-gray-500">Browse available rooms</div>
			</RouterLink>

			<RouterLink to="/buildings" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Buildings</div>
				<div class="text-sm text-gray-500">Manage buildings</div>
			</RouterLink>

			<RouterLink to="/instructors" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Instructors</div>
				<div class="text-sm text-gray-500">View instructor list</div>
			</RouterLink>

			<RouterLink to="/courses" class="block p-4 border hover:bg-gray-50">
				<div class="font-medium">Courses</div>
				<div class="text-sm text-gray-500">Manage courses</div>
			</RouterLink>
		</div>
	</main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>
