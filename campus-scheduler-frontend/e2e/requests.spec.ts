import { test, expect } from '@playwright/test';

test.describe('Change Requests Workflow', () => {
    test('instructor can submit request and admin can approve it', async ({ page }) => {
        const instructors = [
            {
                id: 10,
                firstName: 'Ada',
                lastName: 'Lovelace',
                email: 'ada@campus.edu',
                department: 'Computer Science',
                officeNumber: 'CS-201',
            },
        ];

        const rooms = [
            {
                id: 1,
                buildingId: 1,
                buildingCode: 'ENG',
                buildingName: 'Engineering',
                roomNumber: '101',
                capacity: 30,
                type: 'CLASSROOM',
                features: 'Projector',
            },
            {
                id: 2,
                buildingId: 1,
                buildingCode: 'ENG',
                buildingName: 'Engineering',
                roomNumber: '202',
                capacity: 40,
                type: 'CLASSROOM',
                features: 'Projector',
            },
        ];

        const timeSlots = [
            { id: 1, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00', label: 'Period 1' },
            { id: 2, dayOfWeek: 'TUESDAY', startTime: '10:00', endTime: '11:00', label: 'Period 2' },
        ];

        const schedule = {
            id: 50,
            semester: 'Fall 2026',
            course: {
                id: 20,
                code: 'CS101',
                name: 'Intro to Programming',
                description: null,
                credits: 3,
                enrollmentCapacity: 30,
                department: 'Computer Science',
                instructor: {
                    id: 10,
                    firstName: 'Ada',
                    lastName: 'Lovelace',
                    email: 'ada@campus.edu',
                },
            },
            room: rooms[0],
            timeSlot: timeSlots[0],
        };

        const changeRequests: any[] = [];

        await page.addInitScript(() => {
            localStorage.setItem('campus-operations-system-role', 'instructor');
            localStorage.setItem('campus-operations-system-instructor-id', '10');
        });

        await page.route(/.*\/api\/instructors$/, async route => {
            await route.fulfill({ json: instructors });
        });

        await page.route(/.*\/api\/schedules(\?.*)?$/, async route => {
            await route.fulfill({ json: [schedule] });
        });

        await page.route(/.*\/api\/rooms$/, async route => {
            await route.fulfill({ json: rooms });
        });

        await page.route(/.*\/api\/timeslots$/, async route => {
            await route.fulfill({ json: timeSlots });
        });

        await page.route(/.*\/api\/change-requests\/validate$/, async route => {
            await route.fulfill({
                json: {
                    green: true,
                    hardConflicts: [],
                    softWarnings: [],
                },
            });
        });

        await page.route(/.*\/api\/change-requests\/\d+\/approve$/, async route => {
            const requestId = Number(route.request().url().match(/change-requests\/(\d+)\/approve/)?.[1] ?? 0);
            const existing = changeRequests.find(item => item.id === requestId);

            if (!existing) {
                await route.fulfill({ status: 404 });
                return;
            }

            existing.status = 'APPROVED';
            existing.decisionNote = route.request().postDataJSON()?.decisionNote ?? null;

            await route.fulfill({ json: existing });
        });

        await page.route(/.*\/api\/change-requests(\?.*)?$/, async route => {
            if (route.request().method() === 'POST') {
                const payload = route.request().postDataJSON();

                const next = {
                    id: 1,
                    schedule,
                    requestedByInstructor: instructors[0],
                    requestedByRole: payload.requestedByRole,
                    status: 'PENDING',
                    reasonCategory: payload.reasonCategory,
                    reasonDetails: payload.reasonDetails ?? null,
                    proposedRoom: rooms.find(room => room.id === payload.proposedRoomId) ?? null,
                    proposedTimeSlot: timeSlots.find(slot => slot.id === payload.proposedTimeSlotId) ?? null,
                    originalRoomId: schedule.room.id,
                    originalTimeSlotId: schedule.timeSlot.id,
                    originalSemester: schedule.semester,
                    decisionNote: null,
                    createdAt: '2026-02-12T12:00:00Z',
                    reviewedAt: null,
                    appliedAt: null,
                };

                changeRequests.splice(0, changeRequests.length, next);
                await route.fulfill({ status: 201, json: next });
                return;
            }

            const url = new URL(route.request().url());
            const instructorId = url.searchParams.get('instructorId');
            if (instructorId) {
                await route.fulfill({
                    json: changeRequests.filter(item => item.requestedByInstructor.id === Number(instructorId)),
                });
                return;
            }

            await route.fulfill({ json: changeRequests });
        });

        await page.goto('/requests/new');
        const main = page.locator('main');

        // Form has selects: schedule, issue, proposed room, proposed time slot.
        await main.locator('select').nth(0).selectOption({ index: 1 });
        await main.locator('select').nth(1).selectOption({ index: 1 });
        await main.locator('select').nth(2).selectOption({ index: 2 });
        await main.locator('select').nth(3).selectOption({ index: 1 });
        await main.getByRole('button', { name: 'Submit Request' }).click();

        await expect(page).toHaveURL('/requests');
        await expect(main.getByText('PENDING', { exact: true })).toBeVisible();

        // Switch to admin and approve the request.
        const roleSelect = page.getByRole('combobox', { name: 'Role' });
        await roleSelect.selectOption('admin');
        await expect(roleSelect).toHaveValue('admin');
        const adminRequestsLink = page.getByRole('link', { name: 'Requests' });
        await expect(adminRequestsLink).toBeVisible();
        await adminRequestsLink.click();
        await expect(page).toHaveURL('/requests/admin');

        const adminMain = page.locator('main');
        await expect(adminMain.locator('tbody').getByText('PENDING', { exact: true })).toBeVisible();
        await adminMain.getByRole('button', { name: 'Review' }).click();
        await page.getByRole('button', { name: 'Approve' }).click();

        await expect(adminMain.locator('tbody').getByText('APPROVED', { exact: true })).toBeVisible();
    });
});
