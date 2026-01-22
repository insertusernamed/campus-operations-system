<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DetailView, { type DetailField } from '@/components/common/DetailView.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { coursesService } from '@/services/courses'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data: course, loading, error } = useAsyncData(
	() => coursesService.getById(id.value)
)

const fields = computed<DetailField[]>(() => [
	{ label: 'Credits', value: course.value?.credits },
	{ label: 'Enrollment Capacity', value: course.value?.enrollmentCapacity },
	{ label: 'Department', value: course.value?.department },
	{
		label: 'Instructor',
		value: course.value?.instructor ? `${course.value.instructor.firstName} ${course.value.instructor.lastName}` : null,
		linkTo: course.value?.instructor ? `/instructors/${course.value.instructor.id}` : undefined,
	},
	{ label: 'Description', value: course.value?.description, fullWidth: true },
])
</script>

<template>
	<DetailView :title="course?.code || 'Course'" :subtitle="course?.name" :fields="fields" :loading="loading"
		:error="error" back-route="/courses" back-label="Back to Courses" :edit-route="`/courses/${id}/edit`" />
</template>
