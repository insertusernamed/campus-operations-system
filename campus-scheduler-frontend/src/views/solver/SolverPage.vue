<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSolverWebSocket } from '@/composables/useSolverWebSocket'
import { solverService } from '@/services/solver'
import {
	generatorService,
	type UniversityStats,
	type ArchetypeInfo,
	type GenerationPreview,
} from '@/services/generator'
import { toast } from 'vue3-toastify'
import ResearchModal from '@/components/generator/ResearchModal.vue'

const router = useRouter()

const semester = ref('Fall 2026')
const semesters = ['Fall 2026', 'Spring 2026', 'Fall 2025']

const { progress, connected, error: wsError } = useSolverWebSocket()

const isLoading = ref(false)
const isGenerating = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const stats = ref<UniversityStats>({
	buildings: 0,
	rooms: 0,
	instructors: 0,
	courses: 0,
	schedules: 0,
})

// Research-based generator state
const archetypes = ref<ArchetypeInfo[]>([])
const selectedArchetype = ref<'METROPOLIS' | 'CAMPUS_SPRAWL' | 'COMMUNITY'>('COMMUNITY')
const studentPopulation = ref(8000)
const preview = ref<GenerationPreview | null>(null)
const showResearchModal = ref(false)

const selectedArchetypeInfo = computed(() => {
	return archetypes.value.find((a) => a.id === selectedArchetype.value)
})

const isSolving = computed(() => progress.value?.status === 'SOLVING_ACTIVE')

async function fetchStats() {
	try {
		stats.value = await generatorService.getStats()
	} catch (e) {
		console.error('Failed to fetch stats', e)
	}
}

async function fetchArchetypes() {
	try {
		archetypes.value = await generatorService.getArchetypes()
	} catch (e) {
		console.error('Failed to fetch archetypes', e)
	}
}

async function updatePreview() {
	try {
		preview.value = await generatorService.previewGeneration({
			archetype: selectedArchetype.value,
			studentPopulation: studentPopulation.value,
		})
	} catch (e) {
		console.error('Failed to get preview', e)
	}
}

// Update preview when archetype or population changes
watch([selectedArchetype, studentPopulation], updatePreview, { immediate: false })

// Clamp student population to archetype limits
watch(selectedArchetype, () => {
	const arch = selectedArchetypeInfo.value
	if (arch) {
		if (studentPopulation.value < arch.minStudents) {
			studentPopulation.value = arch.minStudents
		} else if (studentPopulation.value > arch.maxStudents) {
			studentPopulation.value = arch.maxStudents
		}
	}
})

onMounted(async () => {
	await fetchStats()
	await fetchArchetypes()
	// Only call updatePreview after archetypes are loaded
	if (archetypes.value.length > 0) {
		await updatePreview()
	}
})

