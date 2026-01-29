<script setup lang="ts">
import { ref } from 'vue'
import BaseModal from '@/components/common/BaseModal.vue'

const props = defineProps<{
	modelValue: boolean
}>()

const emit = defineEmits<{
	(e: 'update:modelValue', value: boolean): void
}>()

const activeTab = ref<'overview' | 'archetypes' | 'ratios' | 'citations'>('overview')

const universityData = [
	{
		name: 'University of Toronto (St. George)',
		students: 70000,
		buildings: 120,
		courses: 10000,
		densityRatio: 583,
		utilizationRatio: 83,
		loadRatio: 7.0,
	},
	{
		name: 'UBC (Vancouver)',
		students: 59000,
		buildings: 473,
		courses: 10500,
		densityRatio: 125,
		utilizationRatio: 22,
		loadRatio: 5.6,
	},
	{
		name: 'McGill University',
		students: 39000,
		buildings: 100,
		courses: 8000,
		densityRatio: 390,
		utilizationRatio: 80,
		loadRatio: 4.9,
	},
	{
		name: 'University of Alberta',
		students: 38000,
		buildings: 150,
		courses: 8000,
		densityRatio: 253,
		utilizationRatio: 53,
		loadRatio: 4.8,
	},
	{
		name: 'University of Waterloo',
		students: 41000,
		buildings: 100,
		courses: 7500,
		densityRatio: 410,
		utilizationRatio: 75,
		loadRatio: 5.5,
	},
	{
		name: 'Lakehead University',
		students: 8000,
		buildings: 39,
		courses: 2200,
		densityRatio: 202,
		utilizationRatio: 56,
		loadRatio: 3.6,
	},
]

const archetypes = [
	{
		id: 'METROPOLIS',
		name: 'Urban Titan',
		description:
			'High-density urban campus with vertical architecture and intense utilization',
		studentsPerBuilding: 500,
		coursesPerBuilding: 80,
		studentsPerCourse: 7.0, // S/C ratio - higher means less course variety
		academicBuildingRatio: 0.7,
		examples: ['University of Toronto', 'McGill University', 'University of Waterloo'],
		characteristics: [
			'Dense building utilization (500 students per building)',
			'Multi-story, multi-department buildings',
			'High course-per-building ratio (80 courses per building)',
			'Urban integration with porous campus boundaries',
			'Typical enrollment: 40,000 - 80,000 students',
		],
	},
	{
		id: 'CAMPUS_SPRAWL',
		name: 'Research Park',
		description: 'Expansive campus with parkland feel and specialized research facilities',
		studentsPerBuilding: 200,
		coursesPerBuilding: 30,
		studentsPerCourse: 5.5, // S/C ratio
		academicBuildingRatio: 0.5,
		examples: ['University of British Columbia', 'University of Alberta'],
		characteristics: [
			'Low building density (200 students per building)',
			'Many specialized, single-purpose structures',
			'Lower course-per-building ratio (30 courses per building)',
			'Expansive land use, isolated campus feel',
			'Typical enrollment: 30,000 - 60,000 students',
		],
	},
	{
		id: 'COMMUNITY',
		name: 'Community Hub',
		description:
			'Accessible, interconnected campus with personalized attention and efficient space use',
		studentsPerBuilding: 200,
		coursesPerBuilding: 55,
		studentsPerCourse: 3.6, // S/C ratio - lower = more variety = better experience
		academicBuildingRatio: 0.6,
		examples: ['Lakehead University'],
		characteristics: [
			'Moderate building density (200 students per building)',
			'Efficient use of space, interconnected buildings',
			'Moderate course-per-building ratio (55 courses per building)',
			'High accessibility and personalized attention',
			'Typical enrollment: 5,000 - 15,000 students',
			'Often features tunnel/pedway systems for climate adaptation',
		],
	},
]

const citations = [
	{
		id: 1,
		source: 'University of Toronto',
		title: 'Quick facts | University of Toronto',
		url: 'https://www.utoronto.ca/about-u-of-t/quick-facts',
		accessDate: 'January 28, 2026',
	},
	{
		id: 2,
		source: 'Lakehead University',
		title: 'Facts & Figures | Lakehead University',
		url: 'https://www.lakeheadu.ca/research-and-innovation/about/facts-figures',
		accessDate: 'January 28, 2026',
	},
	{
		id: 3,
		source: 'McGill University',
		title: 'Maps and geospatial data | Facilities Management and Ancillary Services',
		url: 'https://www.mcgill.ca/facilities/resources/maps-and-geospatial-data',
		accessDate: 'January 28, 2026',
	},
	{
		id: 4,
		source: 'The Canadian Encyclopedia',
		title: 'University of British Columbia',
		url: 'https://thecanadianencyclopedia.ca/en/article/university-of-british-columbia',
		accessDate: 'January 28, 2026',
	},
	{
		id: 5,
		source: 'University of Alberta',
		title: 'About U of A',
		url: 'https://www.ualberta.ca/en/about/index.html',
		accessDate: 'January 28, 2026',
	},
]
</script>

