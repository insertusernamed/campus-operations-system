import { test, expect } from '@playwright/test';

test.describe('Schedules CRUD & Conflicts', () => {
	const mockSchedules: any[] = [];
	const mockCourses = [{ id: 1, code: 'CS101', name: 'Intro' }];
	const mockRooms = [{ id: 1, buildingCode: 'ENG', roomNumber: '101', capacity: 30 }];
	const mockTimeSlots = [{ id: 1, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }];

	test.beforeEach(async ({ page }) => {
		await page.route(/.*\/api\/schedules$/, async route => {
			if (route.request().method() === 'GET') await route.fulfill({ json: mockSchedules });
			else await route.fulfill({ json: { id: 1, ...route.request().postDataJSON() } });
		});

		await page.route(/.*\/api\/courses$/, async route => route.fulfill({ json: mockCourses }));
		await page.route(/.*\/api\/rooms$/, async route => route.fulfill({ json: mockRooms }));
		await page.route(/.*\/api\/timeslots$/, async route => route.fulfill({ json: mockTimeSlots }));
	});

	test('should show conflict warning and disable submit', async ({ page }) => {
		await page.route(/.*\/api\/schedules\/conflicts.*/, async route => {
			await route.fulfill({ json: { hasConflict: true, conflictingSchedule: { id: 99 } } });
		});

		await page.goto('/schedules/new');
		const main = page.locator('main');

		// Scope to <main> so navbar role controls are excluded.
		await main.locator('select').nth(0).selectOption({ index: 1 }); // Course
		await main.locator('select').nth(1).selectOption({ index: 1 }); // Room
		await main.locator('select').nth(2).selectOption({ index: 1 }); // Time Slot
		await main.locator('input[placeholder="e.g., Fall 2026"]').fill('Fall 2026');

		await expect(main.getByText('This room is already booked at this time!')).toBeVisible({ timeout: 10000 });
		await expect(main.getByRole('button', { name: 'Create' })).toBeDisabled();
	});

    test('should allow creation when no conflict', async ({ page }) => {
        await page.route(/.*\/api\/schedules\/conflicts.*/, async route => {
            await route.fulfill({ json: { hasConflict: false } });
        });

		await page.goto('/schedules/new');
		const main = page.locator('main');

		await main.locator('select').nth(0).selectOption({ index: 1 });
		await main.locator('select').nth(1).selectOption({ index: 1 });
		await main.locator('select').nth(2).selectOption({ index: 1 });
		await main.locator('input[placeholder="e.g., Fall 2026"]').fill('Fall 2026');

		await expect(main.getByText('This room is already booked')).not.toBeVisible();
		await expect(main.getByRole('button', { name: 'Create' })).toBeEnabled();

        await main.getByRole('button', { name: 'Create' }).click();
        await expect(page).toHaveURL('/schedules');
    });

    test('should show api conflict error when create fails', async ({ page }) => {
        await page.route(/.*\/api\/schedules\/conflicts.*/, async route => {
            await route.fulfill({ json: { hasConflict: false } });
        });

        // Override POST /api/schedules to emulate backend validation conflict.
        await page.route(/.*\/api\/schedules$/, async route => {
            if (route.request().method() === 'POST') {
                await route.fulfill({
                    status: 409,
                    contentType: 'application/json',
                    json: { error: 'Room ENG 101 is already booked for this time slot in Fall 2026' },
                });
                return;
            }
            await route.fulfill({ json: mockSchedules });
        });

        await page.goto('/schedules/new');
        const main = page.locator('main');

        await main.locator('select').nth(0).selectOption({ index: 1 });
        await main.locator('select').nth(1).selectOption({ index: 1 });
        await main.locator('select').nth(2).selectOption({ index: 1 });
        await main.locator('input[placeholder="e.g., Fall 2026"]').fill('Fall 2026');

        await main.getByRole('button', { name: 'Create' }).click();

        await expect(page).toHaveURL('/schedules/new');
        await expect(main.getByText('Room ENG 101 is already booked for this time slot in Fall 2026')).toBeVisible();
    });
});
