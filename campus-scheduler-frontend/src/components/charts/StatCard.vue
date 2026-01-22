<script setup lang="ts">
defineProps<{
    label: string
    value: string | number
    sublabel?: string
}>()
</script>

<template>
    <div class="border p-4">
        <div class="text-sm text-gray-600">{{ label }}</div>
        <div class="text-2xl font-bold">{{ value }}</div>
        <div v-if="sublabel" class="text-sm text-gray-500">{{ sublabel }}</div>
    </div>
</template>
