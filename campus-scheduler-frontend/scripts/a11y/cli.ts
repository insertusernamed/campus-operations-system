import path from 'node:path'
import type { A11yCliOptions, A11yRole, A11yScenario, A11yTheme } from './types'

const DEFAULT_REPORT_DIR = 'reports/a11y/latest'

const ROLE_VALUES: A11yRole[] = ['admin', 'instructor']
const THEME_VALUES: A11yTheme[] = ['snow-storm', 'slate']
const SCENARIO_VALUES: A11yScenario[] = ['empty', 'normal', 'dense', 'error']

function parseList(value: string): string[] {
	return value
		.split(',')
		.map(item => item.trim())
		.filter(Boolean)
}

function parseArgValue(args: string[], key: string): string[] {
	const values: string[] = []

	for (let i = 0; i < args.length; i++) {
		const arg = args[i]
		if (arg === `--${key}` && args[i + 1]) {
			values.push(args[i + 1] as string)
			i += 1
			continue
		}

		const prefix = `--${key}=`
		if (arg.startsWith(prefix)) {
			values.push(arg.slice(prefix.length))
		}
	}

	return values
}

function hasArgFlag(args: string[], key: string): boolean {
	return args.includes(`--${key}`)
}

function unique<T>(items: T[]): T[] {
	return Array.from(new Set(items))
}

function readEnvList(key: string): string[] {
	const raw = process.env[key]
	if (!raw) return []
	return parseList(raw)
}

function parseRoles(args: string[]): A11yRole[] | null {
	const raw = [...parseArgValue(args, 'role'), ...readEnvList('A11Y_ROLE_FILTER')]
	if (!raw.length) return null

	const values = unique(raw.flatMap(parseList)).filter((value): value is A11yRole =>
		ROLE_VALUES.includes(value as A11yRole)
	)

	return values.length ? values : null
}

function parseThemes(args: string[]): A11yTheme[] | null {
	const raw = [...parseArgValue(args, 'theme'), ...readEnvList('A11Y_THEME_FILTER')]
	if (!raw.length) return null

	const values = unique(raw.flatMap(parseList)).filter((value): value is A11yTheme =>
		THEME_VALUES.includes(value as A11yTheme)
	)

	return values.length ? values : null
}

function parseRoutes(args: string[]): string[] | null {
	const raw = [...parseArgValue(args, 'route'), ...readEnvList('A11Y_ROUTE_FILTER')]
	if (!raw.length) return null

	const values = unique(raw.flatMap(parseList))
	return values.length ? values : null
}

function parseScenarios(args: string[]): A11yScenario[] {
	const raw = [...parseArgValue(args, 'scenario'), ...readEnvList('A11Y_SCENARIO_FILTER')]
	if (!raw.length) return ['normal']

	const values = unique(raw.flatMap(parseList)).filter((value): value is A11yScenario =>
		SCENARIO_VALUES.includes(value as A11yScenario)
	)

	return values.length ? values : ['normal']
}

function parseFormats(args: string[]): string[] {
	const raw = [...parseArgValue(args, 'format'), ...readEnvList('A11Y_FORMAT')]
	const values = unique(raw.flatMap(parseList).map(v => v.toLowerCase()))
	return values.length ? values : ['json', 'md', 'html']
}

function parseReportDir(args: string[]): string {
	const argValues = parseArgValue(args, 'report-dir')
	const fromArg = argValues[argValues.length - 1]
	const fromEnv = process.env.A11Y_REPORT_DIR
	return path.resolve(process.cwd(), fromArg ?? fromEnv ?? DEFAULT_REPORT_DIR)
}

function parseWorkers(args: string[]): number | null {
	const argValues = parseArgValue(args, 'workers')
	const fromArg = argValues[argValues.length - 1]
	const raw = fromArg ?? process.env.A11Y_WORKERS
	if (!raw) return null

	const parsed = Number.parseInt(raw, 10)
	return Number.isFinite(parsed) && parsed > 0 ? parsed : null
}

function parseFullyParallel(args: string[]): boolean {
	if (hasArgFlag(args, 'fully-parallel')) return true

	const raw = process.env.A11Y_FULLY_PARALLEL
	if (!raw) return false

	return ['1', 'true', 'yes', 'on'].includes(raw.toLowerCase())
}

function parseStrictFlag(args: string[], key: string, envKey: string): boolean {
	if (hasArgFlag(args, key)) return true

	const raw = process.env[envKey]
	if (!raw) return false

	return ['1', 'true', 'yes', 'on'].includes(raw.toLowerCase())
}

export function parseA11yCliOptions(argv: string[] = process.argv.slice(2)): A11yCliOptions {
	return {
		roles: parseRoles(argv),
		themes: parseThemes(argv),
		scenarios: parseScenarios(argv),
		routeFilters: parseRoutes(argv),
		formats: parseFormats(argv),
		reportDir: parseReportDir(argv),
		workers: parseWorkers(argv),
		fullyParallel: parseFullyParallel(argv),
		enableInteractionCrawl: parseStrictFlag(argv, 'enable-interaction-crawl', 'A11Y_ENABLE_INTERACTION_CRAWL'),
		strictMockGaps: parseStrictFlag(argv, 'strict-mock-gaps', 'A11Y_STRICT_MOCK_GAPS'),
		strictRuntimeErrors: parseStrictFlag(argv, 'strict-runtime-errors', 'A11Y_STRICT_RUNTIME_ERRORS'),
		strictUncoveredRoutes: parseStrictFlag(argv, 'strict-uncovered-routes', 'A11Y_STRICT_UNCOVERED_ROUTES'),
	}
}

export function applyA11yEnv(options: A11yCliOptions): NodeJS.ProcessEnv {
	const env = { ...process.env }

	env.A11Y_REPORT_DIR = options.reportDir
	env.A11Y_FORMAT = options.formats.join(',')
	if (options.workers) env.A11Y_WORKERS = String(options.workers)
	if (options.fullyParallel) env.A11Y_FULLY_PARALLEL = '1'
	if (options.enableInteractionCrawl) env.A11Y_ENABLE_INTERACTION_CRAWL = '1'
	if (options.strictMockGaps) env.A11Y_STRICT_MOCK_GAPS = '1'
	if (options.strictRuntimeErrors) env.A11Y_STRICT_RUNTIME_ERRORS = '1'
	if (options.strictUncoveredRoutes) env.A11Y_STRICT_UNCOVERED_ROUTES = '1'
	env.A11Y_SCENARIO_FILTER = options.scenarios.join(',')

	if (options.roles) env.A11Y_ROLE_FILTER = options.roles.join(',')
	if (options.themes) env.A11Y_THEME_FILTER = options.themes.join(',')
	if (options.routeFilters) env.A11Y_ROUTE_FILTER = options.routeFilters.join(',')

	return env
}

export function routeMatchesFilter(route: string, routeFilters: string[] | null): boolean {
	if (!routeFilters || routeFilters.length === 0) return true
	return routeFilters.some(filter => route.includes(filter))
}
