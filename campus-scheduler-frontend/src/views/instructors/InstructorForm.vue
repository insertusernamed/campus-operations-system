<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import EntityForm, { type FormField } from '@/components/common/EntityForm.vue'
import { useCrud } from '@/composables/useCrud'
import { instructorsService, type CreateInstructorRequest } from '@/services/instructors'

const route = useRoute()
const isEdit = computed(() => route.params.id !== undefined)
const instructorId = computed(() => Number(route.params.id))

const form = ref<CreateInstructorRequest>({
    firstName: '',
    lastName: '',
    email: '',
    department: '',
    officeNumber: '',
})

const { saving, error, handleSave } = useCrud({
    getAll: instructorsService.getAll,
    create: instructorsService.create,
    update: instructorsService.update,
    listRoute: '/instructors',
})

const loading = ref(false)

const fields: FormField[] = [
    { name: 'firstName', label: 'First Name', type: 'text', required: true, maxLength: 50 },
    { name: 'lastName', label: 'Last Name', type: 'text', required: true, maxLength: 50 },
    { name: 'email', label: 'Email', type: 'email', required: true, maxLength: 100, placeholder: 'instructor@university.edu', span: 2 },
    { name: 'department', label: 'Department', type: 'text', maxLength: 50, placeholder: 'e.g., Computer Science' },
    { name: 'officeNumber', label: 'Office Number', type: 'text', maxLength: 20, placeholder: 'e.g., ATAC 5012' },
]

onMounted(async () => {
    if (isEdit.value) {
        loading.value = true
        try {
            const instructor = await instructorsService.getById(instructorId.value)
            form.value = {
                firstName: instructor.firstName,
                lastName: instructor.lastName,
                email: instructor.email,
                department: instructor.department || '',
                officeNumber: instructor.officeNumber || '',
            }
        } finally {
            loading.value = false
        }
    }
})

function onSubmit() {
    handleSave(form.value, isEdit.value ? instructorId.value : undefined)
}
</script>

<template>
    <EntityForm :title="isEdit ? 'Edit Instructor' : 'New Instructor'" :fields="fields" v-model="form"
        :loading="loading" :saving="saving" :error="error" back-route="/instructors" back-label="Back to Instructors"
        :submit-label="isEdit ? 'Update' : 'Create'" @submit="onSubmit" />
</template>