<template>
	<BaseModal :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" size="lg">
		<template #header>
			<h2 class="modal-title">Research Data: University Generation Ratios</h2>
		</template>

		<div class="research-modal-content">
			<div class="modal-tabs">
				<button :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">
					Overview
				</button>
				<button :class="{ active: activeTab === 'archetypes' }" @click="activeTab = 'archetypes'">
					Archetypes
				</button>
				<button :class="{ active: activeTab === 'ratios' }" @click="activeTab = 'ratios'">
					Data & Ratios
				</button>
				<button :class="{ active: activeTab === 'citations' }" @click="activeTab = 'citations'">
					Citations
				</button>
			</div>

			<div class="tab-container">
				<!-- Overview Tab -->
				<div v-if="activeTab === 'overview'" class="tab-content">
					<div class="info-box">
						<h3>About This Research</h3>
						<p>
							This generator uses <strong>research-backed ratios</strong> derived from
							analysis of Canada's top universities. Rather than using arbitrary numbers, we
							model real institutional morphology.
						</p>
					</div>

					<div class="formula-section">
						<h3>The Golden Ratios</h3>
						<div class="formula-grid">
							<div class="formula-card">
								<div class="formula">
									<span class="variable">B</span> =
									<span class="variable">S</span> ÷
									<span class="constant">SPB</span>
								</div>
								<div class="formula-legend">
									<strong>Buildings</strong> = Students ÷ Students Per Building
								</div>
							</div>
							<div class="formula-card">
								<div class="formula">
									<span class="variable">C</span> =
									<span class="variable">B<sub>a</sub></span> ×
									<span class="constant">CPB</span>
								</div>
								<div class="formula-legend">
									<strong>Courses</strong> = Academic Buildings × Courses Per Building
								</div>
							</div>
							<div class="formula-card">
								<div class="formula">
									<span class="variable">I</span> =
									<span class="variable">C</span> ÷ <span class="constant">3</span>
								</div>
								<div class="formula-legend">
									<strong>Instructors</strong> = Courses ÷ 3 (avg. load)
								</div>
							</div>
						</div>
					</div>

					<div class="key-finding">
						<h3>Key Finding</h3>
						<p>
							The <strong>"Golden Ratio"</strong> of Canadian academia:
							<span class="highlight">300 students per building</span> (mean across all
							types)
						</p>
						<p>
							Academic diversity ratio:
							<span class="highlight">5 courses per student</span> (catalog potential)
						</p>
					</div>
				</div>

				<!-- Archetypes Tab -->
				<div v-if="activeTab === 'archetypes'" class="tab-content">
					<div v-for="archetype in archetypes" :key="archetype.id" class="archetype-card">
						<div class="archetype-header">
							<h3>{{ archetype.name }}</h3>
							<span class="archetype-badge">{{ archetype.id }}</span>
						</div>
						<p class="archetype-desc">{{ archetype.description }}</p>

						<div class="archetype-stats">
							<div class="stat">
								<span class="stat-value">{{ archetype.studentsPerBuilding }}</span>
								<span class="stat-label">Students/Building</span>
							</div>
							<div class="stat">
								<span class="stat-value">{{ archetype.coursesPerBuilding }}</span>
								<span class="stat-label">Courses/Building</span>
							</div>
							<div class="stat">
								<span class="stat-value">{{ archetype.studentsPerCourse }}</span>
								<span class="stat-label">S/C Ratio</span>
							</div>
							<div class="stat">
								<span class="stat-value">{{ (archetype.academicBuildingRatio * 100).toFixed(0)
								}}%</span>
								<span class="stat-label">Academic Buildings</span>
							</div>
						</div>

						<div class="archetype-examples">
							<strong>Based on:</strong>
							{{ archetype.examples.join(', ') }}
						</div>

						<ul class="characteristics">
							<li v-for="(char, idx) in archetype.characteristics" :key="idx">
								{{ char }}
							</li>
						</ul>
					</div>
				</div>

				<!-- Data & Ratios Tab -->
				<div v-if="activeTab === 'ratios'" class="tab-content">
					<h3>Comparative Data Matrix</h3>
					<div class="table-container">
						<table class="data-table">
							<thead>
								<tr>
									<th>University</th>
									<th>Students (S)</th>
									<th>Buildings (B)</th>
									<th>Courses (C)</th>
									<th>S/B</th>
									<th>C/B</th>
									<th>S/C</th>
								</tr>
							</thead>
							<tbody>
								<tr v-for="uni in universityData" :key="uni.name">
									<td>{{ uni.name }}</td>
									<td>{{ uni.students.toLocaleString() }}</td>
									<td>{{ uni.buildings }}</td>
									<td>{{ uni.courses.toLocaleString() }}</td>
									<td class="ratio">{{ uni.densityRatio }}</td>
									<td class="ratio">{{ uni.utilizationRatio }}</td>
									<td class="ratio">{{ uni.loadRatio }}</td>
								</tr>
							</tbody>
						</table>
					</div>

					<div class="ratio-legend">
						<h4>Ratio Definitions:</h4>
						<ul>
							<li>
								<strong>S/B (Density Ratio):</strong> Students per building. Higher =
								more crowded campus.
							</li>
							<li>
								<strong>C/B (Utilization Ratio):</strong> Courses per building. Higher
								= more multi-use buildings.
							</li>
							<li>
								<strong>S/C (Academic Load):</strong> Students per course offering.
								Lower = more course variety/choice.
							</li>
						</ul>
					</div>
				</div>

				<!-- Citations Tab -->
				<div v-if="activeTab === 'citations'" class="tab-content">
					<h3>Works Cited</h3>
					<p class="citation-intro">
						This research was compiled from official university statistics and academic
						sources. All data was accessed in January 2026.
					</p>
					<ol class="citation-list">
						<li v-for="cite in citations" :key="cite.id">
							<span class="cite-source">{{ cite.source }}.</span>
							<em>{{ cite.title }}</em>.
							<a :href="cite.url" target="_blank" rel="noopener">{{ cite.url }}</a>.
							<span class="access-date">Accessed {{ cite.accessDate }}.</span>
						</li>
					</ol>

					<div class="research-note">
						<h4>Research Methodology</h4>
						<p>
							Data was collected through analysis of official university publications,
							facility management reports, and academic calendars. Building counts
							represent distinct named architectural entities. Course counts represent
							unique catalog entries, not section instances.
						</p>
					</div>
				</div>
			</div>
		</div>
	</BaseModal>
