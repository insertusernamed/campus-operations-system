<script setup lang="ts">
import { onMounted } from 'vue'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCrud } from '@/composables/useCrud'
import { roomsService, type Room } from '@/services/rooms'

const { items, loading, error, fetchAll, handleDelete } = useCrud<Room, never>({
	getAll: roomsService.getAll,
	deleteItem: roomsService.delete,
	listRoute: '/rooms',
	deleteConfirm: 'Are you sure you want to delete this room?',
})

const columns: Column<Room>[] = [
	{ key: 'buildingName', label: 'Building' },
	{ key: 'roomNumber', label: 'Room', linkTo: (r) => `/rooms/${r.id}` },
	{ key: 'capacity', label: 'Capacity' },
	{ key: 'type', label: 'Type' },
	{ key: 'features', label: 'Features' },
]

onMounted(fetchAll)
</script>

<template>
	<DataTable title="Rooms" :items="items" :columns="columns" :loading="loading" :error="error"
		create-route="/rooms/new" create-label="Add Room" :edit-route="(r) => `/rooms/${r.id}/edit`"
		:on-delete="handleDelete">
		<template #empty>
			<EmptyState title="No rooms yet"
				description="Rooms are physical spaces where classes are held. Add rooms with their capacity and type."
				action-label="Add Room" action-route="/rooms/new" secondary-label="Add Building First"
				secondary-route="/buildings/new"
				hint="Rooms belong to buildings. Make sure you have at least one building before adding rooms." />
		</template>
	</DataTable>
</template>
