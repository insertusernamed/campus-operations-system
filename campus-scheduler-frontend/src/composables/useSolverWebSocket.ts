import { ref, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export interface SolverProgress {
	status: 'SOLVING_ACTIVE' | 'NOT_SOLVING'
	score: string | null
	assignedCourses: number
	totalCourses: number
	hardViolations: number
	softScore: number
	message: string
}

export function useSolverWebSocket() {
	const progress = ref<SolverProgress | null>(null)
	const connected = ref(false)
	const error = ref<string | null>(null)

	let client: Client | null = null

	function connect() {
		client = new Client({
			webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
			reconnectDelay: 5000,
			heartbeatIncoming: 4000,
			heartbeatOutgoing: 4000,
			onConnect: () => {
				connected.value = true
				error.value = null

				client?.subscribe('/topic/solver/progress', (message) => {
					try {
						progress.value = JSON.parse(message.body)
					} catch (e) {
						console.error('Failed to parse solver progress:', e)
					}
				})
			},
			onDisconnect: () => {
				connected.value = false
			},
			onStompError: (frame) => {
				error.value = frame.headers['message'] || 'WebSocket error'
				console.error('STOMP error:', frame)
			},
			onWebSocketError: (event) => {
				error.value = 'WebSocket connection failed'
				console.error('WebSocket error:', event)
			},
		})

		client.activate()
	}

	function disconnect() {
		if (client?.active) {
			client.deactivate()
		}
	}

	onMounted(() => {
		connect()
	})

	onUnmounted(() => {
		disconnect()
	})

	return {
		progress,
		connected,
		error,
		connect,
		disconnect,
	}
}
