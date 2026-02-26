import fs from 'node:fs'
import path from 'node:path'
import { parseA11yCliOptions, routeMatchesFilter } from './cli'
import type {
	A11yMockGap,
	A11yRouteManifest,
	A11yScanResult,
	A11ySummary,
	A11yViolation,
} from './types'

interface EslintMessage {
	ruleId: string | null
	message: string
	severity: number
	line: number
	column: number
}

interface EslintFileResult {
	filePath: string
	messages: EslintMessage[]
}

interface EslintViolation extends A11yViolation {
	filePath: string
	line: number
	column: number
}

function readJsonFile<T>(filePath: string, fallback: T): T {
	if (!fs.existsSync(filePath)) return fallback
	try {
		return JSON.parse(fs.readFileSync(filePath, 'utf8')) as T
	} catch {
		return fallback
	}
}

function readRuntimeResults(runtimeDir: string): A11yScanResult[] {
	if (!fs.existsSync(runtimeDir)) return []
	const files = fs
		.readdirSync(runtimeDir)
		.filter(file => file.endsWith('.json'))
		.map(file => path.join(runtimeDir, file))

	return files.map(file => readJsonFile<A11yScanResult | null>(file, null)).filter(Boolean) as A11yScanResult[]
}

function toLintViolations(eslint: EslintFileResult[]): EslintViolation[] {
	const violations: EslintViolation[] = []

	for (const fileResult of eslint) {
		for (const message of fileResult.messages || []) {
			violations.push({
				source: 'eslint',
				ruleId: message.ruleId || 'eslint-unknown-rule',
				impact: message.severity >= 2 ? 'serious' : 'moderate',
				selector: `${fileResult.filePath}:${message.line}:${message.column}`,
				message: message.message,
				filePath: fileResult.filePath,
				line: message.line,
				column: message.column,
			})
		}
	}

	return violations
}

function countBy<T extends string>(values: T[]): Record<string, number> {
	const output: Record<string, number> = {}
	for (const value of values) {
		output[value] = (output[value] || 0) + 1
	}
	return output
}

function dedupeMockGaps(items: A11yMockGap[]): A11yMockGap[] {
	const map = new Map<string, A11yMockGap>()
	for (const item of items) {
		const key = `${item.method}|${item.url}|${item.reason}|${item.route}|${item.role}|${item.theme}|${item.scenario}`
		if (!map.has(key)) map.set(key, item)
	}
	return Array.from(map.values())
}

function buildSummary(
	manifest: A11yRouteManifest,
	runtimeResults: A11yScanResult[],
	lintViolations: EslintViolation[],
	routeFilters: string[] | null
): A11ySummary {
	const runtimeViolations = runtimeResults.flatMap(result => result.violations)
	const allViolations = [...runtimeViolations, ...lintViolations]

	const affectedRoutes = Array.from(
		new Set(runtimeResults.filter(item => item.violations.length > 0).map(item => item.target.route))
	).sort()

	const manifestRoutes = manifest.routes
		.map(entry => entry.route)
		.filter(route => routeMatchesFilter(route, routeFilters))

	const coveredRoutes = new Set(runtimeResults.map(item => item.target.route))
	const uncoveredRoutes = manifestRoutes.filter(route => !coveredRoutes.has(route)).sort()

	const mockGaps = dedupeMockGaps(runtimeResults.flatMap(item => item.mockGaps))

	return {
		generatedAt: new Date().toISOString(),
		totals: {
			targets: runtimeResults.length,
			violations: allViolations.length,
		},
		byImpact: countBy(allViolations.map(item => item.impact || 'unknown')),
		byRule: countBy(allViolations.map(item => item.ruleId || 'unknown')),
		bySource: countBy(allViolations.map(item => item.source || 'unknown')),
		affectedRoutes,
		uncoveredRoutes,
		mockGaps,
	}
}

