import { spawnSync } from 'node:child_process'
import { parseA11yCliOptions } from './cli'

const forwardedArgs = process.argv.slice(2)
const options = parseA11yCliOptions(forwardedArgs)
const strictMode = options.strictMockGaps || options.strictRuntimeErrors || options.strictUncoveredRoutes
const npxBin = process.platform === 'win32' ? 'npx.cmd' : 'npx'

function runStep(label: string, scriptPath: string): number {
	console.log(`[a11y] ${label}`)

	const result = spawnSync(
		npxBin,
		['tsx', scriptPath, ...forwardedArgs],
		{
			cwd: process.cwd(),
			stdio: 'inherit',
		}
	)

	if (result.error) {
		console.error(`[a11y] ${label} failed to launch: ${result.error.message}`)
		return 1
	}

	if (result.status && result.status !== 0) {
		if (strictMode) {
			console.error(`[a11y] ${label} exited with code ${result.status}`)
			return result.status
		}
		console.warn(`[a11y] ${label} exited with code ${result.status}; continuing`)
	}

	return 0
}

function main(): void {
	const exitCodes = [
		runStep('discover routes', 'scripts/a11y/discover-routes.ts'),
		runStep('runtime scan', 'scripts/a11y/runtime.ts'),
		runStep('static lint scan', 'scripts/a11y/lint.ts'),
		runStep('aggregate report', 'scripts/a11y/report.ts'),
	]

	const firstFailure = exitCodes.find(code => code !== 0) ?? 0
	if (strictMode && firstFailure !== 0) {
		process.exit(firstFailure)
	}

	console.log(`[a11y] scan complete (${strictMode ? 'strict mode' : 'report-only mode'})`)
	process.exit(0)
}

main()
