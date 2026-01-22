<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import EntityForm, { type FormField } from '@/components/common/EntityForm.vue'
import { useCrud } from '@/composables/useCrud'
import { buildingsService, type CreateBuildingRequest } from '@/services/buildings'

const route = useRoute()
const isEdit = computed(() => route.params.id !== undefined)
const buildingId = computed(() => Number(route.params.id))

const form = ref<CreateBuildingRequest>({
    name: '',
    code: '',
    address: '',
})

const { saving, error, handleSave } = useCrud({
    getAll: buildingsService.getAll,
    create: buildingsService.create,
    update: buildingsService.update,
    listRoute: '/buildings',
})

const loading = ref(false)

const fields: FormField[] = [
    { name: 'code', label: 'Code', type: 'text', required: true, maxLength: 10, placeholder: 'e.g., ATAC' },
    { name: 'name', label: 'Name', type: 'text', required: true, maxLength: 100, placeholder: 'e.g., Advanced Technology Centre' },
    { name: 'address', label: 'Address', type: 'text', maxLength: 255, placeholder: 'e.g., 955 Oliver Rd', span: 2 },
]

onMounted(async () => {
    if (isEdit.value) {
        loading.value = true
        try {
            const building = await buildingsService.getById(buildingId.value)
            form.value = { name: building.name, code: building.code, address: building.address || '' }
        } finally {
            loading.value = false
        }
    }
})

function onSubmit() {
    handleSave(form.value, isEdit.value ? buildingId.value : undefined)
}
</script>

<template>
    <EntityForm :title="isEdit ? 'Edit Building' : 'New Building'" :fields="fields" v-model="form" :loading="loading"
        :saving="saving" :error="error" back-route="/buildings" back-label="Back to Buildings"
        :submit-label="isEdit ? 'Update' : 'Create'" @submit="onSubmit" />
</template>
