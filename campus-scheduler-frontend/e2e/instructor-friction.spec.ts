import { expect, test } from '@playwright/test'

type MockRequest = {
	id: number
	schedule: any
	requestedByInstructor: any
	requestedByRole: 'INSTRUCTOR' | 'ADMIN'
	status: 'PENDING' | 'APPROVED' | 'REJECTED'
	reasonCategory: string
	reasonDetails: string | null
	proposedRoom: any | null
	proposedTimeSlot: any | null
	originalRoomId: number
	originalTimeSlotId: number
	originalSemester: string
	decisionNote: string | null
	createdAt: string
	reviewedAt: string | null
	appliedAt: string | null
}

const instructors = [
	{
		id: 10,
		firstName: 'Ada',
		lastName: 'Lovelace',
		email: 'ada@campus.edu',
		department: 'Computer Science',
		officeNumber: 'CS-201',
	},
]

const buildings = [
	{ id: 1, code: 'SCI', name: 'Science', address: '100 Campus Rd' },
	{ id: 2, code: 'ENG', name: 'Engineering', address: '200 Campus Rd' },
]

const roomFeatureOptions = [
	{ value: 'projector', label: 'Projector', category: 'Presentation and AV' },
	{ value: 'microphone', label: 'Microphone', category: 'Presentation and AV' },
]

const rooms = [
	{
		id: 1,
		buildingId: 1,
		buildingCode: 'SCI',
		buildingName: 'Science',
		roomNumber: '101',
		capacity: 40,
		type: 'CLASSROOM',
		features: 'Projector, Microphone',
	},
	{
		id: 2,
		buildingId: 2,
		buildingCode: 'ENG',
		buildingName: 'Engineering',
		roomNumber: '202',
		capacity: 40,
		type: 'CLASSROOM',
		features: 'Whiteboard',
	},
]

const timeSlots = [
	{ id: 1, dayOfWeek: 'MONDAY', startTime: '09:00', endTime: '10:00', label: 'Period 1' },
	{ id: 2, dayOfWeek: 'MONDAY', startTime: '11:00', endTime: '12:00', label: 'Period 2' },
	{ id: 3, dayOfWeek: 'MONDAY', startTime: '13:00', endTime: '14:00', label: 'Period 3' },
]

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
}

