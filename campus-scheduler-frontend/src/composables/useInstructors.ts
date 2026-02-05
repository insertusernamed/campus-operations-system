import { ref } from 'vue'
import { instructorsService, type Instructor } from '@/services/instructors'

const instructors = ref<Instructor[]>([])
const loading = ref(false)

export function useInstructors() {
	async function loadInstructors() {
		loading.value = true
		try {
			instructors.value = await instructorsService.getAll()
		} catch (error) {
			console.error('Failed to load instructors', error)
		} finally {
			loading.value = false
		}
	}

	return {
		instructors,
		loading,
		loadInstructors,
	}
}
