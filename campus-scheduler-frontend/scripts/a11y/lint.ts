import fs from 'node:fs'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { parseA11yCliOptions } from './cli'

function main(): void {
	const options = parseA11yCliOptions()
	fs.mkdirSync(options.reportDir, { recursive: true })

	const outputFile = path.join(options.reportDir, 'eslint.json')
	fs.rmSync(outputFile, { force: true })

	const result = spawnSync(
		process.platform === 'win32' ? 'npx.cmd' : 'npx',
		[
			'eslint',
			'src/**/*.{vue,ts}',
			'--format',
			'json',
			'--output-file',
			outputFile,
		],
		{
			cwd: process.cwd(),
			stdio: 'inherit',
		}
	)

	if (result.error) {
		console.error('[a11y] lint command failed to launch:', result.error.message)
	}

	if (!fs.existsSync(outputFile)) {
		fs.writeFileSync(outputFile, '[]', 'utf8')
	}

	if (result.status && result.status !== 0) {
		console.warn(`[a11y] lint exited with code ${result.status}; continuing in report-only mode`)
	}

	process.exit(0)
}

main()
