<script setup lang="ts" generic="T extends { id: number }">
import { RouterLink } from 'vue-router'

export interface Column<T> {
	key: keyof T | string
	label: string
	/** Custom render function - receives the item */
	render?: (item: T) => string
	/** Make this column a link to detail page */
	linkTo?: (item: T) => string
}

defineProps<{
	title: string
	items: T[]
	columns: Column<T>[]
	loading: boolean
	error: string | null
	createRoute?: string
	createLabel?: string
	editRoute?: (item: T) => string
	onDelete?: (id: number) => void
}>()

function getValue<T>(item: T, col: Column<T>): string {
	if (col.render) {
		return col.render(item)
	}
	const value = (item as Record<string, unknown>)[col.key as string]
	return value != null ? String(value) : '-'
}
</script>

<template>
	<div>
		<div class="flex justify-between items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">{{ title }}</h1>
			<RouterLink v-if="createRoute" :to="createRoute"
				class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
				{{ createLabel || 'Add New' }}
			</RouterLink>
		</div>

		<div v-if="loading" class="text-gray-500">Loading...</div>
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<template v-else-if="items.length === 0">
			<slot name="empty">
				<div class="text-gray-500">No items found.</div>
			</slot>
		</template>

		<table v-else class="w-full bg-white border border-gray-200">
			<thead>
				<tr class="bg-gray-50 border-b border-gray-200">
					<th v-for="col in columns" :key="String(col.key)"
						class="text-left px-4 py-3 text-sm font-medium text-gray-700">
						{{ col.label }}
					</th>
					<th v-if="editRoute || onDelete" class="text-left px-4 py-3 text-sm font-medium text-gray-700">
						Actions
					</th>
				</tr>
			</thead>
			<tbody>
				<tr v-for="item in items" :key="item.id" class="border-b border-gray-100">
					<td v-for="col in columns" :key="String(col.key)" class="px-4 py-3">
						<RouterLink v-if="col.linkTo" :to="col.linkTo(item)" class="text-blue-600 hover:underline">
							{{ getValue(item, col) }}
						</RouterLink>
						<span v-else class="text-gray-600">{{ getValue(item, col) }}</span>
					</td>
					<td v-if="editRoute || onDelete" class="px-4 py-3">
						<RouterLink v-if="editRoute" :to="editRoute(item)" class="text-blue-600 hover:underline mr-4">
							Edit
						</RouterLink>
						<button v-if="onDelete" @click="onDelete(item.id)" class="text-red-600 hover:underline">
							Delete
						</button>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
</template>
