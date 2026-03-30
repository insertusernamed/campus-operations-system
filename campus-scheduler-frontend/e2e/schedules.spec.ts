import { test, expect, type Page } from '@playwright/test';

function getNextIsoDateForDay(dayOfWeek: number, referenceDate = new Date()): string {
	const baseDate = new Date(referenceDate.getFullYear(), referenceDate.getMonth(), referenceDate.getDate());
	const diff = (dayOfWeek - baseDate.getDay() + 7) % 7;
	baseDate.setDate(baseDate.getDate() + diff);
	const year = baseDate.getFullYear();
	const month = String(baseDate.getMonth() + 1).padStart(2, '0');
	const day = String(baseDate.getDate()).padStart(2, '0');
	return `${year}-${month}-${day}`;
}

const sharedMondayBookingDate = getNextIsoDateForDay(1);

const mockSchedules: any[] = [];
const mockCourses = [{ id: 1, code: 'CS101', name: 'Intro' }];
const mockRooms = [{ id: 1, buildingCode: 'ENG', roomNumber: '101', capacity: 30 }];
const mockTimeSlots = [{ id: 1, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00' }];

const sharedBuilding = { id: 701, code: 'ENG', name: 'Engineering' };
const sharedRoom = {
	id: 603,
	roomNumber: '310',
	capacity: 18,
	type: 'SEMINAR',
	availabilityStatus: 'AVAILABLE',
	features: null,
	featureSet: [],
	accessibilityFlags: [],
	operationalNotes: null,
	lastInspectionDate: null,
	buildingId: sharedBuilding.id,
	buildingCode: sharedBuilding.code,
	buildingName: sharedBuilding.name,
};
const sharedTimeSlot = {
	id: 301,
	dayOfWeek: 'MONDAY',
	startTime: '09:00',
	endTime: '10:15',
	label: 'Morning Block',
};

async function mockSharedScheduleBoard(page: Page, roomBooking: Record<string, unknown>) {
	await page.route(/.*\/api\/schedules(?:\?.*)?$/, async route => {
		await route.fulfill({ json: [] });
	});

	await page.route(/.*\/api\/room-bookings(?:\?.*)?$/, async route => {
		await route.fulfill({ json: [roomBooking] });
	});

	await page.route(/.*\/api\/rooms$/, async route => {
		await route.fulfill({ json: [sharedRoom] });
	});

	await page.route(/.*\/api\/buildings$/, async route => {
		await route.fulfill({ json: [sharedBuilding] });
	});

	await page.route(/.*\/api\/timeslots$/, async route => {
		await route.fulfill({ json: [sharedTimeSlot] });
	});
}

test.describe('Schedules CRUD & Conflicts', () => {
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

		await main.locator('select').nth(0).selectOption({ index: 1 });
		await main.locator('select').nth(1).selectOption({ index: 1 });
		await main.locator('select').nth(2).selectOption({ index: 1 });
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

		const createResponse = page.waitForResponse(response =>
			response.request().method() === 'POST'
			&& response.url().endsWith('/api/schedules')
		);
		await main.getByRole('button', { name: 'Create' }).click({ force: true });
		await createResponse;
		await page.waitForURL('**/schedules');
	});

	test('should show api conflict error when create fails', async ({ page }) => {
		await page.route(/.*\/api\/schedules\/conflicts.*/, async route => {
			await route.fulfill({ json: { hasConflict: false } });
		});

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

test.describe('Room booking visibility on the shared schedules page', () => {
	test('admin can open a calendar booking and view student details', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'admin');
		});

		await mockSharedScheduleBoard(page, {
			id: 9002,
			room: sharedRoom,
			timeSlot: sharedTimeSlot,
			semester: 'Fall 2026',
			bookingDate: sharedMondayBookingDate,
			createdAt: '2026-03-30T12:00:00Z',
			participantCount: 2,
			viewerCanSeeStudentDetails: true,
			viewerIsOwner: true,
			viewerIsParticipant: true,
			bookedBy: {
				id: 101,
				fullName: 'Maya Patel',
				email: 'maya.patel@students.campus.edu',
			},
			participants: [
				{
					id: 102,
					fullName: 'Jonah Lee',
					email: 'jonah.lee@students.campus.edu',
				},
			],
		});

		await page.goto('/schedules', { waitUntil: 'domcontentloaded' });
		await page.getByLabel('Room Filter').selectOption(String(sharedRoom.id));
		await page.getByRole('button', { name: /Room Booking/ }).click();

		const dialog = page.getByRole('dialog', { name: 'Room Booking Details' });
		await expect(dialog.getByText('Booked by:')).toBeVisible();
		await expect(dialog.getByText('Maya Patel (maya.patel@students.campus.edu)')).toBeVisible();
		await expect(dialog.getByText('Invited students:')).toBeVisible();
		await expect(dialog.getByText('Jonah Lee')).toBeVisible();
		await expect(dialog.getByText('jonah.lee@students.campus.edu')).toBeVisible();
	});

	test('instructor can see a booking on the calendar without student identity details', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'instructor');
			localStorage.setItem('campus-operations-system-instructor-id', '11');
		});

		await mockSharedScheduleBoard(page, {
			id: 9003,
			room: sharedRoom,
			timeSlot: sharedTimeSlot,
			semester: 'Spring 2026',
			bookingDate: sharedMondayBookingDate,
			createdAt: '2026-03-30T12:30:00Z',
			participantCount: 2,
			viewerCanSeeStudentDetails: false,
			viewerIsOwner: false,
			viewerIsParticipant: false,
			bookedBy: null,
			participants: [],
		});

		await page.route(/.*\/api\/instructor-insights\/frictions.*/, async route => {
			await route.fulfill({ json: [] });
		});

		await page.goto('/schedules', { waitUntil: 'domcontentloaded' });
		await expect(page.locator('.sx__time-grid-event').first()).toBeVisible();
		await page.locator('.sx__time-grid-event').first().click();

		const dialog = page.getByRole('dialog', { name: 'Room Booking Details' });
		await expect(dialog.getByText('Student details are hidden for this booking.')).toBeVisible();
		await expect(dialog.getByText('Booked by:')).not.toBeVisible();
		await expect(dialog.getByText('Invited students:')).not.toBeVisible();
		await expect(dialog.getByText('maya.patel@students.campus.edu')).not.toBeVisible();
	});
});
