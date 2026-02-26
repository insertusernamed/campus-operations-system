import { expect, test, type Page } from '@playwright/test'

const instructors = [
	{
		id: 10,
		firstName: 'Ada',
		lastName: 'Lovelace',
		email: 'ada@campus.edu',
		department: 'Computer Science',
		officeNumber: 'CS-201',
	},
	{
		id: 11,
		firstName: 'Grace',
		lastName: 'Hopper',
		email: 'grace@campus.edu',
		department: 'Computer Science',
		officeNumber: 'CS-202',
	},
]

async function setupBaseMocks(page: Page) {
	await page.route(/.*\/api\/instructors$/, async route => {
		await route.fulfill({ json: instructors })
	})

	await page.route(/.*\/api\/generator\/stats$/, async route => {
		await route.fulfill({
			json: {
				buildings: 2,
				rooms: 8,
				instructors: instructors.length,
				courses: 12,
				schedules: 16,
			},
		})
	})

	await page.route(/.*\/api\/change-requests(\?.*)?$/, async route => {
		await route.fulfill({ json: [] })
	})

	await page.route(/.*\/api\/schedules(\?.*)?$/, async route => {
		await route.fulfill({ json: [] })
	})

	await page.route(/.*\/api\/instructor-insights\/frictions(\?.*)?$/, async route => {
		await route.fulfill({ json: [] })
	})

	await page.route(/.*\/api\/instructor-preferences\/room-feature-options$/, async route => {
		await route.fulfill({ json: [] })
	})

	await page.route(/.*\/api\/instructor-preferences\/\d+$/, async route => {
		await route.fulfill({
			json: {
				instructorId: 10,
				preferredStartTime: '08:00',
				preferredEndTime: '18:00',
				maxGapMinutes: 120,
				minTravelBufferMinutes: 15,
				avoidBuildingHops: true,
				preferredBuildingIds: [],
				requiredRoomFeatures: [],
				updatedAt: '2026-02-26T00:00:00Z',
			},
		})
	})
}

test.describe('Role switch instructor selection', () => {
	test('auto-selects first instructor when switching from admin', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'admin')
			localStorage.removeItem('campus-operations-system-instructor-id')
		})
		await setupBaseMocks(page)

		await page.goto('/')
		await page.getByRole('combobox', { name: 'Role' }).selectOption('instructor')
		await expect(page.getByRole('combobox', { name: 'Instructor' })).toHaveValue('10')
		await expect.poll(() => page.evaluate(() => localStorage.getItem('campus-operations-system-instructor-id')))
			.toBe('10')
	})

	test('replaces stale instructor id with the first available instructor', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'instructor')
			localStorage.setItem('campus-operations-system-instructor-id', '999')
		})
		await setupBaseMocks(page)

		await page.goto('/')
		await expect(page.getByRole('combobox', { name: 'Instructor' })).toHaveValue('10')
		await expect.poll(() => page.evaluate(() => localStorage.getItem('campus-operations-system-instructor-id')))
			.toBe('10')
	})
})
