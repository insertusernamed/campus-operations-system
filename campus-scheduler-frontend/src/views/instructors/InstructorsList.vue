<script setup lang="ts">
import { onMounted } from 'vue'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import { useCrud } from '@/composables/useCrud'
import { instructorsService, type Instructor } from '@/services/instructors'

const { items, loading, error, fetchAll, handleDelete } = useCrud<Instructor, never>({
    getAll: instructorsService.getAll,
    deleteItem: instructorsService.delete,
    listRoute: '/instructors',
    deleteConfirm: 'Are you sure you want to delete this instructor?',
})

const columns: Column<Instructor>[] = [
    { key: 'name', label: 'Name', linkTo: (i) => `/instructors/${i.id}`, render: (i) => `${i.lastName}, ${i.firstName}` },
    { key: 'email', label: 'Email' },
    { key: 'department', label: 'Department' },
    { key: 'officeNumber', label: 'Office' },
]

onMounted(fetchAll)
</script>

<template>
    <DataTable title="Instructors" :items="items" :columns="columns" :loading="loading" :error="error"
        create-route="/instructors/new" create-label="Add Instructor" :edit-route="(i) => `/instructors/${i.id}/edit`"
        :on-delete="handleDelete" />
</template>
