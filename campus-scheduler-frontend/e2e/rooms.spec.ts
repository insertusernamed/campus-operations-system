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

        await page.route(/.*\/api\/buildings$/, async route => {
            await route.fulfill({ json: mockBuildings });
        });
    });

    test('should list rooms', async ({ page }) => {
        await page.goto('/rooms');
        await expect(page.getByText('101')).toBeVisible();
        await expect(page.getByText('Engineering')).toBeVisible();
    });

    test('should create a room', async ({ page }) => {
        await page.goto('/rooms/new');

        // RoomForm uses selects and inputs without for/id
        await page.locator('select').first().selectOption({ index: 1 }); // Building
        await page.locator('input[type="text"]').first().fill('202'); // Room Number
        await page.locator('input[type="number"]').fill('25'); // Capacity
        await page.locator('select').nth(1).selectOption('LAB'); // Type

        await page.getByRole('button', { name: 'Create' }).click();
        await expect(page).toHaveURL('/rooms');
    });
});
