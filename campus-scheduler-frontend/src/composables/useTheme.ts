import { computed, ref, watch } from 'vue'

export type Theme = 'snow-storm' | 'slate'

const THEME_KEY = 'campus-operations-system-theme'
const THEME_TRANSITION_CLASS = 'theme-transitioning'
const THEME_TRANSITION_DURATION_MS = 480

let themeTransitionTimeout: ReturnType<typeof window.setTimeout> | null = null

function normalizeTheme(raw: string | null): Theme {
	return raw === 'slate' ? 'slate' : 'snow-storm'
}

function shouldAnimateThemeTransition() {
	if (typeof window.matchMedia !== 'function') {
		return true
	}

	return !window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function applyTheme(theme: Theme, options: { animate?: boolean } = {}) {
	const root = document.documentElement
	const animate = options.animate === true && shouldAnimateThemeTransition()

	if (animate) {
		root.classList.add(THEME_TRANSITION_CLASS)
		if (themeTransitionTimeout !== null) {
			window.clearTimeout(themeTransitionTimeout)
		}
	}

	root.setAttribute('data-theme', theme)

	if (animate) {
		themeTransitionTimeout = window.setTimeout(() => {
			root.classList.remove(THEME_TRANSITION_CLASS)
			themeTransitionTimeout = null
		}, THEME_TRANSITION_DURATION_MS)
	}
}

const rawStoredTheme = localStorage.getItem(THEME_KEY)
const storedTheme = normalizeTheme(rawStoredTheme)

if (rawStoredTheme !== storedTheme) {
	localStorage.setItem(THEME_KEY, storedTheme)
}

const theme = ref<Theme>(storedTheme)
applyTheme(storedTheme)

watch(theme, value => {
	localStorage.setItem(THEME_KEY, value)
	applyTheme(value, { animate: true })
})

export function useTheme() {
	function setTheme(nextTheme: Theme) {
		theme.value = nextTheme
	}

	return {
		theme,
		setTheme,
		isSnowStorm: computed(() => theme.value === 'snow-storm'),
		isSlate: computed(() => theme.value === 'slate'),
	}
}
