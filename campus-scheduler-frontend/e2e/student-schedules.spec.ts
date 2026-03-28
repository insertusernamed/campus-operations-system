import { expect, test } from '@playwright/test'

const students = [
	{
		id: 101,
		studentNumber: 'S100101',
		firstName: 'Maya',
		lastName: 'Patel',
		email: 'maya.patel@students.campus.edu',
		department: 'Computer Science',
		yearLevel: 3,
		targetCourseLoad: 4,
		preferredCourseIds: [1, 2, 3],
	},
]

const baseTimeSlot = {
	id: 301,
	dayOfWeek: 'MONDAY',
	startTime: '09:00',
	endTime: '10:15',
	label: 'Morning Block',
}

const enrolledSchedule = {
	id: 2001,
	course: {
		id: 501,
		code: 'CS301',
		name: 'Distributed Systems',
		description: null,
		credits: 3,
		enrollmentCapacity: 40,
		department: 'Computer Science',
		instructor: { id: 11, firstName: 'Grace', lastName: 'Hopper', email: 'grace@campus.edu' },
	},
	room: {
		id: 601,
		roomNumber: '210',
		capacity: 30,
		type: 'LECTURE_HALL',
		availabilityStatus: 'AVAILABLE',
		features: null,
		featureSet: [],
		accessibilityFlags: [],
		operationalNotes: null,
		lastInspectionDate: null,
		buildingId: 701,
		buildingCode: 'ENG',
		buildingName: 'Engineering',
	},
	timeSlot: baseTimeSlot,
	semester: 'Fall 2026',
}

const waitlistedSchedule = {
	id: 2002,
	course: {
		id: 502,
		code: 'CS341',
		name: 'Operating Systems',
		description: null,
		credits: 3,
		enrollmentCapacity: 35,
		department: 'Computer Science',
		instructor: { id: 12, firstName: 'Ada', lastName: 'Lovelace', email: 'ada@campus.edu' },
	},
	room: {
		id: 602,
		roomNumber: '120',
		capacity: 20,
		type: 'CLASSROOM',
		availabilityStatus: 'AVAILABLE',
		features: null,
		featureSet: [],
		accessibilityFlags: [],
		operationalNotes: null,
		lastInspectionDate: null,
		buildingId: 702,
		buildingCode: 'SCI',
		buildingName: 'Science',
	},
	timeSlot: {
		id: 302,
		dayOfWeek: 'WEDNESDAY',
		startTime: '13:00',
		endTime: '14:15',
		label: 'Afternoon Block',
	},
	semester: 'Fall 2026',
}

test.describe('Student schedule UI', () => {
	test.beforeEach(async ({ page }) => {
		await page.route(/.*\/api\/students$/, async route => {
			await route.fulfill({ json: students })
		})
	})

	test('shows enrolled and waitlisted classes separately with seat pressure data', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'student')
			localStorage.setItem('campus-operations-system-student-id', '101')
		})

		await page.route(/.*\/api\/enrollments\?studentId=101$/, async route => {
			await route.fulfill({
				json: [
					{ id: 1, semester: 'Fall 2026', status: 'ENROLLED', student: null, schedule: null },
					{ id: 2, semester: 'Fall 2026', status: 'WAITLISTED', student: null, schedule: null },
				],
			})
		})

		await page.route(/.*\/api\/students\/101\/schedule\?semester=Fall\+2026$/, async route => {
			await route.fulfill({
				json: {
					studentId: 101,
					semester: 'Fall 2026',
					enrolled: [{ id: 1, semester: 'Fall 2026', status: 'ENROLLED', student: null, schedule: enrolledSchedule }],
					waitlisted: [{ id: 2, semester: 'Fall 2026', status: 'WAITLISTED', student: null, schedule: waitlistedSchedule }],
				},
			})
		})

		await page.route(/.*\/api\/schedules\?semester=Fall\+2026$/, async route => {
			await route.fulfill({
				json: [
					{ ...enrolledSchedule, filledSeats: 18, seatLimit: 20, remainingSeats: 2, waitlistCount: 5 },
					{ ...waitlistedSchedule, filledSeats: 20, seatLimit: 20, remainingSeats: 0, waitlistCount: 7 },
					{
						...enrolledSchedule,
						id: 2999,
						course: { ...enrolledSchedule.course, code: 'CS499', name: 'Unrelated Capstone' },
						filledSeats: 10,
						seatLimit: 25,
						remainingSeats: 15,
						waitlistCount: 0,
					},
				],
			})
		})

		await page.goto('/schedules')

		await expect(page.getByRole('heading', { name: 'My Schedule' })).toBeVisible()
		await expect(page.getByRole('heading', { name: 'Enrolled Classes' })).toBeVisible()
		await expect(page.getByRole('heading', { name: 'Waitlisted Classes' })).toBeVisible()
		await expect(page.getByText('CS301')).toBeVisible()
		await expect(page.getByText('CS341')).toBeVisible()
		await expect(page.getByText('18/20 seats (90%)')).toBeVisible()
		await expect(page.getByText('2 seats left, 5 waitlisted')).toBeVisible()
		await expect(page.getByText('Unrelated Capstone')).not.toBeVisible()

		const waitlistedSection = page.locator('section').filter({ hasText: 'Waitlisted Classes' })
		await waitlistedSection.getByRole('button', { name: 'Details' }).first().click()
		await expect(page.getByText('Status: Waitlisted')).toBeVisible()
	})
})

test.describe('Shared schedule seat metrics', () => {
	test('renders backend seat and waitlist summaries for admin schedule rows', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'admin')
		})

		await page.route(/.*\/api\/schedules$/, async route => {
			await route.fulfill({
				json: [
					{
						...enrolledSchedule,
						filledSeats: 12,
						seatLimit: 30,
						remainingSeats: 18,
						waitlistCount: 4,
					},
				],
			})
		})

		await page.route(/.*\/api\/rooms$/, async route => {
			await route.fulfill({
				json: [
					{
						id: 601,
						roomNumber: '210',
						capacity: 30,
						type: 'LECTURE_HALL',
						availabilityStatus: 'AVAILABLE',
						features: null,
						buildingId: 701,
						buildingCode: 'ENG',
						buildingName: 'Engineering',
					},
				],
			})
		})

		await page.route(/.*\/api\/buildings$/, async route => {
			await route.fulfill({
				json: [{ id: 701, code: 'ENG', name: 'Engineering' }],
			})
		})

		await page.goto('/schedules')
		await page.getByRole('button', { name: 'Table' }).click()

		await expect(page.getByText('12/30 seats (40%)')).toBeVisible()
		await expect(page.getByText('18 seats left, 4 waitlisted')).toBeVisible()
		await expect(page.getByRole('cell', { name: '4', exact: true })).toBeVisible()
	})
})
