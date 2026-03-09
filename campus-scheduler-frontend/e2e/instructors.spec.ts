import { test, expect } from '@playwright/test';

test.describe('Instructors CRUD', () => {
	const mockInstructors = [
		{ id: 1, firstName: 'Ada', lastName: 'Lovelace', email: 'ada@example.com', department: 'Computer Science' },
	];

	const queueRows = [
		{
			id: 1,
			firstName: 'Ada',
			lastName: 'Lovelace',
			fullName: 'Ada Lovelace',
			email: 'ada@example.com',
			department: 'Computer Science',
			assignedCoursesCount: 2,
			assignedCredits: 6,
			targetCreditsMin: 6,
			targetCreditsMax: 12,
			loadStatus: 'BALANCED',
			preferenceCompletenessPercent: 80,
			frictionScore: 4,
			frictionIssueCount: 2,
			frictionSeverity: 'MEDIUM',
			coverageRiskLevel: 'MEDIUM',
			status: 'COVERAGE_RISK',
			overloadCredits: 0,
			underUtilizedCredits: 0,
			recommendedActions: ['Assign to open course(s)'],
		},
	]

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

		await page.route(/.*\/api\/semesters$/, async route => {
			await route.fulfill({
				json: [
					{
						term: 'FALL',
						displayName: 'Fall',
						startMonth: 9,
						startDay: 21,
						endMonth: 12,
						endDay: 20,
						startYearOffset: 0,
						endYearOffset: 0,
					},
				],
			});
		});

		await page.route(/.*\/api\/instructor-insights\/summary(\?.*)?$/, async route => {
			await route.fulfill({
				json: {
					totalInstructors: 1,
					noCurrentAssignment: 0,
					overloadRisk: 0,
					preferenceSetupIncomplete: 0,
					frictionHotspots: 0,
					departmentsWithCoverageRisk: 1,
				},
			});
		});

		await page.route(/.*\/api\/instructor-insights\/queue(\?.*)?$/, async route => {
			await route.fulfill({ json: queueRows });
		});

		await page.route(/.*\/api\/instructor-insights\/load-distribution(\?.*)?$/, async route => {
			await route.fulfill({
				json: {
					semester: 'Fall 2026',
					departments: [
						{
							department: 'Computer Science',
							instructorCount: 1,
							assignedCredits: 6,
							targetCreditsMin: 6,
							targetCreditsMax: 12,
							unfilledCourseCount: 1,
							unfilledCredits: 3,
							coverageRiskLevel: 'MEDIUM',
						},
					],
				},
			});
		});

		await page.route(/.*\/api\/courses(\?.*)?$/, async route => {
			await route.fulfill({ json: [] });
		});
	});

	test('should list instructors', async ({ page }) => {
		await page.goto('/instructors');
		await expect(page.getByText('Ada Lovelace')).toBeVisible();
		await expect(page.getByRole('table').getByText('Coverage Risk')).toBeVisible();
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
		await expect(page.getByLabel('Email')).toHaveValue('ada@example.com');

		await page.getByLabel('Email').fill('ada.lovelace@example.com');
		await page.getByRole('button', { name: 'Update' }).click();
		await expect(page).toHaveURL('/instructors');
	});
});
