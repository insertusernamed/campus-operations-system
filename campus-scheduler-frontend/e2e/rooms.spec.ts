import { test, expect } from '@playwright/test';

test.describe('Rooms CRUD', () => {
	const mockRooms = [
		{ id: 1, roomNumber: '101', capacity: 30, buildingId: 1, buildingName: 'Engineering', buildingCode: 'ENG', type: 'CLASSROOM' }
	];
	const mockBuildings = [
		{ id: 1, name: 'Engineering', code: 'ENG' },
		{ id: 2, name: 'Science', code: 'SCI' }
	];

	test.beforeEach(async ({ page }) => {
		await page.route(/.*\/api\/rooms$/, async route => {
			if (route.request().method() === 'GET') await route.fulfill({ json: mockRooms });
			else await route.fulfill({ json: { id: 2, ...route.request().postDataJSON() } });
		});

		await page.route(/.*\/api\/rooms\/\d+$/, async route => {
			if (route.request().method() === 'GET') await route.fulfill({ json: mockRooms[0] });
			else await route.fulfill({ json: { id: 1, ...route.request().postDataJSON() } });
		});

		// Mock create room endpoint (POST /rooms/building/:buildingId)
		await page.route(/.*\/api\/rooms\/building\/\d+$/, async route => {
			const data = route.request().postDataJSON();
			await route.fulfill({ json: { id: 2, ...data } });
		});

		await page.route(/.*\/api\/buildings$/, async route => {
			await route.fulfill({ json: mockBuildings });
		});
	});

	test('should list rooms', async ({ page }) => {
		await page.goto('/rooms');
		await expect(page.getByText('101')).toBeVisible();
		await expect(page.getByRole('cell', { name: 'Engineering' })).toBeVisible();
	});

	test('should create a room', async ({ page }) => {
		await page.goto('/rooms/new');
		const main = page.locator('main');

		// Scope to <main> so navbar role controls do not affect field indexes.
		await main.locator('select').nth(0).selectOption({ index: 1 }); // Building
		await main.locator('input[type="text"]').first().fill('202'); // Room Number
		await main.locator('input[type="number"]').fill('25'); // Capacity
		await main.locator('select').nth(1).selectOption('LAB'); // Type

		await main.getByRole('button', { name: 'Create' }).click();
		await expect(page).toHaveURL('/rooms');
	});
});
