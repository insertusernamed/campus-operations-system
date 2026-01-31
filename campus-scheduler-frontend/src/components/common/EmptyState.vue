<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
	title: string
	description: string
	actionLabel?: string
	actionRoute?: string
	secondaryLabel?: string
	secondaryRoute?: string
	hint?: string
}>()
</script>

<template>
	<div class="py-12 px-4 text-center">
		<div class="max-w-sm mx-auto">
			<h3 class="text-base font-medium text-gray-900 mb-1">{{ title }}</h3>
			<p class="text-sm text-gray-600 mb-5">{{ description }}</p>

			<div class="flex items-center justify-center gap-3">
				<RouterLink v-if="actionLabel && actionRoute" :to="actionRoute"
					class="px-4 py-2 text-sm bg-blue-600 text-white rounded hover:bg-blue-700">
					{{ actionLabel }}
				</RouterLink>
				<RouterLink v-if="secondaryLabel && secondaryRoute" :to="secondaryRoute"
					class="px-4 py-2 text-sm border border-gray-300 text-gray-700 rounded hover:bg-gray-50">
					{{ secondaryLabel }}
				</RouterLink>
			</div>

			<p v-if="hint" class="mt-5 text-xs text-gray-500">
				{{ hint }}
			</p>
		</div>
	</div>
</template>
