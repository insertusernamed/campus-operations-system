<script setup lang="ts" generic="T extends { id: number }">
import { computed, nextTick, ref, useSlots, watch } from 'vue'
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
	searchPlaceholder?: string
	hideSearch?: boolean
}>()

const PAGE_SIZE = 25

const slots = useSlots()
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
	if (props.hideSearch || !q) return props.items

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
const hasFilterSlot = computed(() => Boolean(slots.filters))
const hasMetricsSlot = computed(() => Boolean(slots.metrics))
const hasCellSlot = computed(() => Boolean(slots.cell))
const hasActionsSlot = computed(() => Boolean(slots.actions))

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
		<div class="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
			<h1 class="text-2xl font-semibold text-gray-900">{{ title }}</h1>
			<div v-if="!loading && !error && items.length > 0"
				class="flex w-full sm:w-auto flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-end">
				<input v-if="!hideSearch" v-model="searchQuery" type="text"
					:placeholder="searchPlaceholder || 'Search...'" aria-label="Search"
					class="h-10 w-full rounded border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 focus:outline-none focus:ring-2 focus:ring-blue-500 sm:w-64" />
				<slot v-if="hasFilterSlot" name="filters" :items="items" :filtered-items="filteredItems"
					:query="searchQuery" />
				<RouterLink v-if="createRoute" :to="createRoute"
					class="inline-flex h-10 items-center justify-center whitespace-nowrap rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">
					{{ createLabel || 'Add New' }}
				</RouterLink>
			</div>
			<RouterLink v-else-if="createRoute" :to="createRoute"
				class="inline-flex h-10 items-center justify-center whitespace-nowrap rounded bg-blue-600 px-4 py-2 text-white hover:bg-blue-700">
				{{ createLabel || 'Add New' }}
			</RouterLink>
		</div>

		<div v-if="hasMetricsSlot && !loading && !error" class="mb-5">
			<slot name="metrics" :items="items" :filtered-items="filteredItems" />
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
			<slot name="filtered-empty">
				No matching items found.
			</slot>
		</div>

		<div v-else>
			<table ref="tableRef" class="w-full border border-gray-200 bg-white">
				<thead>
					<tr class="border-b border-gray-200 bg-gray-50">
						<th v-for="col in columns" :key="String(col.key)"
							class="px-4 py-3 text-left text-sm font-medium text-gray-700">
							{{ col.label }}
						</th>
						<th v-if="editRoute || onDelete" class="px-4 py-3 text-left text-sm font-medium text-gray-700">
							Actions
						</th>
					</tr>
				</thead>
				<tbody>
					<tr v-for="item in pagedItems" :key="item.id" class="border-b border-gray-100">
						<td v-for="col in columns" :key="String(col.key)" class="px-4 py-3">
							<slot v-if="hasCellSlot" name="cell" :item="item" :column="col"
								:value="getValue(item, col)" />
							<template v-else>
								<RouterLink v-if="col.linkTo" :to="col.linkTo(item)"
									class="text-blue-600 hover:underline">
									{{ getValue(item, col) }}
								</RouterLink>
								<span v-else class="text-gray-600">{{ getValue(item, col) }}</span>
							</template>
						</td>
						<td v-if="editRoute || onDelete" class="px-4 py-3">
							<slot v-if="hasActionsSlot" name="actions" :item="item" />
							<template v-else>
								<RouterLink v-if="editRoute" :to="editRoute(item)"
									class="mr-4 text-blue-600 hover:underline">
									Edit
								</RouterLink>
								<button v-if="onDelete" @click="onDelete(item.id)" class="text-red-600 hover:underline">
									Delete
								</button>
							</template>
						</td>
					</tr>
				</tbody>
			</table>

			<div v-if="filteredItems.length > PAGE_SIZE" class="mt-4 flex items-center justify-between">
				<div class="text-sm text-gray-500">
					Showing {{ startItemIndex }}-{{ endItemIndex }} of {{ filteredItems.length }}
				</div>
				<div class="flex items-center gap-2">
					<button type="button" @click="goToPage(page - 1)" :disabled="page === 1"
						class="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white">
						Previous
					</button>
					<div class="text-sm text-gray-700">
						Page {{ page }} of {{ totalPages }}
					</div>
					<button type="button" @click="goToPage(page + 1)" :disabled="page === totalPages"
						class="rounded border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:hover:bg-white">
						Next
					</button>
				</div>
			</div>
		</div>
	</div>
</template>
