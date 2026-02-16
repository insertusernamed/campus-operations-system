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

const rawStoredRole = localStorage.getItem(ROLE_KEY)
const storedRole = normalizeRole(rawStoredRole)
// If the stored role is no longer valid, rewrite it so the UI doesn't get stuck.
if (rawStoredRole !== storedRole) {
	localStorage.setItem(ROLE_KEY, storedRole)
}
const storedInstructorId = localStorage.getItem(INSTRUCTOR_KEY)

const role = ref<Role>(storedRole)
const instructorId = ref<number | null>(storedInstructorId ? Number(storedInstructorId) : null)

watch(role, value => {
	localStorage.setItem(ROLE_KEY, value)
})

watch(instructorId, value => {
	if (value === null || Number.isNaN(value)) {
		localStorage.removeItem(INSTRUCTOR_KEY)
		return
	}
	localStorage.setItem(INSTRUCTOR_KEY, String(value))
})

export function useRole() {
	function setRole(nextRole: Role) {
		role.value = nextRole
	}

	function setInstructorId(id: number | null) {
		instructorId.value = id
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
