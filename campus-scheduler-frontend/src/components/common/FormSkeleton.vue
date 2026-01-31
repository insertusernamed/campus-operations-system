<script setup lang="ts">
defineProps<{
	fields?: number
}>()
</script>

<template>
	<div class="animate-pulse max-w-xl">
		<!-- Title -->
		<div class="h-7 bg-gray-200 rounded w-48 mb-6"></div>

		<!-- Fields -->
		<div class="space-y-4">
			<div v-for="i in (fields ?? 4)" :key="i">
				<!-- Label -->
				<div class="h-4 bg-gray-200 rounded w-24 mb-2"></div>
				<!-- Input -->
				<div class="h-10 bg-gray-200 rounded w-full"></div>
			</div>
		</div>

		<!-- Button -->
		<div class="h-10 bg-gray-200 rounded w-28 mt-6"></div>
	</div>
</template>
