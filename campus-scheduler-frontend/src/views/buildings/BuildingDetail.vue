<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DetailView, { type DetailField } from '@/components/common/DetailView.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { buildingsService } from '@/services/buildings'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data: building, loading, error } = useAsyncData(
    () => buildingsService.getById(id.value)
)

const fields = computed<DetailField[]>(() => [
    { label: 'Code', value: building.value?.code },
    { label: 'Address', value: building.value?.address },
])
</script>

<template>
    <DetailView :title="building?.name || 'Building'" :fields="fields" :loading="loading" :error="error"
        back-route="/buildings" back-label="Back to Buildings" :edit-route="`/buildings/${id}/edit`" />
</template>
