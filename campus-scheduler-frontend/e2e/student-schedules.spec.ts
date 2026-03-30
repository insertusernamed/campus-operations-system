import { expect, test, type Page } from '@playwright/test'

const primaryBookingDate = '2026-03-30'

const ownerStudent = {
	id: 101,
	studentNumber: 'S100101',
	firstName: 'Maya',
	lastName: 'Patel',
	email: 'maya.patel@students.campus.edu',
	department: 'Computer Science',
	yearLevel: 3,
	targetCourseLoad: 4,
	preferredCourseIds: [1, 2, 3],
}

const invitedStudent = {
	id: 102,
	studentNumber: 'S100102',
	firstName: 'Jonah',
	lastName: 'Lee',
	email: 'jonah.lee@students.campus.edu',
	department: 'Computer Science',
	yearLevel: 2,
	targetCourseLoad: 4,
	preferredCourseIds: [2, 4],
}

const students = [ownerStudent, invitedStudent]

const morningTimeSlot = {
	id: 301,
	dayOfWeek: 'MONDAY',
	startTime: '09:00',
	endTime: '10:15',
	label: 'Morning Block',
}

const lateMorningTimeSlot = {
	id: 302,
	dayOfWeek: 'MONDAY',
	startTime: '10:30',
	endTime: '11:45',
	label: 'Late Morning Block',
}

const afternoonTimeSlot = {
	id: 303,
	dayOfWeek: 'WEDNESDAY',
	startTime: '13:00',
	endTime: '14:15',
	label: 'Afternoon Block',
}

const engineeringRoom = {
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
}

const scienceRoom = {
	id: 602,
	roomNumber: '120',
	capacity: 24,
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
}

const bookingRoom = {
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
	buildingId: 701,
	buildingCode: 'ENG',
	buildingName: 'Engineering',
}

const unavailableRoom = {
	id: 604,
	roomNumber: '100',
	capacity: 16,
	type: 'STUDY',
	availabilityStatus: 'MAINTENANCE',
	features: null,
	featureSet: [],
	accessibilityFlags: [],
	operationalNotes: null,
	lastInspectionDate: null,
	buildingId: 703,
	buildingCode: 'LIB',
	buildingName: 'Library',
}

const rooms = [engineeringRoom, scienceRoom, bookingRoom, unavailableRoom]

const buildings = [
	{ id: 701, code: 'ENG', name: 'Engineering' },
	{ id: 702, code: 'SCI', name: 'Science' },
	{ id: 703, code: 'LIB', name: 'Library' },
]

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
	room: engineeringRoom,
	timeSlot: morningTimeSlot,
	semester: 'Spring 2026',
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
	room: scienceRoom,
	timeSlot: afternoonTimeSlot,
	semester: 'Spring 2026',
}

const existingOtherStudentBooking = {
	id: 9001,
	room: scienceRoom,
	timeSlot: morningTimeSlot,
	semester: 'Spring 2026',
	bookingDate: primaryBookingDate,
	createdAt: '2026-03-30T11:00:00Z',
	participantCount: 1,
	viewerCanSeeStudentDetails: false,
	viewerIsOwner: false,
	viewerIsParticipant: false,
	bookedBy: null,
	participants: [],
}

async function mockStudentReferenceRoutes(page: Page) {
	await page.route(/.*\/api\/students$/, async route => {
		await route.fulfill({ json: students })
	})

	await page.route(/.*\/api\/rooms$/, async route => {
		await route.fulfill({ json: rooms })
	})

	await page.route(/.*\/api\/buildings$/, async route => {
		await route.fulfill({ json: buildings })
	})

	await page.route(/.*\/api\/timeslots$/, async route => {
		await route.fulfill({ json: [morningTimeSlot, lateMorningTimeSlot, afternoonTimeSlot] })
	})
}

