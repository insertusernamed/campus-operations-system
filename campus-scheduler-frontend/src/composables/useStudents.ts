import { ref } from 'vue'
import { studentsService, type Student } from '@/services/students'

const students = ref<Student[]>([])
const loading = ref(false)

export function useStudents() {
	async function loadStudents() {
		loading.value = true
		try {
			students.value = await studentsService.getAll()
		} catch (error) {
			console.error('Failed to load students', error)
		} finally {
			loading.value = false
		}
	}

	return {
		students,
		loading,
		loadStudents,
	}
}
