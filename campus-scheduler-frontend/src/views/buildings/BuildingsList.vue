<script setup lang="ts">
import { onMounted } from 'vue'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
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
        :on-delete="handleDelete" />
</template>
