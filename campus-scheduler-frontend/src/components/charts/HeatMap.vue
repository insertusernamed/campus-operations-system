<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'

export interface HeatmapCell {
	row: string
	col: string
	value: number
	label?: string
}

const props = withDefaults(
	defineProps<{
		data: HeatmapCell[]
		rows: string[]
		cols: string[]
		title?: string
		minValue?: number
		maxValue?: number
		cellWidth?: number
		cellHeight?: number
		showValues?: boolean
		valueFormatter?: (value: number) => string
	}>(),
	{
		cellWidth: 60,
		cellHeight: 40,
		showValues: true,
	}
)

const emit = defineEmits<{
	cellClick: [cell: HeatmapCell]
}>()

const canvas = ref<HTMLCanvasElement | null>(null)
const tooltip = ref<{ visible: boolean; x: number; y: number; content: string }>({
	visible: false,
	x: 0,
	y: 0,
	content: '',
})

const defaultFormatter = (value: number) => value.toString()
const formatter = computed(() => props.valueFormatter || defaultFormatter)

const padding = { top: 40, right: 20, bottom: 20, left: 80 }

const cellMap = computed(() => {
	const map = new Map<string, HeatmapCell>()
	props.data.forEach((cell) => {
		map.set(`${cell.row}-${cell.col}`, cell)
	})
	return map
})

function getColor(value: number, min: number, max: number): string {
	const normalized = max > min ? (value - min) / (max - min) : 0
	// Green to yellow to red gradient with varying luminance for accessibility
	// Low values: green (hue 120), High values: red (hue 0)
	// Luminance also decreases as value increases for colorblind accessibility
	const hue = 120 - normalized * 120 // 120 (green) -> 0 (red)
	const saturation = 50 + normalized * 20 // 50% -> 70%
	const lightness = 85 - normalized * 35 // 85% (light) -> 50% (darker)
	return `hsl(${hue}, ${saturation}%, ${lightness}%)`
}

function draw() {
	if (!canvas.value) return

	const ctx = canvas.value.getContext('2d')
	if (!ctx) return

	const dpr = window.devicePixelRatio || 1
	const width = padding.left + props.cols.length * props.cellWidth + padding.right
	const height = padding.top + props.rows.length * props.cellHeight + padding.bottom

	canvas.value.width = width * dpr
	canvas.value.height = height * dpr
	canvas.value.style.width = `${width}px`
	canvas.value.style.height = `${height}px`
	ctx.scale(dpr, dpr)

	ctx.clearRect(0, 0, width, height)

	const values = props.data.map((d) => d.value)
	const autoMin = values.length > 0 ? Math.min(...values) : 0
	const autoMax = values.length > 0 ? Math.max(...values) : 1
	const min = props.minValue ?? autoMin
	const max = props.maxValue ?? autoMax
	const effectiveMax = max > min ? max : min + 1

	// Column headers
	ctx.fillStyle = '#333'
	ctx.font = '11px sans-serif'
	ctx.textAlign = 'center'
	props.cols.forEach((col, i) => {
		const x = padding.left + i * props.cellWidth + props.cellWidth / 2
		ctx.fillText(col, x, padding.top - 10)
	})

	// Row headers
	ctx.textAlign = 'right'
	props.rows.forEach((row, i) => {
		const y = padding.top + i * props.cellHeight + props.cellHeight / 2 + 4
		ctx.fillText(row, padding.left - 10, y)
	})

	// Cells
	props.rows.forEach((row, rowIndex) => {
		props.cols.forEach((col, colIndex) => {
			const cell = cellMap.value.get(`${row}-${col}`)
			const x = padding.left + colIndex * props.cellWidth
			const y = padding.top + rowIndex * props.cellHeight

			if (cell) {
				ctx.fillStyle = getColor(cell.value, min, effectiveMax)
			} else {
				ctx.fillStyle = '#f5f5f5'
			}
			ctx.fillRect(x + 1, y + 1, props.cellWidth - 2, props.cellHeight - 2)

			if (props.showValues && cell && cell.value > 0) {
				const normalized = (cell.value - min) / (effectiveMax - min)
				ctx.fillStyle = normalized > 0.6 ? '#fff' : '#333'
				ctx.font = '11px sans-serif'
				ctx.textAlign = 'center'
				ctx.fillText(
					formatter.value(cell.value),
					x + props.cellWidth / 2,
					y + props.cellHeight / 2 + 4
				)
			}
		})
	})
}

function handleMouseMove(event: MouseEvent) {
	if (!canvas.value) return

	const rect = canvas.value.getBoundingClientRect()
	const x = event.clientX - rect.left
	const y = event.clientY - rect.top

	const colIndex = Math.floor((x - padding.left) / props.cellWidth)
	const rowIndex = Math.floor((y - padding.top) / props.cellHeight)

	if (
		colIndex >= 0 &&
		colIndex < props.cols.length &&
		rowIndex >= 0 &&
		rowIndex < props.rows.length
	) {
		const row = props.rows[rowIndex]
		const col = props.cols[colIndex]
		const cell = cellMap.value.get(`${row}-${col}`)

		if (cell) {
			tooltip.value = {
				visible: true,
				x: event.clientX - rect.left + 10,
				y: event.clientY - rect.top - 10,
				content: cell.label || `${row} - ${col}: ${formatter.value(cell.value)}`,
			}
			return
		}
	}

	tooltip.value.visible = false
}

function handleMouseLeave() {
	tooltip.value.visible = false
}

function handleClick(event: MouseEvent) {
	if (!canvas.value) return

	const rect = canvas.value.getBoundingClientRect()
	const x = event.clientX - rect.left
	const y = event.clientY - rect.top

	const colIndex = Math.floor((x - padding.left) / props.cellWidth)
	const rowIndex = Math.floor((y - padding.top) / props.cellHeight)

	if (
		colIndex >= 0 &&
		colIndex < props.cols.length &&
		rowIndex >= 0 &&
		rowIndex < props.rows.length
	) {
		const row = props.rows[rowIndex]
		const col = props.cols[colIndex]
		const cell = cellMap.value.get(`${row}-${col}`)

		if (cell) {
			emit('cellClick', cell)
		}
	}
}

onMounted(draw)
watch([() => props.data, () => props.rows, () => props.cols], draw, { deep: true })
</script>

<template>
	<div>
		<h3 v-if="title" class="font-semibold mb-2">{{ title }}</h3>
		<div v-if="data.length === 0" class="text-gray-500 py-8 text-center">No data</div>
		<div v-else class="overflow-x-auto relative">
			<canvas ref="canvas" role="img" :aria-label="title ? `${title} heatmap` : 'Schedule heatmap'"
				@mousemove="handleMouseMove" @mouseleave="handleMouseLeave" @click="handleClick"></canvas>
			<div v-if="tooltip.visible" class="absolute bg-black text-white text-xs px-2 py-1 pointer-events-none"
				:style="{ left: `${tooltip.x}px`, top: `${tooltip.y}px` }">
				{{ tooltip.content }}
			</div>
		</div>
	</div>
</template>
