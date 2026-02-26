import fs from 'node:fs'
import path from 'node:path'
import os from 'node:os'
import { spawnSync } from 'node:child_process'
import { applyA11yEnv, parseA11yCliOptions } from './cli'

function resolveWorkerCount(requestedWorkers: number | null): number {
	if (requestedWorkers && requestedWorkers > 0) {
		return requestedWorkers
	}

	const cpuCount = typeof os.availableParallelism === 'function'
		? os.availableParallelism()
		: os.cpus().length

	return Math.max(1, Math.min(8, cpuCount - 1))
}

function main(): void {
	const options = parseA11yCliOptions()
	const env = applyA11yEnv(options)
	const strictMode = options.strictMockGaps || options.strictRuntimeErrors || options.strictUncoveredRoutes
	const runtimeDir = path.join(options.reportDir, 'runtime')
	const playwrightArgs = ['playwright', 'test', '-c', 'playwright.a11y.config.ts']
	const workers = resolveWorkerCount(options.workers)
	const fullyParallel = options.fullyParallel || workers > 1

	if (fullyParallel) {
		playwrightArgs.push('--fully-parallel')
		if (!options.fullyParallel && workers > 1) {
			console.log('[a11y] auto-enabled fully-parallel mode')
		}
	}

	playwrightArgs.push(`--workers=${workers}`)
	if (!options.workers) {
		console.log(`[a11y] auto-selected workers=${workers}`)
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