test.describe('Student schedule UI', () => {
	test.beforeEach(async ({ page }) => {
		await mockStudentReferenceRoutes(page)
	})

	test('shows enrolled and waitlisted classes separately with seat pressure data', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'student')
		})

		await page.route(/.*\/api\/room-bookings(?:\?.*)?$/, async route => {
			await route.fulfill({ json: [] })
		})

		await page.route(/.*\/api\/enrollments\?studentId=\d+$/, async route => {
			await route.fulfill({
				json: [
					{ id: 1, semester: 'Spring 2026', status: 'ENROLLED', student: null, schedule: null },
					{ id: 2, semester: 'Spring 2026', status: 'WAITLISTED', student: null, schedule: null },
				],
			})
		})

		await page.route(/.*\/api\/students\/\d+\/schedule\?semester=Spring\+2026$/, async route => {
			const match = route.request().url().match(/\/api\/students\/(\d+)\/schedule/)
			const requestedStudentId = Number(match?.[1] ?? ownerStudent.id)
			await route.fulfill({
				json: {
					studentId: requestedStudentId,
					semester: 'Spring 2026',
					enrolled: [{ id: 1, semester: 'Spring 2026', status: 'ENROLLED', student: null, schedule: enrolledSchedule }],
					waitlisted: [{ id: 2, semester: 'Spring 2026', status: 'WAITLISTED', student: null, schedule: waitlistedSchedule }],
				},
			})
		})

		await page.route(/.*\/api\/schedules\?semester=Spring\+2026$/, async route => {
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

		const enrolledSection = page.locator('section').filter({ hasText: 'Enrolled Classes' })
		const waitlistedSection = page.locator('section').filter({ hasText: 'Waitlisted Classes' })

		await expect(page.getByRole('heading', { name: 'My Schedule' })).toBeVisible()
		await expect(enrolledSection.getByRole('row', { name: /CS301 Distributed Systems/ })).toBeVisible()
		await expect(waitlistedSection.getByRole('row', { name: /CS341 Waitlisted Operating Systems/ })).toBeVisible()
		await expect(page.getByRole('heading', { name: 'Enrolled Classes' })).toBeVisible()
		await expect(page.getByRole('heading', { name: 'Waitlisted Classes' })).toBeVisible()
		await expect(page.getByRole('heading', { name: 'Calendar' })).toBeVisible()
		await expect(page.getByText('18/20 seats (90%)')).toBeVisible()
		await expect(page.getByText('2 seats left, 5 waitlisted')).toBeVisible()
		await expect(page.getByText('Unrelated Capstone')).not.toBeVisible()

		await waitlistedSection.getByRole('button', { name: 'Details' }).first().click({ force: true })
		await expect(page.getByText('Status: Waitlisted')).toBeVisible()
	})

	test('lets a student create a booking with filtered rooms and participant email lookup', async ({ page }) => {
		let createPayload: Record<string, unknown> | null = null
		let studentSearchHeaders: Record<string, string> | null = null
		const roomBookingQueries: string[] = []
		const createdBookingBase = {
			id: 9002,
			room: bookingRoom,
			timeSlot: morningTimeSlot,
			semester: 'Spring 2026',
			participantCount: 2,
			viewerCanSeeStudentDetails: true,
			viewerIsOwner: true,
			viewerIsParticipant: true,
			bookedBy: {
				id: ownerStudent.id,
				fullName: `${ownerStudent.firstName} ${ownerStudent.lastName}`,
				email: ownerStudent.email,
			},
			participants: [
				{
					id: invitedStudent.id,
					fullName: `${invitedStudent.firstName} ${invitedStudent.lastName}`,
					email: invitedStudent.email,
				},
			],
		}

		let roomBookings = [existingOtherStudentBooking]

		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'student')
		})

		await page.route(/.*\/api\/room-bookings\/student-search.*/, async route => {
			const requestUrl = new URL(route.request().url())
			studentSearchHeaders = route.request().headers()
			expect(requestUrl.searchParams.get('query')).toBe('jon')
			expect(requestUrl.searchParams.get('semester')).toBe('Spring 2026')
			expect(requestUrl.searchParams.get('timeSlotId')).toBe(String(morningTimeSlot.id))
			expect(requestUrl.searchParams.getAll('excludeStudentId')).toContain(String(ownerStudent.id))
			await route.fulfill({
				json: [
					{
						id: invitedStudent.id,
						email: invitedStudent.email,
						fullName: `${invitedStudent.firstName} ${invitedStudent.lastName}`,
						hasClassDuringPeriod: true,
					},
				],
			})
		})

		await page.route(/.*\/api\/room-bookings(?:\?.*)?$/, async route => {
			if (route.request().method() === 'POST') {
				createPayload = route.request().postDataJSON() as Record<string, unknown>
				const headers = route.request().headers()
				expect(headers['x-viewer-role']).toBe('student')
				expect(headers['x-viewer-student-id']).toBe(String(ownerStudent.id))
				const createdBooking = {
					...createdBookingBase,
					bookingDate: String(createPayload.bookingDate),
					createdAt: '2026-03-30T12:00:00Z',
				}
				roomBookings = [...roomBookings, createdBooking]
				await route.fulfill({
					status: 201,
					contentType: 'application/json',
					json: createdBooking,
				})
				return
			}

			const requestUrl = new URL(route.request().url())
			roomBookingQueries.push(requestUrl.search)
			expect(requestUrl.searchParams.get('semester')).toBe('Spring 2026')
			expect(requestUrl.searchParams.has('studentId')).toBe(false)
			await route.fulfill({ json: roomBookings })
		})

		await page.route(/.*\/api\/enrollments\?studentId=\d+$/, async route => {
			await route.fulfill({
				json: [
					{ id: 1, semester: 'Spring 2026', status: 'ENROLLED', student: null, schedule: null },
					{ id: 2, semester: 'Spring 2026', status: 'WAITLISTED', student: null, schedule: null },
				],
			})
		})

		await page.route(/.*\/api\/students\/\d+\/schedule\?semester=Spring\+2026$/, async route => {
			const match = route.request().url().match(/\/api\/students\/(\d+)\/schedule/)
			const requestedStudentId = Number(match?.[1] ?? ownerStudent.id)
			await route.fulfill({
				json: {
					studentId: requestedStudentId,
					semester: 'Spring 2026',
					enrolled: [{ id: 1, semester: 'Spring 2026', status: 'ENROLLED', student: null, schedule: enrolledSchedule }],
					waitlisted: [{ id: 2, semester: 'Spring 2026', status: 'WAITLISTED', student: null, schedule: waitlistedSchedule }],
				},
			})
		})

		await page.route(/.*\/api\/schedules\?semester=Spring\+2026$/, async route => {
			await route.fulfill({
				json: [
					{ ...enrolledSchedule, filledSeats: 18, seatLimit: 20, remainingSeats: 2, waitlistCount: 5 },
					{ ...waitlistedSchedule, filledSeats: 20, seatLimit: 20, remainingSeats: 0, waitlistCount: 7 },
				],
			})
		})

		await page.goto('/schedules')
		await page.getByLabel('Student').selectOption(String(ownerStudent.id))
		await expect(page.getByRole('button', { name: 'Book Room' })).toBeEnabled()

		await page.getByRole('button', { name: 'Book Room' }).click()

		const dialog = page.getByRole('dialog', { name: 'Book Room' })
		await expect(dialog).toBeVisible()

		await expect(dialog.getByText('Students can book at most 3 weeks ahead.')).toBeVisible()
		await dialog.getByRole('button', { name: /Morning Block/ }).first().click()
		await expect(dialog.getByRole('button', { name: /ENG 310/ })).toBeVisible()
		await expect(dialog.getByRole('button', { name: /SCI 120/ })).toHaveCount(0)

		await dialog.locator('#participant-search').fill('jon')
		await expect(dialog.getByText('Jonah Lee')).toBeVisible()
		await expect(dialog.getByText('jonah.lee@students.campus.edu')).toBeVisible()
		await expect(dialog.getByText('Has classes during this period')).toBeVisible()

		await dialog.getByRole('button', { name: 'Add' }).click()
		await expect(dialog.getByText('Selected Students')).toBeVisible()
		await expect(dialog.getByText('No invited students yet.')).not.toBeVisible()

		await dialog.getByRole('button', { name: /ENG 310/ }).click()
		await dialog.getByRole('button', { name: 'Create Booking' }).click()

		const successToast = page.getByText('Room booking created')
		await expect(successToast).toBeVisible()
		await expect(dialog).toBeHidden()

		expect(createPayload).toEqual({
			studentId: ownerStudent.id,
			semester: 'Spring 2026',
			bookingDate: primaryBookingDate,
			timeSlotId: morningTimeSlot.id,
			roomId: bookingRoom.id,
			participantEmails: [invitedStudent.email],
		})
		expect(studentSearchHeaders?.['x-viewer-role']).toBe('student')
		expect(studentSearchHeaders?.['x-viewer-student-id']).toBe(String(ownerStudent.id))
		expect(roomBookingQueries.length).toBeGreaterThan(0)

		const roomBookingsSection = page.locator('section').filter({ hasText: 'My Room Bookings' })
		await expect(roomBookingsSection.getByText('ENG 310')).toBeVisible()
		await expect(roomBookingsSection.getByText('2 students')).toBeVisible()
		await expect(roomBookingsSection.getByText('Owner')).toBeVisible()

		await roomBookingsSection.getByRole('button', { name: 'Details' }).click({ force: true })
		const detailsDialog = page.getByRole('dialog', { name: 'Room Booking Details' })
		await expect(detailsDialog.getByText('Booked by:')).toBeVisible()
		await expect(detailsDialog.getByText('Maya Patel (maya.patel@students.campus.edu)')).toBeVisible()
		await expect(detailsDialog.getByText('Invited students:')).toBeVisible()
		await expect(detailsDialog.getByText('Jonah Lee')).toBeVisible()
		await expect(detailsDialog.getByText('jonah.lee@students.campus.edu')).toBeVisible()
	})
})

test.describe('Shared schedule seat metrics', () => {
	test('renders backend seat and waitlist summaries for admin schedule rows', async ({ page }) => {
		await page.addInitScript(() => {
			localStorage.setItem('campus-operations-system-role', 'admin')
		})

		await page.route(/.*\/api\/room-bookings(?:\?.*)?$/, async route => {
			await route.fulfill({ json: [] })
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
			await route.fulfill({ json: [engineeringRoom] })
		})

		await page.route(/.*\/api\/buildings$/, async route => {
			await route.fulfill({ json: [{ id: 701, code: 'ENG', name: 'Engineering' }] })
		})

		await page.route(/.*\/api\/timeslots$/, async route => {
			await route.fulfill({ json: [morningTimeSlot] })
		})

		await page.goto('/schedules')
		await page.getByRole('button', { name: 'Table' }).click()

		await expect(page.getByText('12/30 seats (40%)')).toBeVisible()
		await expect(page.getByText('18 seats left, 4 waitlisted')).toBeVisible()
		await expect(page.getByRole('cell', { name: '4', exact: true })).toBeVisible()
	})
})
