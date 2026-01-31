<script setup lang="ts">
defineProps<{
	columns?: number
	rows?: number
}>()
</script>

<template>
	<div class="animate-pulse">
		<!-- Header -->
		<div class="bg-gray-50 border border-gray-200 rounded-t">
			<div class="grid gap-4 px-4 py-3" :style="{ gridTemplateColumns: `repeat(${columns ?? 4}, 1fr)` }">
				<div v-for="i in (columns ?? 4)" :key="'h' + i" class="h-4 bg-gray-200 rounded"
					:style="{ width: i === 1 ? '60%' : '40%' }"></div>
			</div>
		</div>

		<!-- Rows -->
		<div class="border-x border-b border-gray-200 rounded-b divide-y divide-gray-100">
			<div v-for="row in (rows ?? 5)" :key="'r' + row" class="grid gap-4 px-4 py-3"
				:style="{ gridTemplateColumns: `repeat(${columns ?? 4}, 1fr)` }">
				<div v-for="col in (columns ?? 4)" :key="'c' + col" class="h-4 bg-gray-200 rounded"
					:style="{ width: col === 1 ? '75%' : `${50 + (row * 7 + col * 13) % 30}%` }"></div>
			</div>
		</div>
	</div>
</template>
