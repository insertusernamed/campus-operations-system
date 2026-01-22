import { test, expect } from '@playwright/test';

test.describe('Instructors CRUD', () => {
    const mockInstructors = [
        { id: 1, firstName: 'John', lastName: 'Doe', email: 'john@example.com', department: 'CS' },
    ];

    test.beforeEach(async ({ page }) => {
        await page.route(/.*\/api\/instructors$/, async route => {
            const method = route.request().method();
            if (method === 'GET') {
                await route.fulfill({ json: mockInstructors });
            } else if (method === 'POST') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 2, ...data } });
            }
        });

        await page.route(/.*\/api\/instructors\/\d+$/, async route => {
            const method = route.request().method();
            if (method === 'GET') {
                await route.fulfill({ json: mockInstructors[0] });
            } else if (method === 'PUT') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 1, ...data } });
            } else if (method === 'DELETE') {
                await route.fulfill({ status: 204 });
            }
        });
    });

    test('should list instructors', async ({ page }) => {
        await page.goto('/instructors');
        await expect(page.getByText('Doe, John')).toBeVisible();
        await expect(page.getByText('john@example.com')).toBeVisible();
    });

    test('should create an instructor', async ({ page }) => {
        await page.goto('/instructors/new');

        await page.getByLabel('First Name').fill('Jane');
        await page.getByLabel('Last Name').fill('Smith');
        await page.getByLabel('Email').fill('jane@example.com');
        await page.getByLabel('Department').fill('Math');

        await page.getByRole('button', { name: 'Create' }).click();
        await expect(page).toHaveURL('/instructors');
    });

    test('should edit an instructor', async ({ page }) => {
        await page.goto('/instructors/1/edit');
        await expect(page.getByLabel('Email')).toHaveValue('john@example.com');

        await page.getByLabel('Email').fill('john.doe@example.com');
        await page.getByRole('button', { name: 'Update' }).click();
        await expect(page).toHaveURL('/instructors');
    });
});
