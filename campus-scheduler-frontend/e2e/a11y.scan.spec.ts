import fs from 'node:fs'
import path from 'node:path'
import { test } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'
import { parseA11yCliOptions, routeMatchesFilter } from '../scripts/a11y/cli'
import { installA11yMockApi } from './a11y/mockApi'
import type {
	A11yRole,
	A11yRouteManifest,
	A11yRouteTarget,
	A11yScanResult,
	A11yTheme,
	A11yViolation,
} from '../scripts/a11y/types'

const options = parseA11yCliOptions()
const runtimeDir = path.join(options.reportDir, 'runtime')
const manifestPath = path.join(options.reportDir, 'manifest.json')

function readManifest(filePath: string): A11yRouteManifest {
	if (!fs.existsSync(filePath)) {
		throw new Error(`A11y route manifest not found: ${filePath}`)
	}
	return JSON.parse(fs.readFileSync(filePath, 'utf8')) as A11yRouteManifest
}

function buildTargets(manifest: A11yRouteManifest): A11yRouteTarget[] {
	const roles: A11yRole[] = options.roles ?? ['admin', 'instructor']
	const themes: A11yTheme[] = options.themes ?? ['snow-storm', 'slate']
	const routeEntries = manifest.routes.filter(entry => routeMatchesFilter(entry.route, options.routeFilters))

	const targets: A11yRouteTarget[] = []
	for (const entry of routeEntries) {
		for (const role of roles) {
			for (const theme of themes) {
				targets.push({
					route: entry.route,
					template: entry.template,
					role,
					theme,
					source: entry.source,
				})
			}
		}
	}

	return targets
}

