<script setup lang="ts">
import { computed, watch, onUnmounted } from 'vue'

const props = withDefaults(
	defineProps<{
		modelValue: boolean
		title?: string
		size?: 'sm' | 'md' | 'lg' | 'xl' | 'full'
		closeOnOverlay?: boolean
		closeOnEscape?: boolean
		showClose?: boolean
	}>(),
	{
		size: 'md',
		closeOnOverlay: true,
		closeOnEscape: true,
		showClose: true,
	}
)

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void
	(e: 'close'): void
}>()

const isOpen = computed({
	get: () => props.modelValue,
	set: (value) => {
		emit('update:modelValue', value)
		if (!value) emit('close')
	},
})

const sizeClasses = computed(() => {
	switch (props.size) {
		case 'sm':
			return 'max-w-md'
		case 'md':
			return 'max-w-2xl'
		case 'lg':
			return 'max-w-4xl'
		case 'xl':
			return 'max-w-6xl'
		case 'full':
			return 'max-w-[95vw]'
		default:
			return 'max-w-2xl'
	}
})

function closeModal() {
	isOpen.value = false
}

function handleOverlayClick() {
	if (props.closeOnOverlay) {
		closeModal()
	}
}

function handleKeydown(e: KeyboardEvent) {
	if (e.key === 'Escape' && props.closeOnEscape && isOpen.value) {
		closeModal()
	}
}

watch(
	() => props.modelValue,
	(open) => {
		if (open) {
			window.addEventListener('keydown', handleKeydown)
		} else {
			window.removeEventListener('keydown', handleKeydown)
		}
	},
	{ immediate: true }
)

onUnmounted(() => {
	document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
	<Teleport to="body">
		<Transition name="modal">
			<div v-if="isOpen" class="modal-overlay">
				<button v-if="props.closeOnOverlay" type="button" class="modal-overlay-dismiss"
					aria-label="Close modal overlay" @click="handleOverlayClick"></button>
				<div class="modal-container" :class="sizeClasses" role="dialog" aria-modal="true"
					aria-labelledby="modal-title">
					<!-- Header -->
					<div v-if="title || $slots.header || showClose" class="modal-header">
						<slot name="header">
							<h2 v-if="title" id="modal-title" class="modal-title">{{ title }}</h2>
						</slot>
						<button v-if="showClose" class="close-btn" @click="closeModal" aria-label="Close modal">
							<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
								fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"
								stroke-linejoin="round">
								<line x1="18" y1="6" x2="6" y2="18"></line>
								<line x1="6" y1="6" x2="18" y2="18"></line>
							</svg>
						</button>
					</div>

					<!-- Body -->
					<div class="modal-body">
						<slot></slot>
					</div>

					<!-- Footer -->
					<div v-if="$slots.footer" class="modal-footer">
						<slot name="footer"></slot>
					</div>
				</div>
			</div>
		</Transition>
	</Teleport>
</template>

<style scoped>
.modal-overlay {
	position: fixed;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	background: rgba(0, 0, 0, 0.5);
	display: flex;
	justify-content: center;
	align-items: center;
	z-index: 1000;
	backdrop-filter: blur(4px);
	padding: 1rem;
}

.modal-overlay-dismiss {
	position: absolute;
	inset: 0;
	border: 0;
	padding: 0;
	margin: 0;
	background: transparent;
}

.modal-container {
	background: var(--color-background, #fff);
	border-radius: 12px;
	width: 100%;
	max-height: 85vh;
	position: relative;
	z-index: 1;
	display: flex;
	flex-direction: column;
	box-shadow:
		0 20px 25px -5px rgba(0, 0, 0, 0.1),
		0 10px 10px -5px rgba(0, 0, 0, 0.04);
	overflow: hidden;
}

.modal-header {
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 1.25rem 1.5rem;
	border-bottom: 1px solid var(--color-border, #e5e7eb);
	flex-shrink: 0;
}

.modal-title {
	margin: 0;
	font-size: 1.25rem;
	font-weight: 600;
	color: var(--color-heading, #111827);
}

.close-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	width: 36px;
	height: 36px;
	padding: 0;
	background: transparent;
	border: none;
	border-radius: 8px;
	cursor: pointer;
	color: var(--color-text-mute, #6b7280);
	transition: all 0.15s ease;
}

.close-btn:hover {
	background: var(--color-background-mute, #f3f4f6);
	color: var(--color-heading, #111827);
}

.close-btn:focus {
	outline: 2px solid var(--vt-c-green, #42b883);
	outline-offset: 2px;
}

.modal-body {
	padding: 1.5rem;
	overflow-y: auto;
	flex: 1;
}

.modal-footer {
	display: flex;
	justify-content: flex-end;
	gap: 0.75rem;
	padding: 1rem 1.5rem;
	border-top: 1px solid var(--color-border, #e5e7eb);
	flex-shrink: 0;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
	transition: opacity 0.2s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
	transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
	opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
	transform: scale(0.95) translateY(10px);
}
</style>
