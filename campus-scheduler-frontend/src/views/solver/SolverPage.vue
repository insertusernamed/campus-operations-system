<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useSolverWebSocket } from '@/composables/useSolverWebSocket'
import { solverService } from '@/services/solver'
import { generatorService } from '@/services/generator'
import { toast } from 'vue3-toastify'

const router = useRouter()

const semester = ref('Fall 2026')
const semesters = ['Fall 2026', 'Spring 2026', 'Fall 2025']

const { progress, connected, error: wsError } = useSolverWebSocket()

const isLoading = ref(false)
const isGenerating = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')

const isSolving = computed(() => progress.value?.status === 'SOLVING_ACTIVE')

async function generateData() {
	isGenerating.value = true
	errorMessage.value = ''
	statusMessage.value = ''
	try {
		const result = await generatorService.generateSmall()
		statusMessage.value = `Generated: ${result.buildings} buildings, ${result.rooms} rooms, ${result.instructors} instructors, ${result.courses} courses`
	} catch (e: unknown) {
		errorMessage.value = e instanceof Error ? e.message : 'Failed to generate data'
		toast.error(errorMessage.value)
	} finally {
		isGenerating.value = false
	}
}

async function clearData() {
	if (!confirm('Clear all generated data?')) return
	isLoading.value = true
	errorMessage.value = ''
	statusMessage.value = ''
	try {
		await generatorService.reset()
		statusMessage.value = 'All data cleared'
	} catch (e: unknown) {
		errorMessage.value = e instanceof Error ? e.message : 'Failed to clear data'
		toast.error(errorMessage.value)
	} finally {
		isLoading.value = false
	}
}

async function startSolver() {
	isLoading.value = true
	errorMessage.value = ''
	statusMessage.value = ''
	try {
		const result = await solverService.start(semester.value)
		statusMessage.value = result.message
	} catch (e: unknown) {
		errorMessage.value = e instanceof Error ? e.message : 'Failed to start solver'
		toast.error(errorMessage.value)
	} finally {
		isLoading.value = false
	}
}

async function stopSolver() {
	isLoading.value = true
	try {
		await solverService.stop()
		statusMessage.value = 'Solver stopped'
	} catch (e: unknown) {
		errorMessage.value = e instanceof Error ? e.message : 'Failed to stop solver'
		toast.error(errorMessage.value)
	} finally {
		isLoading.value = false
	}
}

async function saveSolution() {
	isLoading.value = true
	try {
		const result = await solverService.save()
		statusMessage.value = result.message
		toast.success('Solution saved successfully!', {
			onClick: () => router.push('/schedules'),
			style: { cursor: 'pointer' },
		})
	} catch (e: unknown) {
		errorMessage.value = e instanceof Error ? e.message : 'Failed to save solution'
		toast.error('Failed to save solution')
	} finally {
		isLoading.value = false
	}
}

const progressPercent = computed(() => {
	if (!progress.value || progress.value.totalCourses === 0) return 0
	return Math.round((progress.value.assignedCourses / progress.value.totalCourses) * 100)
})
</script>

