<script setup lang="ts" generic="T extends { id: number }">
import { computed, nextTick, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import TableSkeleton from './TableSkeleton.vue'

export interface Column<T> {
	key: keyof T | string
	label: string
	/** Custom render function - receives the item */
	render?: (item: T) => string
	/** Make this column a link to detail page */
	linkTo?: (item: T) => string
}

const props = defineProps<{
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

const PAGE_SIZE = 25

const searchQuery = ref('')
const page = ref(1)
const tableRef = ref<HTMLTableElement | null>(null)

const skeletonColumns = computed(() => {
	const base = props.columns.length
	const hasActions = props.editRoute || props.onDelete
	return hasActions ? base + 1 : base
})

function getValue<T>(item: T, col: Column<T>): string {
	if (col.render) {
		return col.render(item)
	}
	const value = (item as Record<string, unknown>)[col.key as string]
	return value != null ? String(value) : '-'
}

const normalizedQuery = computed(() => searchQuery.value.trim().toLowerCase())

const filteredItems = computed(() => {
	const q = normalizedQuery.value
	if (!q) return props.items

	return props.items.filter((item) =>
		props.columns.some((col) => getValue(item, col).toLowerCase().includes(q)),
	)
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredItems.value.length / PAGE_SIZE)))

const pagedItems = computed(() => {
	const start = (page.value - 1) * PAGE_SIZE
	return filteredItems.value.slice(start, start + PAGE_SIZE)
})

const startItemIndex = computed(() => {
	if (filteredItems.value.length === 0) return 0
	return (page.value - 1) * PAGE_SIZE + 1
})

const endItemIndex = computed(() => Math.min(page.value * PAGE_SIZE, filteredItems.value.length))

function prefersReducedMotion(): boolean {
	if (typeof window === 'undefined') return true
	return window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches ?? false
}

async function goToPage(nextPage: number) {
	const clamped = Math.min(Math.max(1, nextPage), totalPages.value)
	if (clamped === page.value) return

	page.value = clamped
	await nextTick()

	tableRef.value?.scrollIntoView({
		behavior: prefersReducedMotion() ? 'auto' : 'smooth',
		block: 'start',
	})
}

watch(normalizedQuery, () => {
	page.value = 1
})

watch(
	() => filteredItems.value.length,
	() => {
		if (page.value > totalPages.value) page.value = totalPages.value
	},
)
</script>

<template>
	<div>
		<div class="flex flex-col gap-3 sm:flex-row sm:justify-between sm:items-center mb-6">
			<h1 class="text-2xl font-semibold text-gray-900">{{ title }}</h1>
			<div v-if="!loading && !error && items.length > 0" class="flex flex-col gap-3 sm:flex-row sm:items-center">
				<input v-model="searchQuery" type="text" placeholder="Search..." aria-label="Search"
					class="w-full sm:w-64 h-10 px-3 py-2 text-sm border border-gray-300 rounded bg-white text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500" />
				<RouterLink v-if="createRoute" :to="createRoute"
					class="h-10 inline-flex items-center justify-center px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 whitespace-nowrap">
					{{ createLabel || 'Add New' }}
				</RouterLink>
			</div>
			<RouterLink v-else-if="createRoute" :to="createRoute"
				class="h-10 inline-flex items-center justify-center px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 whitespace-nowrap">
				{{ createLabel || 'Add New' }}
			</RouterLink>
		</div>

		<div v-if="loading">
			<TableSkeleton :columns="skeletonColumns" :rows="5" />
		</div>
		<div v-else-if="error" class="text-red-600">{{ error }}</div>
		<template v-else-if="items.length === 0">
			<slot name="empty">
				<div class="text-gray-500">No items found.</div>
			</slot>
		</template>
		<div v-else-if="filteredItems.length === 0" class="text-gray-500">
			No matching items found.
		</div>

		<div v-else>
			<table ref="tableRef" class="w-full bg-white border border-gray-200">
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
					<tr v-for="item in pagedItems" :key="item.id" class="border-b border-gray-100">
						<td v-for="col in columns" :key="String(col.key)" class="px-4 py-3">
							<RouterLink v-if="col.linkTo" :to="col.linkTo(item)" class="text-blue-600 hover:underline">
								{{ getValue(item, col) }}
							</RouterLink>
							<span v-else class="text-gray-600">{{ getValue(item, col) }}</span>
						</td>
						<td v-if="editRoute || onDelete" class="px-4 py-3">
							<RouterLink v-if="editRoute" :to="editRoute(item)"
								class="text-blue-600 hover:underline mr-4">
								Edit
							</RouterLink>
							<button v-if="onDelete" @click="onDelete(item.id)" class="text-red-600 hover:underline">
								Delete
							</button>
						</td>
					</tr>
				</tbody>
			</table>

			<div v-if="filteredItems.length > PAGE_SIZE" class="flex justify-between items-center mt-4">
				<div class="text-sm text-gray-500">
					Showing {{ startItemIndex }}-{{ endItemIndex }} of {{ filteredItems.length }}
				</div>
				<div class="flex items-center gap-2">
					<button type="button" @click="goToPage(page - 1)" :disabled="page === 1"
						class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white">
						Previous
					</button>
					<div class="text-sm text-gray-700">
						Page {{ page }} of {{ totalPages }}
					</div>
					<button type="button" @click="goToPage(page + 1)" :disabled="page === totalPages"
						class="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white">
						Next
					</button>
				</div>
			</div>
		</div>
	</div>
</template>
