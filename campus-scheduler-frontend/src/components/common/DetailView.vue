<script setup lang="ts">
import { RouterLink } from 'vue-router'
import DetailSkeleton from './DetailSkeleton.vue'

export interface DetailField {
	label: string
	value: string | number | null | undefined
	/** Render as link */
	linkTo?: string
	/** Span full width */
	fullWidth?: boolean
}

defineProps<{
	title: string
	subtitle?: string
	fields: DetailField[]
	loading: boolean
	error: string | null
	backRoute: string
	backLabel?: string
	editRoute?: string
}>()
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink :to="backRoute" class="text-blue-600 hover:underline text-sm">
				{{ backLabel || 'Back' }}
			</RouterLink>
		</div>

		<DetailSkeleton v-if="loading" :fields="fields.length" />
		<div v-else-if="error" class="text-red-600">{{ error }}</div>

		<div v-else class="bg-white border border-gray-200 p-6">
			<div class="flex justify-between items-start mb-4">
				<div>
					<h1 class="text-2xl font-semibold text-gray-900">{{ title }}</h1>
					<p v-if="subtitle" class="text-lg text-gray-600">{{ subtitle }}</p>
				</div>
				<RouterLink v-if="editRoute" :to="editRoute"
					class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
					Edit
				</RouterLink>
			</div>

			<dl class="grid grid-cols-1 sm:grid-cols-2 gap-4">
				<div v-for="field in fields" :key="field.label" :class="{ 'col-span-full': field.fullWidth }">
					<dt class="text-sm font-medium text-gray-500">{{ field.label }}</dt>
					<dd class="text-gray-900">
						<RouterLink v-if="field.linkTo && field.value" :to="field.linkTo"
							class="text-blue-600 hover:underline">
							{{ field.value }}
						</RouterLink>
						<template v-else>{{ field.value ?? '-' }}</template>
					</dd>
				</div>
			</dl>
		</div>
	</div>
</template>