</template>

<style scoped>
.modal-title {
	margin: 0;
	font-size: 1.1rem;
	font-weight: 500;
	letter-spacing: -0.01em;
}

.research-modal-content {
	min-height: 400px;
}

/* Tabs - simple underline style */
.modal-tabs {
	display: flex;
	gap: 1.5rem;
	border-bottom: 1px solid #ddd;
	margin-bottom: 1.5rem;
}

.modal-tabs button {
	background: none;
	border: none;
	padding: 0.6rem 0;
	font-size: 0.875rem;
	color: #666;
	cursor: pointer;
	position: relative;
}

.modal-tabs button:hover {
	color: #333;
}

.modal-tabs button.active {
	color: #111;
	font-weight: 500;
}

.modal-tabs button.active::after {
	content: '';
	position: absolute;
	bottom: -1px;
	left: 0;
	right: 0;
	height: 2px;
	background: #111;
}

.tab-container {
	margin-top: 1rem;
}

.tab-content {
	animation: fadeIn 0.15s ease;
}

@keyframes fadeIn {
	from {
		opacity: 0;
	}

	to {
		opacity: 1;
	}
}

.tab-content h3 {
	margin-top: 0;
	margin-bottom: 0.75rem;
	font-size: 1rem;
	font-weight: 500;
	color: #111;
}

/* Overview Tab */
.info-box {
	border: 1px solid #e5e5e5;
	padding: 1rem 1.25rem;
	margin-bottom: 2rem;
}

.info-box h3 {
	margin: 0 0 0.5rem 0;
	font-size: 0.8rem;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: #888;
}

.info-box p {
	margin: 0;
	line-height: 1.7;
	color: #333;
}

.formula-section h3 {
	margin-bottom: 1rem;
}

.formula-grid {
	display: grid;
	grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
	gap: 1px;
	background: #e5e5e5;
	border: 1px solid #e5e5e5;
	margin-bottom: 2rem;
}

.formula-card {
	background: #fff;
	padding: 1.25rem 1rem;
	text-align: left;
}

.formula {
	font-size: 1.1rem;
	font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
	margin-bottom: 0.5rem;
	color: #111;
}

