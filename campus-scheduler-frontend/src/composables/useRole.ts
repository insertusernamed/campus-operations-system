import { computed, ref, watch } from 'vue'

export type Role = 'admin' | 'instructor' | 'student'

export const ROLE_KEY = 'campus-operations-system-role'
export const INSTRUCTOR_KEY = 'campus-operations-system-instructor-id'
export const STUDENT_KEY = 'campus-operations-system-student-id'

function normalizeRole(raw: string | null): Role {
	if (raw === 'admin' || raw === 'instructor' || raw === 'student') return raw
	return 'admin'
}

function normalizeSelectedId(raw: string | null): number | null {
	if (raw === null) return null
	const parsed = Number(raw)
	if (!Number.isInteger(parsed) || parsed <= 0) {
		return null
	}
	return parsed
}

const rawStoredRole = localStorage.getItem(ROLE_KEY)
const storedRole = normalizeRole(rawStoredRole)
// If the stored role is no longer valid, rewrite it so the UI doesn't get stuck.
if (rawStoredRole !== storedRole) {
	localStorage.setItem(ROLE_KEY, storedRole)
}
const rawStoredInstructorId = localStorage.getItem(INSTRUCTOR_KEY)
const storedInstructorId = normalizeSelectedId(rawStoredInstructorId)
if (storedInstructorId === null) {
	localStorage.removeItem(INSTRUCTOR_KEY)
} else if (rawStoredInstructorId !== String(storedInstructorId)) {
	localStorage.setItem(INSTRUCTOR_KEY, String(storedInstructorId))
}
const rawStoredStudentId = localStorage.getItem(STUDENT_KEY)
const storedStudentId = normalizeSelectedId(rawStoredStudentId)
if (storedStudentId === null) {
	localStorage.removeItem(STUDENT_KEY)
} else if (rawStoredStudentId !== String(storedStudentId)) {
	localStorage.setItem(STUDENT_KEY, String(storedStudentId))
}

const role = ref<Role>(storedRole)
const instructorId = ref<number | null>(storedInstructorId)
const studentId = ref<number | null>(storedStudentId)

watch(role, value => {
	localStorage.setItem(ROLE_KEY, value)
})

watch(instructorId, value => {
	const normalized = normalizeSelectedId(value === null ? null : String(value))
	if (normalized !== value) {
		instructorId.value = normalized
		return
	}
	if (normalized === null) {
		localStorage.removeItem(INSTRUCTOR_KEY)
		return
	}
	localStorage.setItem(INSTRUCTOR_KEY, String(normalized))
})

watch(studentId, value => {
	const normalized = normalizeSelectedId(value === null ? null : String(value))
	if (normalized !== value) {
		studentId.value = normalized
		return
	}
	if (normalized === null) {
		localStorage.removeItem(STUDENT_KEY)
		return
	}
	localStorage.setItem(STUDENT_KEY, String(normalized))
})

export function useRole() {
	function setRole(nextRole: Role) {
		role.value = nextRole
	}

	function setInstructorId(id: number | null) {
		instructorId.value = normalizeSelectedId(id === null ? null : String(id))
	}

	function setStudentId(id: number | null) {
		studentId.value = normalizeSelectedId(id === null ? null : String(id))
	}

	return {
		role,
		instructorId,
		studentId,
		setRole,
		setInstructorId,
		setStudentId,
		isAdmin: computed(() => role.value === 'admin'),
		isInstructor: computed(() => role.value === 'instructor'),
		isStudent: computed(() => role.value === 'student'),
	}
}
