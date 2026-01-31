<script setup lang="ts">
import { onMounted } from 'vue'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCrud } from '@/composables/useCrud'
import { buildingsService, type Building } from '@/services/buildings'

const { items, loading, error, fetchAll, handleDelete } = useCrud<Building, never>({
	getAll: buildingsService.getAll,
	deleteItem: buildingsService.delete,
	listRoute: '/buildings',
	deleteConfirm: 'Are you sure you want to delete this building?',
})

const columns: Column<Building>[] = [
	{ key: 'code', label: 'Code', linkTo: (b) => `/buildings/${b.id}` },
	{ key: 'name', label: 'Name' },
	{ key: 'address', label: 'Address' },
]

onMounted(fetchAll)
</script>

<template>
	<DataTable title="Buildings" :items="items" :columns="columns" :loading="loading" :error="error"
		create-route="/buildings/new" create-label="Add Building" :edit-route="(b) => `/buildings/${b.id}/edit`"
		:on-delete="handleDelete">
		<template #empty>
			<EmptyState title="No buildings yet"
				description="Buildings contain the rooms where classes are held. Start by adding your campus buildings."
				action-label="Add Building" action-route="/buildings/new"
				hint="You can also generate sample data from the Solver page." />
		</template>
	</DataTable>
</template>
