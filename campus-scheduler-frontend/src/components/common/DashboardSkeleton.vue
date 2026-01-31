<template>
	<div class="animate-pulse">
		<!-- Stat cards -->
		<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
			<div v-for="i in 4" :key="i" class="bg-white border border-gray-200 p-4 rounded">
				<div class="h-8 bg-gray-200 rounded w-16 mb-2"></div>
				<div class="h-4 bg-gray-200 rounded w-24"></div>
			</div>
		</div>

		<!-- Charts row -->
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
			<div class="bg-white border border-gray-200 p-4 rounded">
				<div class="h-5 bg-gray-200 rounded w-40 mb-4"></div>
				<div class="h-48 bg-gray-200 rounded"></div>
			</div>
			<div class="bg-white border border-gray-200 p-4 rounded">
				<div class="h-5 bg-gray-200 rounded w-32 mb-4"></div>
				<div class="h-48 bg-gray-200 rounded"></div>
			</div>
		</div>

		<!-- Heatmap -->
		<div class="bg-white border border-gray-200 p-4 rounded">
			<div class="h-5 bg-gray-200 rounded w-48 mb-4"></div>
			<div class="h-64 bg-gray-200 rounded"></div>
		</div>
	</div>
</template>
