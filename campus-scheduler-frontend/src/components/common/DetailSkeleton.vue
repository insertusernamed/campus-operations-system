<script setup lang="ts">
defineProps<{
	fields?: number
}>()
</script>

<template>
	<div class="animate-pulse">
		<!-- Back link -->
		<div class="h-4 bg-gray-200 rounded w-16 mb-6"></div>

		<div class="bg-white border border-gray-200 p-6">
			<!-- Header -->
			<div class="flex justify-between items-start mb-6">
				<div>
					<div class="h-7 bg-gray-200 rounded w-56 mb-2"></div>
					<div class="h-5 bg-gray-200 rounded w-40"></div>
				</div>
				<div class="h-10 bg-gray-200 rounded w-20"></div>
			</div>

			<!-- Fields grid -->
			<div class="grid grid-cols-2 gap-4">
				<div v-for="i in (fields ?? 6)" :key="i">
					<div class="h-4 bg-gray-200 rounded w-20 mb-1"></div>
					<div class="h-5 bg-gray-200 rounded" :style="{ width: `${60 + (i * 17) % 30}%` }"></div>
				</div>
			</div>
		</div>
	</div>
</template>
