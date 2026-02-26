export type A11yRole = 'admin' | 'instructor'
export type A11yTheme = 'snow-storm' | 'slate'
export type A11yScenario = 'empty' | 'normal' | 'dense' | 'error'

export interface A11yRouteManifestEntry {
	template: string
	route: string
	source: 'static' | 'dynamic'
	params?: Record<string, string>
}

export interface A11yRouteManifest {
	generatedAt: string
	sourceFile: string
	routes: A11yRouteManifestEntry[]
}

export interface A11yRouteTarget {
	route: string
	template: string
	role: A11yRole
	theme: A11yTheme
	scenario: A11yScenario
	source: 'static' | 'dynamic'
}

export interface A11yViolation {
	ruleId: string
	impact: string
	selector: string
	message: string
	helpUrl?: string
	source: 'axe' | 'custom' | 'eslint'
	context?: string
}

export interface A11yMockGap {
	method: string
	url: string
	reason: string
	route: string
	role: A11yRole
	theme: A11yTheme
	scenario: A11yScenario
}

export interface A11yScanResult {
	target: A11yRouteTarget
	scannedAt: string
	documentTitle: string
	finalUrl: string
	violations: A11yViolation[]
	mockGaps: A11yMockGap[]
	runtimeErrors: string[]
}

export interface A11ySummary {
	generatedAt: string
	totals: {
		targets: number
		violations: number
	}
	byImpact: Record<string, number>
	byRule: Record<string, number>
	bySource: Record<string, number>
	affectedRoutes: string[]
	uncoveredRoutes: string[]
	mockGaps: A11yMockGap[]
}

export interface A11yCliOptions {
	roles: A11yRole[] | null
	themes: A11yTheme[] | null
	scenarios: A11yScenario[]
	routeFilters: string[] | null
	formats: string[]
	reportDir: string
	workers: number | null
	fullyParallel: boolean
	strictMockGaps: boolean
	strictRuntimeErrors: boolean
	strictUncoveredRoutes: boolean
}
