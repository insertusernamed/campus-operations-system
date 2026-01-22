<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'

export interface BarChartData {
    label: string
    value: number
}

const props = withDefaults(
    defineProps<{
        data: BarChartData[]
        title?: string
        height?: number
        maxValue?: number
        showValues?: boolean
        valueFormatter?: (value: number) => string
    }>(),
    {
        height: 300,
        showValues: true,
    }
)

const canvas = ref<HTMLCanvasElement | null>(null)

const defaultFormatter = (value: number) => `${value.toFixed(1)}%`
const formatter = computed(() => props.valueFormatter || defaultFormatter)

function draw() {
    if (!canvas.value || props.data.length === 0) return

    const ctx = canvas.value.getContext('2d')
    if (!ctx) return

    const dpr = window.devicePixelRatio || 1
    const rect = canvas.value.getBoundingClientRect()
    canvas.value.width = rect.width * dpr
    canvas.value.height = rect.height * dpr
    ctx.scale(dpr, dpr)

    const width = rect.width
    const height = rect.height
    const padding = { top: 20, right: 20, bottom: 60, left: 50 }
    const chartWidth = width - padding.left - padding.right
    const chartHeight = height - padding.top - padding.bottom

    ctx.clearRect(0, 0, width, height)

    const max = props.maxValue ?? Math.max(...props.data.map((d) => d.value), 1)

    // Grid lines
    ctx.strokeStyle = '#ccc'
    ctx.lineWidth = 1
    for (let i = 0; i <= 4; i++) {
        const y = padding.top + (chartHeight / 4) * i
        ctx.beginPath()
        ctx.moveTo(padding.left, y)
        ctx.lineTo(width - padding.right, y)
        ctx.stroke()

        ctx.fillStyle = '#666'
        ctx.font = '11px sans-serif'
        ctx.textAlign = 'right'
        const value = max - (max / 4) * i
        ctx.fillText(formatter.value(value), padding.left - 8, y + 4)
    }

    // Bars
    const barWidth = Math.min(50, (chartWidth / props.data.length) * 0.7)
    const gap = (chartWidth - barWidth * props.data.length) / (props.data.length + 1)

    props.data.forEach((item, index) => {
        const x = padding.left + gap + index * (barWidth + gap)
        const barHeight = (item.value / max) * chartHeight
        const y = padding.top + chartHeight - barHeight

        ctx.fillStyle = '#4a90d9'
        ctx.fillRect(x, y, barWidth, barHeight)

        if (props.showValues && item.value > 0) {
            ctx.fillStyle = '#333'
            ctx.font = '11px sans-serif'
            ctx.textAlign = 'center'
            ctx.fillText(formatter.value(item.value), x + barWidth / 2, y - 5)
        }

        ctx.fillStyle = '#666'
        ctx.font = '11px sans-serif'
        ctx.textAlign = 'center'
        ctx.save()
        ctx.translate(x + barWidth / 2, height - padding.bottom + 15)
        ctx.rotate(-Math.PI / 6)
        ctx.fillText(truncateLabel(item.label, 12), 0, 0)
        ctx.restore()
    })
}

function truncateLabel(label: string, maxLength: number): string {
    return label.length > maxLength ? label.slice(0, maxLength - 1) + '…' : label
}

onMounted(draw)
watch(() => props.data, draw, { deep: true })
</script>

<template>
    <div>
        <h3 v-if="title" class="font-semibold mb-2">{{ title }}</h3>
        <div v-if="data.length === 0" class="text-gray-500 py-8 text-center">No data</div>
        <canvas v-else ref="canvas" :style="{ width: '100%', height: `${height}px` }"></canvas>
    </div>
</template>
