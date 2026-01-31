<script setup lang="ts">
import { onMounted } from 'vue'
import DataTable, { type Column } from '@/components/common/DataTable.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useCrud } from '@/composables/useCrud'
import { coursesService, type Course } from '@/services/courses'

const { items, loading, error, fetchAll, handleDelete } = useCrud<Course, never>({
	getAll: coursesService.getAll,
	deleteItem: coursesService.delete,
	listRoute: '/courses',
	deleteConfirm: 'Are you sure you want to delete this course?',
})

const columns: Column<Course>[] = [
	{ key: 'code', label: 'Code', linkTo: (c) => `/courses/${c.id}` },
	{ key: 'name', label: 'Name' },
	{ key: 'credits', label: 'Credits' },
	{ key: 'department', label: 'Department' },
	{ key: 'instructor', label: 'Instructor', render: (c) => c.instructor ? `${c.instructor.firstName} ${c.instructor.lastName}` : '-' },
]

onMounted(fetchAll)
</script>

<template>
	<DataTable title="Courses" :items="items" :columns="columns" :loading="loading" :error="error"
		create-route="/courses/new" create-label="Add Course" :edit-route="(c) => `/courses/${c.id}/edit`"
		:on-delete="handleDelete">
		<template #empty>
			<EmptyState title="No courses yet"
				description="Courses are the classes that need to be scheduled into rooms. Add courses with enrollment counts and room requirements."
				action-label="Add Course" action-route="/courses/new"
				hint="Each course specifies how many students it has and what type of room it needs." />
		</template>
	</DataTable>
</template>
