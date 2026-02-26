import { ref, watch } from 'vue'

export type RouteTransitionName =
	| 'route-fade-up'
	| 'route-slide-left'
	| 'route-solver-focus'
	| 'route-zoom-blur'
	| 'route-flip-soft'

const TRANSITION_KEY = 'campus-operations-system-route-transition'
const DEFAULT_TRANSITION: RouteTransitionName = 'route-fade-up'

function normalizeTransition(raw: string | null): RouteTransitionName {
	switch (raw) {
		case 'route-slide-left':
		case 'route-solver-focus':
		case 'route-zoom-blur':
		case 'route-flip-soft':
		case 'route-fade-up':
			return raw
		default:
			return DEFAULT_TRANSITION
	}
}

const rawStoredTransition = localStorage.getItem(TRANSITION_KEY)
const storedTransition = normalizeTransition(rawStoredTransition)

if (rawStoredTransition !== storedTransition) {
	localStorage.setItem(TRANSITION_KEY, storedTransition)
}

const routeTransition = ref<RouteTransitionName>(storedTransition)

watch(routeTransition, value => {
	localStorage.setItem(TRANSITION_KEY, value)
})

export function useRouteTransition() {
	function setRouteTransition(nextTransition: RouteTransitionName) {
		routeTransition.value = nextTransition
	}

	return {
		routeTransition,
		setRouteTransition,
	}
}