function sanitizeFileName(value: string): string {
	return value
		.replace(/^\//, '')
		.replace(/[^a-zA-Z0-9._-]+/g, '_')
		.replace(/_+/g, '_')
		.replace(/^_/, '') || 'root'
}

function buildResultFilePath(target: A11yRouteTarget): string {
	const id = `${target.role}__${target.theme}__${sanitizeFileName(target.route)}`
	return path.join(runtimeDir, `${id}.json`)
}

function dedupeViolations(violations: A11yViolation[]): A11yViolation[] {
	const map = new Map<string, A11yViolation>()

	for (const violation of violations) {
		const key = [
			violation.source,
			violation.ruleId,
			violation.impact,
			violation.selector,
			violation.message,
			violation.helpUrl || '',
		].join('||')

		if (!map.has(key)) {
			map.set(key, violation)
		}
	}

	return Array.from(map.values())
}

function axeToViolations(result: Awaited<ReturnType<AxeBuilder['analyze']>>): A11yViolation[] {
	const violations: A11yViolation[] = []

	for (const violation of result.violations) {
		for (const node of violation.nodes) {
			violations.push({
				source: 'axe',
				ruleId: violation.id,
				impact: violation.impact ?? 'unknown',
				selector: node.target.join(' '),
				message: node.failureSummary || violation.description,
				helpUrl: violation.helpUrl,
			})
		}
	}

	return violations
}

async function runKeyboardFlow(page: import('@playwright/test').Page): Promise<void> {
	const focusableCount = await page
		.locator('a[href], button, input, select, textarea, [tabindex]:not([tabindex="-1"])')
		.count()

	const maxTabs = Math.min(Math.max(focusableCount + 2, 8), 40)

	for (let i = 0; i < maxTabs; i++) {
		await page.keyboard.press('Tab')
		await page.waitForTimeout(30)
	}
}

async function runCustomChecks(page: import('@playwright/test').Page): Promise<A11yViolation[]> {
	return page.evaluate(() => {
		type RawIssue = {
			source: 'custom'
			ruleId: string
			impact: string
			selector: string
			message: string
			helpUrl?: string
			context?: string
		}

		function cssPath(element: Element): string {
			const parts: string[] = []
			let current: Element | null = element
			let depth = 0

			while (current && depth < 5) {
				let part = current.tagName.toLowerCase()
				if (current.id) {
					part += `#${current.id}`
					parts.unshift(part)
					break
				}

				const className = (current.getAttribute('class') || '')
					.split(/\s+/)
					.filter(Boolean)
					.slice(0, 2)
					.join('.')
				if (className) {
					part += `.${className}`
				}

				const parent = current.parentElement
				if (parent) {
					const siblings = Array.from(parent.children).filter(child => child.tagName === current?.tagName)
					if (siblings.length > 1) {
						const index = siblings.indexOf(current)
						part += `:nth-of-type(${index + 1})`
					}
				}

				parts.unshift(part)
				current = parent
				depth += 1
			}

			return parts.join(' > ')
		}

		function hasAccessibleName(element: HTMLElement): boolean {
			const ariaLabel = element.getAttribute('aria-label')
			if (ariaLabel && ariaLabel.trim().length > 0) return true

			const ariaLabelledBy = element.getAttribute('aria-labelledby')
			if (ariaLabelledBy) {
				const ids = ariaLabelledBy.split(/\s+/).filter(Boolean)
				const hasLabel = ids.some(id => {
					const labelEl = document.getElementById(id)
					return !!labelEl && (labelEl.textContent || '').trim().length > 0
				})
				if (hasLabel) return true
			}

			const title = element.getAttribute('title')
			if (title && title.trim().length > 0) return true

			if (element instanceof HTMLInputElement) {
				if (element.type === 'button' || element.type === 'submit' || element.type === 'reset') {
					return (element.value || '').trim().length > 0
				}
			}

			const text = (element.textContent || '').trim()
			if (text.length > 0) return true

			const alt = element.getAttribute('alt')
			if (alt && alt.trim().length > 0) return true

			return false
		}

		const issues: RawIssue[] = []

		const interactive = Array.from(
			document.querySelectorAll<HTMLElement>(
				'button, [role="button"], a[href], input:not([type="hidden"]), select, textarea'
			)
		)

		for (const element of interactive) {
			if (!hasAccessibleName(element)) {
				issues.push({
					source: 'custom',
					ruleId: 'custom-accessible-name',
					impact: 'serious',
					selector: cssPath(element),
					message: 'Interactive element is missing an accessible name',
					context: element.outerHTML.slice(0, 240),
				})
			}
		}

		const imagesWithoutAlt = Array.from(document.querySelectorAll<HTMLImageElement>('img:not([alt])'))
		for (const image of imagesWithoutAlt) {
			issues.push({
				source: 'custom',
				ruleId: 'custom-image-alt',
				impact: 'moderate',
				selector: cssPath(image),
				message: 'Image element is missing an alt attribute',
				context: image.outerHTML.slice(0, 240),
			})
		}

		const headings = Array.from(document.querySelectorAll<HTMLElement>('h1, h2, h3, h4, h5, h6'))
		let previousLevel = 0
		for (const heading of headings) {
			const level = Number(heading.tagName.slice(1))
			if (previousLevel > 0 && level - previousLevel > 1) {
				issues.push({
					source: 'custom',
					ruleId: 'custom-heading-order',
					impact: 'moderate',
					selector: cssPath(heading),
					message: `Heading level jumped from h${previousLevel} to h${level}`,
					context: heading.outerHTML.slice(0, 240),
				})
			}
			previousLevel = level
		}

		const ids = new Map<string, Element[]>()
		const withIds = Array.from(document.querySelectorAll<HTMLElement>('[id]'))
		for (const element of withIds) {
			const id = element.id.trim()
			if (!id) continue
			const list = ids.get(id) ?? []
			list.push(element)
			ids.set(id, list)
		}

		for (const [id, elements] of ids.entries()) {
			if (elements.length <= 1) continue
			for (const element of elements) {
				issues.push({
					source: 'custom',
					ruleId: 'custom-duplicate-id',
					impact: 'serious',
					selector: cssPath(element),
					message: `Duplicate id attribute found: "${id}"`,
					context: element.outerHTML.slice(0, 240),
				})
			}
		}

		return issues
	})
}

async function writeResult(result: A11yScanResult): Promise<void> {
	fs.mkdirSync(runtimeDir, { recursive: true })
	const filePath = buildResultFilePath(result.target)
	fs.writeFileSync(filePath, JSON.stringify(result, null, 2), 'utf8')
}

const manifest = readManifest(manifestPath)
const targets = buildTargets(manifest)

if (targets.length === 0) {
	test('a11y runtime scanner has targets', () => {
		throw new Error('No targets discovered for accessibility runtime scan')
	})
}

for (const target of targets) {
	test(`a11y ${target.role} ${target.theme} ${target.route}`, async ({ page }) => {
		const runtimeErrors: string[] = []

		const result: A11yScanResult = {
			target,
			scannedAt: new Date().toISOString(),
			documentTitle: '',
			finalUrl: '',
			violations: [],
			mockGaps: [],
			runtimeErrors,
		}

		page.on('console', message => {
			if (message.type() === 'error') {
				runtimeErrors.push(`console: ${message.text()}`)
			}
		})

		try {
			await page.addInitScript(({ role, theme }) => {
				localStorage.setItem('campus-operations-system-role', role)
				localStorage.setItem('campus-operations-system-theme', theme)

				const applyTheme = () => {
					if (document.documentElement) {
						document.documentElement.setAttribute('data-theme', theme)
					}
				}
				applyTheme()
				if (!document.documentElement) {
					document.addEventListener('DOMContentLoaded', applyTheme, { once: true })
				}

				if (role === 'instructor') {
					localStorage.setItem('campus-operations-system-instructor-id', '10')
				} else {
					localStorage.removeItem('campus-operations-system-instructor-id')
				}
			}, {
				role: target.role,
				theme: target.theme,
			})

			const { mockGaps } = await installA11yMockApi(page, {
				route: target.route,
				role: target.role,
				theme: target.theme,
			})

			await page.goto(target.route, { waitUntil: 'domcontentloaded' })
			await page.waitForLoadState('networkidle', { timeout: 5000 }).catch(() => undefined)
			await page.waitForTimeout(200)

			const firstAxe = await new AxeBuilder({ page })
				.withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
				.analyze()

			await runKeyboardFlow(page)

			const secondAxe = await new AxeBuilder({ page })
				.withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'])
				.analyze()

			const customChecks = await runCustomChecks(page)

			const allViolations = dedupeViolations([
				...axeToViolations(firstAxe),
				...axeToViolations(secondAxe),
				...customChecks,
			])

			result.scannedAt = new Date().toISOString()
			result.documentTitle = await page.title()
			result.finalUrl = page.url()
			result.violations = allViolations
			result.mockGaps = mockGaps
		} catch (error) {
			const message = error instanceof Error ? error.stack || error.message : String(error)
			runtimeErrors.push(`runtime: ${message}`)
			result.scannedAt = new Date().toISOString()
			result.documentTitle = await page.title().catch(() => '')
			result.finalUrl = page.url()
		} finally {
			await writeResult(result)
		}
	})
}
