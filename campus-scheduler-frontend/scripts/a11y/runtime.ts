import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { applyA11yEnv, parseA11yCliOptions } from './cli'

function main(): void {
	const options = parseA11yCliOptions()
	const env = applyA11yEnv(options)
	const strictMode = options.strictMockGaps || options.strictRuntimeErrors || options.strictUncoveredRoutes
	const runtimeDir = path.join(options.reportDir, 'runtime')
	const playwrightArgs = ['playwright', 'test', '-c', 'playwright.a11y.config.ts']

	if (options.fullyParallel) {
		playwrightArgs.push('--fully-parallel')
	}

	if (options.workers) {
		playwrightArgs.push(`--workers=${options.workers}`)
	}

	fs.rmSync(runtimeDir, { recursive: true, force: true })
	fs.mkdirSync(runtimeDir, { recursive: true })

	const result = spawnSync(
		process.platform === 'win32' ? 'npx.cmd' : 'npx',
		playwrightArgs,
		{
			cwd: process.cwd(),
			env,
			stdio: 'inherit',
		}
	)

	if (result.error) {
		console.error('[a11y] runtime scan command failed to launch:', result.error.message)
		process.exit(strictMode ? 1 : 0)
	}

	if (result.status && result.status !== 0) {
		if (strictMode) {
			console.error(`[a11y] runtime scan exited with code ${result.status}`)
			process.exit(result.status)
		}
		console.warn(`[a11y] runtime scan exited with code ${result.status}; continuing in report-only mode`)
	}

	process.exit(0)
}

main()
