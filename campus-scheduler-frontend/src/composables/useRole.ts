import { computed, ref, watch } from 'vue'

// TODO(student-role): bring back the student role once we have real Student
// entities + enrollments and a proper student selector.
export type Role = 'admin' | 'instructor' /* | 'student' */

const ROLE_KEY = 'campus-operations-system-role'
const INSTRUCTOR_KEY = 'campus-operations-system-instructor-id'

function normalizeRole(raw: string | null): Role {
	if (raw === 'admin' || raw === 'instructor') return raw
	// Legacy: student role was previously selectable but not implemented.
	if (raw === 'student') return 'instructor'
	return 'admin'
}

function normalizeInstructorId(raw: string | null): number | null {
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
const storedInstructorId = normalizeInstructorId(rawStoredInstructorId)
if (storedInstructorId === null) {
	localStorage.removeItem(INSTRUCTOR_KEY)
} else if (rawStoredInstructorId !== String(storedInstructorId)) {
	localStorage.setItem(INSTRUCTOR_KEY, String(storedInstructorId))
}

const role = ref<Role>(storedRole)
const instructorId = ref<number | null>(storedInstructorId)

watch(role, value => {
	localStorage.setItem(ROLE_KEY, value)
})

watch(instructorId, value => {
	const normalized = value !== null && Number.isInteger(value) && value > 0 ? value : null
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

export function useRole() {
	function setRole(nextRole: Role) {
		role.value = nextRole
	}

	function setInstructorId(id: number | null) {
		const normalized = id !== null && Number.isInteger(id) && id > 0 ? id : null
		instructorId.value = normalized
	}

	return {
		role,
		instructorId,
		setRole,
		setInstructorId,
		isAdmin: computed(() => role.value === 'admin'),
		isInstructor: computed(() => role.value === 'instructor'),
	}
}
