<script setup lang="ts">
import { RouterLink } from 'vue-router'
import FormSkeleton from './FormSkeleton.vue'

export type FieldType = 'text' | 'email' | 'number' | 'select' | 'textarea' | 'time'

export interface FormField {
	name: string
	label: string
	type: FieldType
	required?: boolean
	placeholder?: string
	maxLength?: number
	min?: number
	max?: number
	rows?: number
	disabled?: boolean
	helpText?: string
	/** For select fields */
	options?: { value: string | number | null; label: string }[]
	/** Grid column span (1 or 2, default 1) */
	span?: 1 | 2
}

const props = defineProps<{
	title: string
	fields: FormField[]
	modelValue: Record<string, unknown>
	loading?: boolean
	saving?: boolean
	error: string | null
	backRoute: string
	backLabel?: string
	submitLabel?: string
}>()

const emit = defineEmits<{
	(e: 'update:modelValue', value: Record<string, unknown>): void
	(e: 'submit'): void
}>()

function updateField(name: string, value: unknown) {
	emit('update:modelValue', { ...props.modelValue, [name]: value })
}
</script>

<template>
	<div>
		<div class="mb-6">
			<RouterLink :to="backRoute" class="text-blue-600 hover:underline text-sm">
				{{ backLabel || 'Back' }}
			</RouterLink>
		</div>

		<div class="bg-white border border-gray-200 p-6 max-w-xl">
			<h1 class="text-2xl font-semibold text-gray-900 mb-6">{{ title }}</h1>

			<FormSkeleton v-if="loading" :fields="fields.length" />

			<form v-else @submit.prevent="$emit('submit')" class="space-y-4">
				<div v-if="error" class="p-3 bg-red-50 border border-red-200 text-red-600 rounded">
					{{ error }}
				</div>

				<div class="grid grid-cols-2 gap-4">
					<div v-for="field in fields" :key="field.name"
						:class="field.span === 2 ? 'col-span-2' : 'col-span-1'">
						<label :for="field.name" class="block text-sm font-medium text-gray-700 mb-1">
							{{ field.label }}
							<span v-if="field.required" class="text-red-500">*</span>
						</label>

						<!-- Select -->
						<select v-if="field.type === 'select'" :id="field.name" :aria-label="field.label"
							:value="modelValue[field.name]"
							@change="updateField(field.name, ($event.target as HTMLSelectElement).value || null)"
							:required="field.required" :disabled="field.disabled"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100">
							<option v-for="opt in field.options" :key="String(opt.value)" :value="opt.value">
								{{ opt.label }}
							</option>
						</select>

						<!-- Textarea -->
						<textarea v-else-if="field.type === 'textarea'" :id="field.name"
							:value="modelValue[field.name] as string"
							@input="updateField(field.name, ($event.target as HTMLTextAreaElement).value)"
							:required="field.required" :placeholder="field.placeholder" :maxlength="field.maxLength"
							:rows="field.rows || 3"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"></textarea>

						<!-- Input (text, email, number, time) -->
						<input v-else :id="field.name" :type="field.type" :value="modelValue[field.name]"
							@input="updateField(field.name, field.type === 'number' ? Number(($event.target as HTMLInputElement).value) : ($event.target as HTMLInputElement).value)"
							:required="field.required" :placeholder="field.placeholder" :maxlength="field.maxLength"
							:min="field.min" :max="field.max" :disabled="field.disabled"
							class="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100" />

						<p v-if="field.helpText" class="text-xs text-gray-500 mt-1">{{ field.helpText }}</p>
					</div>
				</div>

				<div class="flex gap-4 pt-4">
					<button type="submit" :disabled="saving"
						class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
						{{ saving ? 'Saving...' : (submitLabel || 'Save') }}
					</button>
					<RouterLink :to="backRoute" class="px-4 py-2 border border-gray-300 rounded hover:bg-gray-50">
						Cancel
					</RouterLink>
				</div>
			</form>
		</div>
	</div>
</template>