function toMarkdown(
	summary: A11ySummary,
	runtimeResults: A11yScanResult[],
	lintViolations: EslintViolation[]
): string {
	const topRules = Object.entries(summary.byRule)
		.sort((a, b) => b[1] - a[1])
		.slice(0, 20)

	const affectedComponents = Array.from(new Set(lintViolations.map(item => item.filePath))).sort()

	const lines: string[] = []
	lines.push('# Accessibility Scan Summary')
	lines.push('')
	lines.push(`Generated: ${summary.generatedAt}`)
	lines.push(`Targets scanned: ${summary.totals.targets}`)
	lines.push(`Total violations: ${summary.totals.violations}`)
	lines.push('')
	lines.push('## Violations by Severity')
	lines.push('')
	for (const [severity, count] of Object.entries(summary.byImpact).sort((a, b) => b[1] - a[1])) {
		lines.push(`- ${severity}: ${count}`)
	}

	lines.push('')
	lines.push('## Violations by Source')
	lines.push('')
	for (const [source, count] of Object.entries(summary.bySource).sort((a, b) => b[1] - a[1])) {
		lines.push(`- ${source}: ${count}`)
	}

	lines.push('')
	lines.push('## Top Rules')
	lines.push('')
	for (const [rule, count] of topRules) {
		lines.push(`- ${rule}: ${count}`)
	}

	lines.push('')
	lines.push('## Affected Routes')
	lines.push('')
	if (summary.affectedRoutes.length === 0) {
		lines.push('- None')
	} else {
		for (const route of summary.affectedRoutes) {
			lines.push(`- ${route}`)
		}
	}

	lines.push('')
	lines.push('## Affected Components (Lint)')
	lines.push('')
	if (affectedComponents.length === 0) {
		lines.push('- None')
	} else {
		for (const file of affectedComponents) {
			lines.push(`- ${file}`)
		}
	}

	lines.push('')
	lines.push('## Uncovered Routes')
	lines.push('')
	if (summary.uncoveredRoutes.length === 0) {
		lines.push('- None')
	} else {
		for (const route of summary.uncoveredRoutes) {
			lines.push(`- ${route}`)
		}
	}

	lines.push('')
	lines.push('## Mock Gaps')
	lines.push('')
	if (summary.mockGaps.length === 0) {
		lines.push('- None')
	} else {
		for (const gap of summary.mockGaps) {
			lines.push(`- ${gap.method} ${gap.url} (${gap.reason}) [${gap.role}/${gap.theme}/${gap.scenario} ${gap.route}]`)
		}
	}

	lines.push('')
	lines.push('## Runtime Errors')
	lines.push('')
	const runtimeErrors = runtimeResults.flatMap(result => result.runtimeErrors)
	if (runtimeErrors.length === 0) {
		lines.push('- None')
	} else {
		for (const error of runtimeErrors.slice(0, 100)) {
			lines.push(`- ${error}`)
		}
	}

	return lines.join('\n')
}

function toHtml(markdownSummary: string): string {
	const escaped = markdownSummary
		.replace(/&/g, '&amp;')
		.replace(/</g, '&lt;')
		.replace(/>/g, '&gt;')

	return [
		'<!doctype html>',
		'<html lang="en">',
		'<head>',
		'<meta charset="utf-8" />',
		'<meta name="viewport" content="width=device-width, initial-scale=1" />',
		'<title>Accessibility Scan Summary</title>',
		'<style>',
		'body { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; margin: 24px; line-height: 1.5; }',
		'pre { white-space: pre-wrap; }',
		'</style>',
		'</head>',
		'<body>',
		'<pre>',
		escaped,
		'</pre>',
		'</body>',
		'</html>',
	].join('')
}

function printStrictReasonBlock(title: string, count: number, lines: string[]): void {
	console.error(`[a11y][strict] ${title}: ${count}`)
	for (const line of lines.slice(0, 20)) {
		console.error(`  - ${line}`)
	}
	if (lines.length > 20) {
		console.error(`  - ... and ${lines.length - 20} more`)
	}
}

function main(): void {
	const options = parseA11yCliOptions()
	fs.mkdirSync(options.reportDir, { recursive: true })

	const manifest = readJsonFile<A11yRouteManifest>(path.join(options.reportDir, 'manifest.json'), {
		generatedAt: new Date().toISOString(),
		sourceFile: '',
		routes: [],
	})

	const runtimeResults = readRuntimeResults(path.join(options.reportDir, 'runtime'))
	const eslintRaw = readJsonFile<EslintFileResult[]>(path.join(options.reportDir, 'eslint.json'), [])
	const lintViolations = toLintViolations(eslintRaw)

	const summary = buildSummary(manifest, runtimeResults, lintViolations, options.routeFilters)

	const output = {
		metadata: {
			generatedAt: summary.generatedAt,
			reportDir: options.reportDir,
			formats: options.formats,
		},
		manifest,
		summary,
		runtime: {
			results: runtimeResults,
		},
		lint: {
			violations: lintViolations,
			raw: eslintRaw,
		},
	}

	const markdownSummary = toMarkdown(summary, runtimeResults, lintViolations)

	fs.writeFileSync(path.join(options.reportDir, 'results.json'), JSON.stringify(output, null, 2), 'utf8')
	fs.writeFileSync(path.join(options.reportDir, 'summary.md'), markdownSummary, 'utf8')

	if (options.formats.includes('html')) {
		fs.writeFileSync(path.join(options.reportDir, 'index.html'), toHtml(markdownSummary), 'utf8')
	}

	const runtimeErrors = runtimeResults.flatMap(result => result.runtimeErrors)

	if (options.strictMockGaps && summary.mockGaps.length > 0) {
		process.exitCode = 1
		printStrictReasonBlock(
			'mock gaps',
			summary.mockGaps.length,
			summary.mockGaps.map(gap => `${gap.method} ${gap.url} (${gap.reason}) [${gap.role}/${gap.theme}/${gap.scenario} ${gap.route}]`)
		)
	}

	if (options.strictRuntimeErrors && runtimeErrors.length > 0) {
		process.exitCode = 1
		printStrictReasonBlock('runtime errors', runtimeErrors.length, runtimeErrors)
	}

	if (options.strictUncoveredRoutes && summary.uncoveredRoutes.length > 0) {
		process.exitCode = 1
		printStrictReasonBlock('uncovered routes', summary.uncoveredRoutes.length, summary.uncoveredRoutes)
	}

	console.log(`[a11y] report generated in ${options.reportDir}`)
	process.exit(process.exitCode ?? 0)
}

main()