async function generateData() {
	isGenerating.value = true
	errorMessage.value = ''
	statusMessage.value = ''
	try {
		const result = await generatorService.generateWithArchetype({
			archetype: selectedArchetype.value,
			studentPopulation: studentPopulation.value,
		})
		statusMessage.value = `Generated: ${result.buildings} buildings, ${result.rooms} rooms, ${result.instructors} instructors, ${result.courses} courses`
		await fetchStats()
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
		await fetchStats()
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

// Solution quality based on assignment percentage
const solutionQuality = computed(() => {
	if (!progress.value) return null

	const assigned = progress.value.assignedCourses || 0
	const total = progress.value.totalCourses || 0
	const hardViolations = progress.value.hardViolations || 0

	if (total === 0) return null

	// Hard violations take priority
	if (hardViolations > 0) {
		return { label: 'Conflicts', class: 'bg-red-100 text-red-800' }
	}

	const pct = (assigned / total) * 100

	if (pct >= 100) return { label: 'Complete', class: 'bg-green-100 text-green-800' }
	if (pct >= 90) return { label: 'Nearly Done', class: 'bg-green-50 text-green-700' }
	if (pct >= 70) return { label: 'Partial', class: 'bg-yellow-100 text-yellow-700' }
	if (pct >= 50) return { label: 'Incomplete', class: 'bg-orange-100 text-orange-700' }
	return { label: 'Early', class: 'bg-gray-100 text-gray-600' }
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

		<!-- Current Statistics -->
		<div class="grid grid-cols-2 md:grid-cols-5 gap-4 mb-6">
			<div
				class="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center text-center">
				<div class="text-gray-500 text-sm">Buildings</div>
				<div class="text-2xl font-bold">{{ stats.buildings }}</div>
			</div>
			<div
				class="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center text-center">
				<div class="text-gray-500 text-sm">Rooms</div>
				<div class="text-2xl font-bold">{{ stats.rooms }}</div>
			</div>
			<div
				class="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center text-center">
				<div class="text-gray-500 text-sm">Instructors</div>
				<div class="text-2xl font-bold">{{ stats.instructors }}</div>
			</div>
			<div
				class="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center text-center">
				<div class="text-gray-500 text-sm">Courses</div>
				<div class="text-2xl font-bold">{{ stats.courses }}</div>
			</div>
			<div
				class="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col items-center justify-center text-center">
				<div class="text-gray-500 text-sm">Scheduled Classes</div>
				<div class="text-2xl font-bold">{{ stats.schedules }}</div>
			</div>
		</div>

		<!-- Data Generator Panel -->
		<div class="border p-4 mb-6">
			<div class="flex items-center justify-between mb-4">
				<h2 class="font-semibold">Data Generator</h2>
				<button @click="showResearchModal = true" class="text-sm text-blue-600 hover:underline">
					View Research Data
				</button>
			</div>

			<!-- Archetype Selection -->
			<div class="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4" role="radiogroup"
				aria-label="University archetype selection">
				<button v-for="arch in archetypes" :key="arch.id" role="radio"
					:aria-checked="selectedArchetype === arch.id"
					@click="selectedArchetype = arch.id as typeof selectedArchetype" :disabled="isGenerating" :class="[
						'p-3 border text-left transition-colors',
						selectedArchetype === arch.id
							? 'border-blue-500 bg-blue-50'
							: 'border-gray-200 hover:border-gray-300',
					]">
					<div class="font-medium text-sm">{{ arch.displayName }}</div>
					<div class="text-xs text-gray-500 mt-1">{{ arch.description }}</div>
					<div class="text-xs text-gray-400 mt-1">
						{{ arch.minStudents.toLocaleString() }} - {{ arch.maxStudents.toLocaleString() }} students
					</div>
				</button>
			</div>

			<!-- Student Population Slider -->
			<div class="mb-4" v-if="selectedArchetypeInfo">
				<label class="block text-sm font-medium text-gray-700 mb-2">
					Student Population: {{ studentPopulation.toLocaleString() }}
				</label>
				<input type="range" v-model.number="studentPopulation" :min="selectedArchetypeInfo.minStudents"
					:max="selectedArchetypeInfo.maxStudents" :step="1000" :disabled="isGenerating"
					aria-label="Student population" :aria-valuetext="`${studentPopulation.toLocaleString()} students`"
					class="w-full h-2 bg-gray-200 rounded-lg cursor-pointer" />
				<div class="flex justify-between text-xs text-gray-400 mt-1">
					<span>{{ selectedArchetypeInfo.minStudents.toLocaleString() }}</span>
					<span>{{ selectedArchetypeInfo.maxStudents.toLocaleString() }}</span>
				</div>
			</div>

			<!-- Preview -->
			<div v-if="preview" class="bg-gray-50 p-3 mb-4 text-sm">
				<div class="grid grid-cols-2 md:grid-cols-5 gap-2 text-center">
					<div>
						<div class="font-semibold">{{ preview.totalBuildings }}</div>
						<div class="text-xs text-gray-500">Buildings</div>
					</div>
					<div>
						<div class="font-semibold">{{ preview.totalRooms }}</div>
						<div class="text-xs text-gray-500">Rooms</div>
					</div>
					<div>
						<div class="font-semibold">{{ preview.instructors }}</div>
						<div class="text-xs text-gray-500">Instructors</div>
					</div>
					<div>
						<div class="font-semibold">{{ preview.courses }}</div>
						<div class="text-xs text-gray-500">Courses</div>
					</div>
					<div class="col-span-2 md:col-span-1">
						<div class="font-semibold">{{ preview.studentPopulation.toLocaleString() }}</div>
						<div class="text-xs text-gray-500">Students</div>
					</div>
				</div>
			</div>

			<!-- Actions -->
			<div class="flex items-center gap-3">
				<button @click="generateData" :disabled="isGenerating"
					class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 min-w-35">
					{{ isGenerating ? 'Generating...' : 'Generate Data' }}
				</button>
				<button @click="clearData" :disabled="isLoading"
					class="px-4 py-2 border border-red-500 text-red-600 rounded hover:bg-red-50 disabled:opacity-50">
					Clear All
				</button>
			</div>
		</div>

		<!-- Research Modal -->
		<ResearchModal v-model="showResearchModal" />

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
					<span v-if="solutionQuality" class="inline-block mt-1 px-2 py-0.5 text-xs font-medium rounded"
						:class="solutionQuality.class">
						{{ solutionQuality.label }}
					</span>
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
