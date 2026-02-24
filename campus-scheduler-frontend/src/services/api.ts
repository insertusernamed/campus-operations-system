import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'
const api = axios.create({
	baseURL,
	// Do not set Content-Type globally: it forces CORS preflights on GETs.
	// Axios will set Content-Type automatically for JSON request bodies.
	headers: { Accept: 'application/json' },
})

export default api
