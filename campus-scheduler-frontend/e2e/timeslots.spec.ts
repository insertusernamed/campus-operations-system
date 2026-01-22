import { test, expect } from '@playwright/test';

test.describe('Time Slots CRUD', () => {
    const mockTimeSlots = [
        { id: 1, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00', label: 'Period 1' },
        { id: 2, dayOfWeek: 'WEDNESDAY', startTime: '10:00', endTime: '11:00', label: 'Period 2' }
    ];

    test.beforeEach(async ({ page }) => {
        await page.route(/.*\/api\/timeslots$/, async route => {
            if (route.request().method() === 'GET') await route.fulfill({ json: mockTimeSlots });
            else await route.fulfill({ json: { id: 3, ...route.request().postDataJSON() } });
        });

        await page.route(/.*\/api\/timeslots\/\d+$/, async route => {
            if (route.request().method() === 'GET') await route.fulfill({ json: mockTimeSlots[0] });
            else await route.fulfill({ json: { id: 1, ...route.request().postDataJSON() } });
        });
    });

    test('should list timeslots grouped by day', async ({ page }) => {
        await page.goto('/timeslots');
        await expect(page.getByText('Monday')).toBeVisible();
        await expect(page.getByText('Wednesday')).toBeVisible();
        await expect(page.getByText('09:00 - 10:00')).toBeVisible();
    });

    test('should create a time slot', async ({ page }) => {
        await page.goto('/timeslots/new');

        // TimeSlotForm: select for day, time inputs, text input for label
        await page.locator('select').selectOption('FRIDAY');
        await page.locator('input[type="time"]').first().fill('14:00');
        await page.locator('input[type="time"]').nth(1).fill('15:00');
        await page.locator('input[placeholder="e.g., Period 1"]').fill('Period 5');

        await page.getByRole('button', { name: 'Create' }).click();
        await expect(page).toHaveURL('/timeslots');
    });
});