<template>
	<div>
		<h1 class="text-2xl font-bold mb-6">Auto-Scheduler</h1>

		<!-- Connection Status -->
		<div class="mb-4 text-sm">
			<span :class="connected ? 'text-green-600' : 'text-red-600'">
				{{ connected ? 'Connected' : 'Disconnected' }}
			</span>
			<div v-if="wsError" class="text-red-600 mt-1">
				WebSocket Error: {{ wsError }}
			</div>
		</div>

		<!-- Data Generator Panel -->
		<div class="border p-4 mb-6">
			<h2 class="font-semibold mb-3">Data Generator</h2>
			<div class="flex gap-3">
				<button @click="generateData" :disabled="isGenerating"
					class="px-4 py-2 bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-50">
					{{ isGenerating ? 'Generating...' : 'Generate Demo Data' }}
				</button>
				<button @click="clearData" :disabled="isLoading"
					class="px-4 py-2 border border-red-500 text-red-600 hover:bg-red-50 disabled:opacity-50">
					Clear All Data
				</button>
			</div>
		</div>

		<!-- Solver Controls -->
		<div class="border p-4 mb-6">
			<h2 class="font-semibold mb-3">Solver Controls</h2>
			<div class="flex items-center gap-4 mb-4">
				<div>
					<label class="text-sm mr-2">Semester:</label>
					<select v-model="semester" class="border px-2 py-1" :disabled="isSolving">
						<option v-for="sem in semesters" :key="sem" :value="sem">{{ sem }}</option>
					</select>
				</div>
			</div>
			<div class="flex gap-3">
				<button v-if="!isSolving" @click="startSolver" :disabled="isLoading"
					class="px-4 py-2 bg-green-600 text-white hover:bg-green-700 disabled:opacity-50">
					Start Solver
				</button>
				<button v-else @click="stopSolver" :disabled="isLoading"
					class="px-4 py-2 bg-red-600 text-white hover:bg-red-700 disabled:opacity-50">
					Stop Solver
				</button>
				<button @click="saveSolution" :disabled="isLoading || isSolving"
					class="px-4 py-2 border hover:bg-gray-50 disabled:opacity-50">
					Save Solution
				</button>
			</div>
		</div>

		<!-- Status Messages -->
		<div v-if="statusMessage"
			class="border border-green-300 bg-green-50 p-3 mb-4 text-green-800 flex justify-between items-center">
			<span>{{ statusMessage }}</span>
			<RouterLink v-if="statusMessage.includes('Saved')" to="/schedules" class="underline font-bold ml-4">
				View Schedule &rarr;
			</RouterLink>
		</div>
		<div v-if="errorMessage" class="border border-red-300 bg-red-50 p-3 mb-4 text-red-800">
			{{ errorMessage }}
		</div>

		<!-- Progress Display -->
		<div v-if="progress" class="border p-4 mb-6">
			<h2 class="font-semibold mb-3">Solver Progress</h2>
			<div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
				<div class="text-center p-3 bg-gray-50">
					<div class="text-2xl font-bold">{{ progress.status === 'SOLVING_ACTIVE' ? 'Running' : 'Idle' }}
					</div>
					<div class="text-sm text-gray-500">Status</div>
				</div>
				<div class="text-center p-3 bg-gray-50">
					<div class="text-2xl font-bold">{{ progress.assignedCourses }}/{{ progress.totalCourses }}</div>
					<div class="text-sm text-gray-500">Assigned</div>
				</div>
				<div class="text-center p-3 bg-gray-50">
					<div v-tooltip="{ content: 'Critical constraint violations that must be fixed (e.g., two classes in the same room at the same time). Must be 0 for a valid schedule.', distance: 8 }"
						class="text-2xl font-bold cursor-help"
						:class="progress.hardViolations > 0 ? 'text-red-600' : 'text-green-600'">
						{{ progress.hardViolations }}
					</div>
					<div class="text-sm text-gray-500">Hard Violations</div>
				</div>
				<div class="text-center p-3 bg-gray-50">
					<div v-tooltip="{ content: 'Solution quality score (format: Xhard/Ysoft). Hard score must be 0 for validity. Lower soft score = better optimization.', distance: 8 }"
						class="text-2xl font-bold cursor-help">
						{{ progress.score || 'N/A' }}
					</div>
					<div class="text-sm text-gray-500">Score</div>
				</div>
			</div>

			<!-- Progress Bar -->
			<div class="h-4 bg-gray-200 overflow-hidden">
				<div class="h-full bg-blue-600 transition-all duration-300" :style="{ width: progressPercent + '%' }">
				</div>
			</div>
			<div class="text-sm text-gray-500 mt-1">{{ progressPercent }}% assigned</div>

			<!-- Latest Message -->
			<div v-if="progress.message" class="mt-3 text-sm text-gray-600 italic">
				{{ progress.message }}
			</div>
		</div>

		<!-- Empty State -->
		<div v-else class="border p-8 text-center text-gray-500">
			No solver data. Generate data and start the solver to see progress.
		</div>
	</div>
</template>
