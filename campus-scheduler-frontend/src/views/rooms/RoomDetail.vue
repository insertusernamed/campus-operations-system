<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DetailView, { type DetailField } from '@/components/common/DetailView.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { roomsService } from '@/services/rooms'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data: room, loading, error } = useAsyncData(
    () => roomsService.getById(id.value)
)

const fields = computed<DetailField[]>(() => [
    { label: 'Building', value: room.value?.buildingName },
    { label: 'Capacity', value: room.value?.capacity },
    { label: 'Type', value: room.value?.type },
    { label: 'Features', value: room.value?.features },
])
</script>

<template>
    <DetailView :title="`Room ${room?.roomNumber || ''}`" :fields="fields" :loading="loading" :error="error"
        back-route="/rooms" back-label="Back to Rooms" :edit-route="`/rooms/${id}/edit`" />
</template>
