import axios, { AxiosHeaders } from 'axios'
import { ROLE_KEY, STUDENT_KEY, type Role } from '@/composables/useRole'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const api = axios.create({
	baseURL,
	// Do not set Content-Type globally: it forces CORS preflights on GETs.
	// Axios will set Content-Type automatically for JSON request bodies.
	headers: { Accept: 'application/json' },
})

function readStorageValue(key: string): string | null {
	if (typeof window === 'undefined') {
		return null
	}
	return window.localStorage.getItem(key)
}

function readViewerRole(): Role {
	const raw = readStorageValue(ROLE_KEY)
	if (raw === 'admin' || raw === 'instructor' || raw === 'student') {
		return raw
	}
	return 'admin'
}

function readSelectedStudentId(): number | null {
	const raw = readStorageValue(STUDENT_KEY)
	if (raw === null) {
		return null
	}

	const parsed = Number(raw)
	if (!Number.isInteger(parsed) || parsed <= 0) {
		return null
	}

	return parsed
}

api.interceptors.request.use((config) => {
	const headers = AxiosHeaders.from(config.headers)
	const viewerRole = readViewerRole()
	headers.set('X-Viewer-Role', viewerRole)

	const studentId = viewerRole === 'student' ? readSelectedStudentId() : null
	if (studentId !== null) {
		headers.set('X-Viewer-Student-Id', String(studentId))
	} else {
		headers.delete('X-Viewer-Student-Id')
	}

	config.headers = headers
	return config
})

export default api