.variable {
	color: #0066cc;
}

.constant {
	color: #666;
}

.formula-legend {
	font-size: 0.8rem;
	color: #666;
	line-height: 1.4;
}

.key-finding {
	border-top: 1px solid #e5e5e5;
	padding-top: 1.5rem;
}

.key-finding h3 {
	font-size: 0.8rem;
	text-transform: uppercase;
	letter-spacing: 0.05em;
	color: #888;
	margin-bottom: 0.75rem;
}

.key-finding p {
	margin: 0.5rem 0;
	line-height: 1.6;
	color: #333;
}

.highlight {
	font-weight: 600;
	color: #111;
}

/* Archetypes Tab */
.archetype-card {
	border-bottom: 1px solid #e5e5e5;
	padding: 1.5rem 0;
}

.archetype-card:first-child {
	padding-top: 0;
}

.archetype-card:last-child {
	border-bottom: none;
}

.archetype-header {
	display: flex;
	justify-content: space-between;
	align-items: baseline;
	margin-bottom: 0.5rem;
}

.archetype-header h3 {
	margin: 0;
	font-size: 1.1rem;
}

.archetype-badge {
	font-size: 0.7rem;
	font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
	color: #888;
	letter-spacing: 0.02em;
}

.archetype-desc {
	color: #555;
	margin-bottom: 1.25rem;
	line-height: 1.5;
}

.archetype-stats {
	display: flex;
	gap: 2rem;
	margin-bottom: 1rem;
	flex-wrap: wrap;
}

.stat {
	min-width: 100px;
}

.stat-value {
	display: block;
	font-size: 1.75rem;
	font-weight: 400;
	color: #111;
	line-height: 1;
	margin-bottom: 0.25rem;
}

.stat-label {
	font-size: 0.75rem;
	color: #888;
}

.archetype-examples {
	font-size: 0.875rem;
	color: #666;
	margin-bottom: 0.75rem;
}

.characteristics {
	margin: 0;
	padding-left: 1.25rem;
	color: #555;
}

.characteristics li {
	margin-bottom: 0.35rem;
	font-size: 0.875rem;
	line-height: 1.5;
}

/* Data Table */
.table-container {
	overflow-x: auto;
	margin-bottom: 1.5rem;
}

.data-table {
	width: 100%;
	border-collapse: collapse;
	font-size: 0.875rem;
}

.data-table th,
.data-table td {
	padding: 0.625rem 0.75rem;
	text-align: left;
	border-bottom: 1px solid #e5e5e5;
}

.data-table th {
	font-weight: 500;
	color: #666;
	font-size: 0.75rem;
	text-transform: uppercase;
	letter-spacing: 0.03em;
}

.data-table td {
	color: #333;
}

.data-table tbody tr:hover {
	background: #fafafa;
}

.data-table td.ratio {
	font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
	font-size: 0.8rem;
	color: #0066cc;
}

.ratio-legend {
	border: 1px solid #e5e5e5;
	padding: 1rem 1.25rem;
}

.ratio-legend h4 {
	margin: 0 0 0.5rem 0;
	font-size: 0.875rem;
	font-weight: 500;
}

.ratio-legend ul {
	margin: 0;
	padding-left: 1.25rem;
	color: #555;
}

.ratio-legend li {
	margin-bottom: 0.4rem;
	font-size: 0.875rem;
	line-height: 1.5;
}

/* Citations */
.citation-intro {
	color: #555;
	margin-bottom: 1.25rem;
	line-height: 1.6;
}

.citation-list {
	padding-left: 1.5rem;
	color: #333;
}

.citation-list li {
	margin-bottom: 0.75rem;
	line-height: 1.6;
	font-size: 0.875rem;
}

.cite-source {
	font-weight: 500;
}

.citation-list a {
	color: #0066cc;
	word-break: break-all;
}

.citation-list a:hover {
	text-decoration: none;
}

.access-date {
	color: #888;
	font-size: 0.8rem;
}

.research-note {
	border-top: 1px solid #e5e5e5;
	padding-top: 1.25rem;
	margin-top: 1.5rem;
}

.research-note h4 {
	margin: 0 0 0.5rem 0;
	font-size: 0.875rem;
	font-weight: 500;
}

.research-note p {
	margin: 0;
	color: #555;
	line-height: 1.6;
	font-size: 0.875rem;
}

@media (max-width: 768px) {
	.archetype-stats {
		gap: 1.5rem;
	}

	.formula-grid {
		grid-template-columns: 1fr;
	}

	.modal-tabs {
		gap: 1rem;
		overflow-x: auto;
	}
}
</style>
