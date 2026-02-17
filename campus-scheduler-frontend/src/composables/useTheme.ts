import { computed, ref, watch } from 'vue'

export type Theme = 'snow-storm' | 'slate'

const THEME_KEY = 'campus-operations-system-theme'

function normalizeTheme(raw: string | null): Theme {
	return raw === 'slate' ? 'slate' : 'snow-storm'
}

function applyTheme(theme: Theme) {
	document.documentElement.setAttribute('data-theme', theme)
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
	applyTheme(value)
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
