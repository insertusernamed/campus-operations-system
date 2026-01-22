import { test, expect } from '@playwright/test';

test.describe('Courses CRUD', () => {
    const mockCourses = [
        { id: 1, code: 'CS101', name: 'Intro to CS', credits: 3, department: 'CS', instructor: { id: 1, firstName: 'John', lastName: 'Doe' } }
    ];
    const mockInstructors = [
        { id: 1, firstName: 'John', lastName: 'Doe' },
        { id: 2, firstName: 'Jane', lastName: 'Smith' }
    ];

    test.beforeEach(async ({ page }) => {
        await page.route(/.*\/api\/courses$/, async route => {
            const method = route.request().method();
            if (method === 'GET') await route.fulfill({ json: mockCourses });
            if (method === 'POST') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 2, ...data } });
            }
        });

        await page.route(/.*\/api\/courses\/\d+$/, async route => {
            const method = route.request().method();
            if (method === 'GET') await route.fulfill({ json: mockCourses[0] });
            if (method === 'PUT') await route.fulfill({ json: { id: 1, ...route.request().postDataJSON() } });
            if (method === 'DELETE') await route.fulfill({ status: 204 });
        });

        await page.route(/.*\/api\/instructors$/, async route => {
            await route.fulfill({ json: mockInstructors });
        });

        // Mock createWithInstructor endpoint (POST /courses/instructor/:instructorId)
        await page.route(/.*\/api\/courses\/instructor\/\d+$/, async route => {
            if (route.request().method() === 'POST') {
                const data = route.request().postDataJSON();
                await route.fulfill({ json: { id: 2, ...data } });
            } else {
                await route.fulfill({ json: mockCourses });
            }
        });

        await page.route(/.*\/api\/courses\/\d+\/instructor\/\d+$/, async route => {
            await route.fulfill({ json: {} });
        });
    });

    test('should list courses', async ({ page }) => {
        await page.goto('/courses');
        await expect(page.getByText('CS101')).toBeVisible();
        await expect(page.getByText('John Doe')).toBeVisible();
    });

    test('should create a course with instructor', async ({ page }) => {
        await page.goto('/courses/new');

        // CourseForm uses v-model directly, need to use input placeholders or nth selectors
        await page.locator('input[placeholder="e.g., COMP 4431"]').fill('CS102');
        await page.locator('input').nth(1).fill('Data Structures');
        await page.locator('input[type="number"]').first().fill('3');
        await page.locator('input[type="number"]').nth(1).fill('40');

        // Instructor is the only select on the form (index 0)
        await page.locator('select').first().selectOption({ index: 1 });

        await page.getByRole('button', { name: 'Create' }).click();
        await expect(page).toHaveURL('/courses');
    });
});
