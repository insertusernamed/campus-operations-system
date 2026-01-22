<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DetailView, { type DetailField } from '@/components/common/DetailView.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { instructorsService } from '@/services/instructors'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data: instructor, loading, error } = useAsyncData(
	() => instructorsService.getById(id.value)
)

const fields = computed<DetailField[]>(() => [
	{ label: 'Email', value: instructor.value?.email },
	{ label: 'Department', value: instructor.value?.department },
	{ label: 'Office Number', value: instructor.value?.officeNumber },
])
</script>

<template>
	<DetailView :title="instructor ? `${instructor.firstName} ${instructor.lastName}` : 'Instructor'" :fields="fields"
		:loading="loading" :error="error" back-route="/instructors" back-label="Back to Instructors"
		:edit-route="`/instructors/${id}/edit`" />
</template>
