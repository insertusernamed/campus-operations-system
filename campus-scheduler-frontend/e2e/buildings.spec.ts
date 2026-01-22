import { test, expect } from '@playwright/test';

test.describe('Buildings CRUD', () => {
    const mockBuildings = [
        { id: 1, name: 'Engineering Building', code: 'ENG', address: '123 Tech Lane' },
        { id: 2, name: 'Science Center', code: 'SCI', address: '456 Lab Road' },
    ];

    test.beforeEach(async ({ page }) => {
        // Log console and network to help debug
        page.on('console', msg => console.log(`BROWSER: ${msg.text()}`));
        page.on('request', req => {
            if (req.url().includes('/api/')) {
                console.log(`REQUEST: ${req.method()} ${req.url()}`);
            }
        });
        page.on('response', res => {
            if (res.url().includes('/api/')) {
                console.log(`RESPONSE: ${res.status()} ${res.url()}`);
            }
        });

        // Use regex for reliable matching
        await page.route(/.*\/api\/buildings$/, async route => {
            const method = route.request().method();
            console.log(`MOCK INTERCEPTED: ${method} /api/buildings`);
            if (method === 'GET') {
                await route.fulfill({ json: mockBuildings });
            } else if (method === 'POST') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 3, ...data } });
            }
        });

        await page.route(/.*\/api\/buildings\/\d+$/, async route => {
            const method = route.request().method();
            console.log(`MOCK INTERCEPTED: ${method} /api/buildings/:id`);
            if (method === 'GET') {
                await route.fulfill({ json: mockBuildings[0] });
            } else if (method === 'PUT') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 1, ...data } });
            } else if (method === 'DELETE') {
                await route.fulfill({ status: 204 });
            }
        });
    });

    test('should list buildings', async ({ page }) => {
        await page.goto('/buildings');
        await expect(page.getByText('Engineering Building')).toBeVisible();
        await expect(page.getByText('Science Center')).toBeVisible();
    });

    test('should create a building', async ({ page }) => {
        await page.goto('/buildings/new');

        await page.getByLabel('Code').fill('ART');
        await page.getByLabel('Name').fill('Arts Studio');
        await page.getByLabel('Address').fill('789 Creative Way');

        await page.getByRole('button', { name: 'Create' }).click();

        // Should navigate back to list
        await expect(page).toHaveURL('/buildings');
    });

    test('should edit a building', async ({ page }) => {
        await page.goto('/buildings/1/edit');

        // Verify pre-filled data
        await expect(page.getByLabel('Name')).toHaveValue('Engineering Building');

        await page.getByLabel('Name').fill('Engineering Complex');
        await page.getByRole('button', { name: 'Update' }).click();

        await expect(page).toHaveURL('/buildings');
    });
});
