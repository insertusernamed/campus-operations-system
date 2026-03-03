<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DetailView, { type DetailField } from '@/components/common/DetailView.vue'
import { useAsyncData } from '@/composables/useAsyncData'
import { roomsService } from '@/services/rooms'

const route = useRoute()
const id = computed(() => Number(route.params.id))

const { data: room, loading, error } = useAsyncData(
	() => roomsService.getById(id.value),
)

const fields = computed<DetailField[]>(() => [
	{ label: 'Building', value: room.value?.buildingName },
	{ label: 'Capacity', value: room.value?.capacity },
	{ label: 'Type', value: room.value?.type },
	{ label: 'Availability', value: room.value?.availabilityStatus.replace(/_/g, ' ') },
	{ label: 'Feature Tags', value: room.value?.featureSet.join(', ') || room.value?.features || '-' },
	{ label: 'Accessibility Flags', value: room.value?.accessibilityFlags.join(', ') || '-' },
	{ label: 'Last Inspection', value: room.value?.lastInspectionDate || '-' },
	{ label: 'Operational Notes', value: room.value?.operationalNotes || '-' },
])
</script>

<template>
	<DetailView :title="`Room ${room?.roomNumber || ''}`" :fields="fields" :loading="loading" :error="error"
		back-route="/rooms" back-label="Back to Rooms" :edit-route="`/rooms/${id}/edit`" />
</template>