const companionSchedule = {
	id: 51,
	semester: 'Fall 2026',
	course: {
		id: 21,
		code: 'CS201',
		name: 'Data Structures',
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
	timeSlot: timeSlots[2],
}

async function setupBaseMocks(page: any) {
	await page.addInitScript(() => {
		localStorage.setItem('campus-operations-system-role', 'instructor')
		localStorage.setItem('campus-operations-system-instructor-id', '10')
	})

	await page.route(/.*\/api\/instructors$/, async (route: any) => {
		await route.fulfill({ json: instructors })
	})

	await page.route(/.*\/api\/buildings$/, async (route: any) => {
		await route.fulfill({ json: buildings })
	})

	await page.route(/.*\/api\/instructor-preferences\/room-feature-options$/, async (route: any) => {
		await route.fulfill({ json: roomFeatureOptions })
	})

	await page.route(/.*\/api\/rooms$/, async (route: any) => {
		await route.fulfill({ json: rooms })
	})

	await page.route(/.*\/api\/timeslots$/, async (route: any) => {
		await route.fulfill({ json: timeSlots })
	})
}

test.describe('Instructor friction workflows', () => {
	test('instructor preferences save and reload on dashboard', async ({ page }) => {
		let preferences = {
			instructorId: 10,
			preferredStartTime: '08:00',
			preferredEndTime: '18:00',
			maxGapMinutes: 120,
			minTravelBufferMinutes: 15,
			avoidBuildingHops: true,
			preferredBuildingIds: [] as number[],
			requiredRoomFeatures: [] as string[],
			updatedAt: '2026-02-25T12:00:00Z',
		}

		await setupBaseMocks(page)

		await page.route(/.*\/api\/schedules(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [schedule] })
		})

		await page.route(/.*\/api\/change-requests(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [] })
		})

		await page.route(/.*\/api\/instructor-insights\/frictions(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [] })
		})

		await page.route(/.*\/api\/instructor-preferences\/10$/, async (route: any) => {
			if (route.request().method() === 'PUT') {
				const payload = route.request().postDataJSON()
				preferences = {
					...preferences,
					...payload,
					updatedAt: '2026-02-25T12:05:00Z',
				}
				await route.fulfill({ json: preferences })
				return
			}
			await route.fulfill({ json: preferences })
		})

		await page.goto('/')
		await page.getByRole('button', { name: 'Class Preferences' }).click()

		await page.locator('#pref-max-gap').fill('75')
		await page.locator('#pref-feature-projector').check()
		await page.locator('#pref-feature-microphone').check()
		await page.getByRole('button', { name: 'Save' }).click()

		await page.getByRole('button', { name: 'Class Preferences' }).click()
		await expect(page.locator('#pref-max-gap')).toHaveValue('75')
		await expect(page.locator('#pref-feature-projector')).toBeChecked()
		await expect(page.locator('#pref-feature-microphone')).toBeChecked()
	})

	test('friction fix CTA deep-links to prefilled request form', async ({ page }) => {
		await setupBaseMocks(page)

		await page.route(/.*\/api\/schedules(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [schedule] })
		})

		await page.route(/.*\/api\/change-requests(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [] })
		})

		await page.route(/.*\/api\/instructor-preferences\/10$/, async (route: any) => {
			await route.fulfill({
				json: {
					instructorId: 10,
					preferredStartTime: '08:00',
					preferredEndTime: '18:00',
					maxGapMinutes: 120,
					minTravelBufferMinutes: 15,
					avoidBuildingHops: true,
					preferredBuildingIds: [],
					requiredRoomFeatures: [],
					updatedAt: '2026-02-25T12:00:00Z',
				},
			})
		})

		await page.route(/.*\/api\/instructor-insights\/frictions(\?.*)?$/, async (route: any) => {
			await route.fulfill({
				json: [
					{
						id: 'friction-1',
						type: 'OUTSIDE_PREFERRED_WINDOW',
						severity: 'MEDIUM',
						scheduleId: 50,
						message: 'Class falls outside your preferred time window',
						recommendedIssue: 'TIME_OF_DAY_PREFERENCE',
					},
				],
			})
		})

		await page.route(/.*\/api\/change-requests\/validate$/, async (route: any) => {
			await route.fulfill({ json: { green: true, hardConflicts: [], softWarnings: [] } })
		})

		await page.goto('/')
		await page.getByRole('link', { name: 'Fix' }).click()

		await expect(page).toHaveURL(/\/requests\/new\?scheduleId=50&issue=TIME_OF_DAY_PREFERENCE/)
		await expect(page.locator('#request-schedule')).toHaveValue('50')
		await expect(page.locator('#request-issue')).toHaveValue('TIME_OF_DAY_PREFERENCE')
	})

	test('suggestion ranking uses instructor preferences', async ({ page }) => {
		await setupBaseMocks(page)

		await page.route(/.*\/api\/schedules(\?.*)?$/, async (route: any) => {
			await route.fulfill({ json: [schedule, companionSchedule] })
		})

		await page.route(/.*\/api\/change-requests\/validate$/, async (route: any) => {
			await route.fulfill({ json: { green: true, hardConflicts: [], softWarnings: [] } })
		})

		await page.route(/.*\/api\/instructor-preferences\/10$/, async (route: any) => {
			await route.fulfill({
				json: {
					instructorId: 10,
					preferredStartTime: '08:00',
					preferredEndTime: '18:00',
					maxGapMinutes: 90,
					minTravelBufferMinutes: 15,
					avoidBuildingHops: true,
					preferredBuildingIds: [1],
					requiredRoomFeatures: ['projector'],
					updatedAt: '2026-02-25T12:00:00Z',
				},
			})
		})

		await page.route(/.*\/api\/solver\/impact$/, async (route: any) => {
			const payload = route.request().postDataJSON()
			const toRoom = payload.proposedRoomId
			const toSlot = payload.proposedTimeSlotId
			await route.fulfill({
				json: {
					status: 'SOLVED',
					score: '0hard/-1soft',
					scoreSummary: 'ok',
					moves: [
						{
							scheduleId: 50,
							courseCode: 'CS101',
							fromRoomId: 1,
							fromRoomLabel: 'SCI 101',
							toRoomId: toRoom,
							toRoomLabel: toRoom === 1 ? 'SCI 101' : 'ENG 202',
							fromTimeSlotId: 1,
							fromTimeSlotLabel: 'Mon 09:00-10:00',
							toTimeSlotId: toSlot,
							toTimeSlotLabel: toSlot === 2 ? 'Mon 11:00-12:00' : 'Mon 13:00-14:00',
						},
					],
					constraintSummaries: [],
				},
			})
		})

		await page.goto('/requests/new')
		const main = page.locator('main')
		await main.locator('#request-schedule').selectOption('50')
		await main.locator('#request-issue').selectOption('TIME_OF_DAY_PREFERENCE')
		await main.getByRole('button', { name: 'Generate options' }).click()

		await expect(main.locator('#request-proposed-room')).toHaveValue('1')
		await expect(main.getByText('Best match for your preferences')).toBeVisible()
	})

	test('my requests shows timeline and status refresh updates', async ({ page }) => {
		await page.addInitScript(() => {
			const original = window.setInterval
			window.setInterval = ((handler: TimerHandler, timeout?: number, ...args: any[]) => {
				const adjusted = typeof timeout === 'number' ? Math.min(timeout, 100) : 100
				return original(handler, adjusted, ...args)
			}) as typeof window.setInterval
			localStorage.setItem('campus-operations-system-role', 'instructor')
			localStorage.setItem('campus-operations-system-instructor-id', '10')
		})

		await setupBaseMocks(page)

		let requestPollCount = 0
		await page.route(/.*\/api\/change-requests(\?.*)?$/, async (route: any) => {
			requestPollCount += 1
			const isApproved = requestPollCount > 1
			const payload: MockRequest = {
				id: 1,
				schedule,
				requestedByInstructor: instructors[0],
				requestedByRole: 'INSTRUCTOR',
				status: isApproved ? 'APPROVED' : 'PENDING',
				reasonCategory: 'PEDAGOGICAL_CONFLICT',
				reasonDetails: 'Need a better transition between classes',
				proposedRoom: rooms[1],
				proposedTimeSlot: timeSlots[1],
				originalRoomId: 1,
				originalTimeSlotId: 1,
				originalSemester: 'Fall 2026',
				decisionNote: isApproved ? 'Approved after review' : null,
				createdAt: '2026-02-20T12:00:00Z',
				reviewedAt: isApproved ? '2026-02-21T12:00:00Z' : null,
				appliedAt: isApproved ? '2026-02-21T12:05:00Z' : null,
			}
			await route.fulfill({ json: [payload] })
		})

		await page.goto('/requests')
		const main = page.locator('main')
		await expect(main.getByText('Requested Change')).toBeVisible()
		await expect(main.getByText('Timeline')).toBeVisible()

		await expect(page.getByText('request is now approved')).toBeVisible({ timeout: 5000 })
		await expect(main.getByText('APPROVED', { exact: true })).toBeVisible()
		await expect(main.getByText('Decision Note:')).toBeVisible()
	})
})
