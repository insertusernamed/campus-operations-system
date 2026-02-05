import { computed, ref, watch } from 'vue'

export type Role = 'admin' | 'instructor' | 'student'

const ROLE_KEY = 'campus-scheduler-role'
const INSTRUCTOR_KEY = 'campus-scheduler-instructor-id'

const storedRole = (localStorage.getItem(ROLE_KEY) as Role | null) || 'admin'
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
        isStudent: computed(() => role.value === 'student'),
    }
}
