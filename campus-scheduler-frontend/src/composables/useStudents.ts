import { ref } from 'vue'
import { studentsService, type Student } from '@/services/students'

const students = ref<Student[]>([])
const loading = ref(false)

export function useStudents() {
	async function loadStudents() {
		loading.value = true
		try {
			students.value = (await studentsService.getAll()).sort((a, b) => {
				const lastNameComparison = (a.lastName ?? '').localeCompare(b.lastName ?? '')
				if (lastNameComparison !== 0) {
					return lastNameComparison
				}
				const firstNameComparison = (a.firstName ?? '').localeCompare(b.firstName ?? '')
				if (firstNameComparison !== 0) {
					return firstNameComparison
				}
				return (a.email ?? '').localeCompare(b.email ?? '')
			})
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
