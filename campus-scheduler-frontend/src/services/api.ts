import axios from 'axios'

const api = axios.create({
	baseURL: 'http://localhost:8080/api',
	// Do not set Content-Type globally: it forces CORS preflights on GETs.
	// Axios will set Content-Type automatically for JSON request bodies.
	headers: { Accept: 'application/json' },
})

export default api
