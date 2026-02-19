import { spawnSync } from 'node:child_process'

const forwardedArgs = process.argv.slice(2)
const npxBin = process.platform === 'win32' ? 'npx.cmd' : 'npx'

function runStep(label: string, scriptPath: string): void {
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
		return
	}

	if (result.status && result.status !== 0) {
		console.warn(`[a11y] ${label} exited with code ${result.status}; continuing`)
	}
}

function main(): void {
	runStep('discover routes', 'scripts/a11y/discover-routes.ts')
	runStep('runtime scan', 'scripts/a11y/runtime.ts')
	runStep('static lint scan', 'scripts/a11y/lint.ts')
	runStep('aggregate report', 'scripts/a11y/report.ts')

	console.log('[a11y] scan complete (report-only mode)')
}

main()
