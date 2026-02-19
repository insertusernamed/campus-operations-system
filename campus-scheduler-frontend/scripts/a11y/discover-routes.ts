import fs from 'node:fs'
import path from 'node:path'
import { parseA11yCliOptions } from './cli'
import type { A11yRouteManifest, A11yRouteManifestEntry } from './types'

const ROUTER_FILE = path.resolve(process.cwd(), 'src/router/index.ts')

const PARAM_FIXTURES: Record<string, string> = {
	id: '1',
	roomId: '1',
	buildingId: '1',
	instructorId: '10',
	courseId: '20',
	scheduleId: '50',
}

function extractRouteTemplates(routerSource: string): string[] {
	const pathPattern = /path:\s*'([^']+)'/g
	const matches = [...routerSource.matchAll(pathPattern)]
	return Array.from(new Set(matches.map(match => match[1] || '').filter(Boolean)))
}

function expandDynamicRoute(template: string): { route: string; params: Record<string, string> } {
	const params: Record<string, string> = {}

	const route = template.replace(/:([A-Za-z0-9_]+)/g, (_raw, paramName: string) => {
		const replacement = PARAM_FIXTURES[paramName] ?? '1'
		params[paramName] = replacement
		return replacement
	})

	return { route, params }
}

function toManifestEntries(templates: string[]): A11yRouteManifestEntry[] {
	return templates.map((template) => {
		if (!template.includes(':')) {
			return {
				template,
				route: template,
				source: 'static',
			}
		}

		const expanded = expandDynamicRoute(template)
		return {
			template,
			route: expanded.route,
			source: 'dynamic',
			params: expanded.params,
		}
	})
}

function main(): void {
	const options = parseA11yCliOptions()

	if (!fs.existsSync(ROUTER_FILE)) {
		throw new Error(`Router file not found: ${ROUTER_FILE}`)
	}

	const routerSource = fs.readFileSync(ROUTER_FILE, 'utf8')
	const templates = extractRouteTemplates(routerSource)
	const entries = toManifestEntries(templates)

	const manifest: A11yRouteManifest = {
		generatedAt: new Date().toISOString(),
		sourceFile: ROUTER_FILE,
		routes: entries,
	}

	fs.mkdirSync(options.reportDir, { recursive: true })
	const outputPath = path.join(options.reportDir, 'manifest.json')
	fs.writeFileSync(outputPath, JSON.stringify(manifest, null, 2), 'utf8')

	console.log(`[a11y] discovered ${manifest.routes.length} routes -> ${outputPath}`)
}

main()
