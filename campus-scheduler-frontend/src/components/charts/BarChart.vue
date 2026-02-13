<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed } from 'vue'

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
		minBarWidth?: number
		barGap?: number
		minChartWidth?: number
		valueLabelDensityThreshold?: number
		xLabelMaxLength?: number
	}>(),
	{
		height: 300,
		showValues: true,
		minBarWidth: 24,
		barGap: 10,
		minChartWidth: 640,
		valueLabelDensityThreshold: 16,
		xLabelMaxLength: 12,
	}
)

const canvas = ref<HTMLCanvasElement | null>(null)

const padding = { top: 20, right: 20, bottom: 70, left: 50 }

const defaultFormatter = (value: number) => `${value.toFixed(1)}%`
const formatter = computed(() => props.valueFormatter || defaultFormatter)

const estimatedCanvasWidth = computed(() => {
	const barCount = Math.max(props.data.length, 1)
	const minChartAreaWidth = barCount * (props.minBarWidth + props.barGap) + props.barGap
	const requiredWidth = padding.left + padding.right + minChartAreaWidth
	return Math.max(props.minChartWidth, requiredWidth)
})

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
	const barCount = props.data.length
	const rawBarWidth = (chartWidth / barCount) * 0.62
	const barWidth = Math.min(50, Math.max(props.minBarWidth, rawBarWidth))
	const gap = Math.max(props.barGap, (chartWidth - barWidth * barCount) / (barCount + 1))
	const valueLabelStep =
		barCount <= props.valueLabelDensityThreshold
			? 1
			: Math.ceil(barCount / props.valueLabelDensityThreshold)

	props.data.forEach((item, index) => {
		const x = padding.left + gap + index * (barWidth + gap)
		const barHeight = (item.value / max) * chartHeight
		const y = padding.top + chartHeight - barHeight

		ctx.fillStyle = '#4a90d9'
		ctx.fillRect(x, y, barWidth, barHeight)

		if (props.showValues && item.value > 0 && index % valueLabelStep === 0) {
			ctx.save()
			ctx.font = '11px sans-serif'
			ctx.textAlign = 'center'
			ctx.textBaseline = 'bottom'
			const valueText = formatter.value(item.value)
			const textHalfWidth = ctx.measureText(valueText).width / 2
			const textOutlineWidth = 4
			const outlineHalf = Math.ceil(textOutlineWidth / 2)
			const valueX = x + barWidth / 2
			const clampedValueX = Math.min(
				width - padding.right - textHalfWidth - outlineHalf - 2,
				Math.max(padding.left + textHalfWidth + outlineHalf + 2, valueX)
			)
			const valueY = y - 6

			// Outlined text: avoids a filled background "box" that can cover bar pixels,
			// while preserving contrast over the bar or grid.
			ctx.lineJoin = 'round'
			ctx.miterLimit = 2
			ctx.strokeStyle = '#fff'
			ctx.lineWidth = textOutlineWidth
			ctx.strokeText(valueText, clampedValueX, valueY)
			ctx.fillStyle = '#333'
			ctx.fillText(valueText, clampedValueX, valueY)
			ctx.restore()
		}

		ctx.fillStyle = '#666'
		ctx.font = '11px sans-serif'
		ctx.textAlign = 'center'
		ctx.save()
		ctx.translate(x + barWidth / 2, height - padding.bottom + 20)
		ctx.rotate(-Math.PI / 4)
		ctx.fillText(truncateLabel(item.label, props.xLabelMaxLength), 0, 0)
		ctx.restore()
	})
}

function truncateLabel(label: string, maxLength: number): string {
	return label.length > maxLength ? label.slice(0, maxLength - 1) + '…' : label
}

function handleResize() {
	draw()
}

onMounted(() => {
	draw()
	window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
	window.removeEventListener('resize', handleResize)
})

watch(
	() => [
		props.data,
		props.height,
		props.maxValue,
		props.showValues,
		props.minBarWidth,
		props.barGap,
		props.minChartWidth,
		props.valueLabelDensityThreshold,
		props.xLabelMaxLength,
	],
	draw,
	{ deep: true }
)
</script>

<template>
	<div>
		<h3 v-if="title" class="font-semibold mb-2">{{ title }}</h3>
		<div v-if="data.length === 0" class="text-gray-500 py-8 text-center">No data</div>
		<div v-else class="overflow-x-auto pb-1">
			<canvas ref="canvas" role="img" :aria-label="title ? `${title} bar chart` : 'Bar chart'"
				:style="{ width: `${estimatedCanvasWidth}px`, minWidth: '100%', height: `${height}px` }"></canvas>
		</div>
	</div>
</template>
