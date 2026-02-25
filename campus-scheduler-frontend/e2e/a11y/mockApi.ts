import type { Page, Route } from '@playwright/test'
import type { A11yMockGap, A11yRole, A11yTheme } from '../../scripts/a11y/types'

interface MockInstallOptions {
	route: string
	role: A11yRole
	theme: A11yTheme
}

interface MockState {
	buildings: Array<{ id: number; name: string; code: string; address: string | null }>
	rooms: Array<{
		id: number
		roomNumber: string
		capacity: number
		type: string
		features: string | null
		buildingId: number | null
		buildingCode: string | null
		buildingName: string | null
	}>
	instructors: Array<{
		id: number
		firstName: string
		lastName: string
		email: string
		department: string | null
		officeNumber: string | null
	}>
	courses: Array<{
		id: number
		code: string
		name: string
		description: string | null
		credits: number
		enrollmentCapacity: number
		department: string | null
		instructor: { id: number; firstName: string; lastName: string; email: string } | null
	}>
	timeslots: Array<{
		id: number
		dayOfWeek: string
		startTime: string
		endTime: string
		label: string | null
	}>
	schedules: Array<{
		id: number
		course: {
			id: number
			code: string
			name: string
			description: string | null
			credits: number
			enrollmentCapacity: number
			department: string | null
			instructor: { id: number; firstName: string; lastName: string; email: string } | null
		}
		room: {
			id: number
			roomNumber: string
			capacity: number
			type: string
			features: string | null
			buildingId: number | null
			buildingCode: string | null
			buildingName: string | null
		}
		timeSlot: {
			id: number
			dayOfWeek: string
			startTime: string
			endTime: string
			label: string | null
		}
		semester: string
	}>
	changeRequests: Array<{
		id: number
		schedule: MockState['schedules'][number]
		requestedByInstructor: MockState['instructors'][number]
		requestedByRole: 'INSTRUCTOR' | 'ADMIN'
		status: 'PENDING' | 'APPROVED' | 'REJECTED'
		reasonCategory: 'MEDICAL' | 'EQUIPMENT_FAILURE' | 'PEDAGOGICAL_CONFLICT' | 'OTHER'
		reasonDetails: string | null
		proposedRoom: MockState['rooms'][number] | null
		proposedTimeSlot: MockState['timeslots'][number] | null
		originalRoomId: number
		originalTimeSlotId: number
		originalSemester: string
		decisionNote: string | null
		createdAt: string
		reviewedAt: string | null
		appliedAt: string | null
	}>
}

const semesterDefinitions = [
	{
		term: 'WINTER',
		displayName: 'Winter',
		startMonth: 12,
		startDay: 21,
		endMonth: 3,
		endDay: 20,
		startYearOffset: -1,
		endYearOffset: 0,
	},
	{
		term: 'SPRING',
		displayName: 'Spring',
		startMonth: 3,
		startDay: 21,
		endMonth: 6,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
	{
		term: 'SUMMER',
		displayName: 'Summer',
		startMonth: 6,
		startDay: 21,
		endMonth: 9,
		endDay: 20,
		startYearOffset: 0,
		endYearOffset: 0,
	},
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
]

function createState(): MockState {
	const buildings = [
		{ id: 1, code: 'ENG', name: 'Engineering Building', address: '123 Tech Lane' },
		{ id: 2, code: 'SCI', name: 'Science Center', address: '456 Lab Road' },
	]

	const rooms = [
		{
			id: 1,
			buildingId: 1,
			buildingCode: 'ENG',
			buildingName: 'Engineering Building',
			roomNumber: '101',
			capacity: 30,
			type: 'CLASSROOM',
			features: 'Projector',
		},
		{
			id: 2,
			buildingId: 2,
			buildingCode: 'SCI',
			buildingName: 'Science Center',
			roomNumber: '202',
			capacity: 45,
			type: 'LAB',
			features: 'Computers',
		},
	]

	const instructors = [
		{
			id: 10,
			firstName: 'Ada',
			lastName: 'Lovelace',
			email: 'ada@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-201',
		},
		{
			id: 11,
			firstName: 'Alan',
			lastName: 'Turing',
			email: 'alan@campus.edu',
			department: 'Computer Science',
			officeNumber: 'CS-202',
		},
	]

	const courses = [
		{
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
		{
			id: 21,
			code: 'CS201',
			name: 'Data Structures',
			description: null,
			credits: 3,
			enrollmentCapacity: 40,
			department: 'Computer Science',
			instructor: {
				id: 11,
				firstName: 'Alan',
				lastName: 'Turing',
				email: 'alan@campus.edu',
			},
		},
	]

	const timeslots = [
		{
			id: 1,
			dayOfWeek: 'MONDAY',
			startTime: '09:00',
			endTime: '10:00',
			label: 'Period 1',
		},
		{
			id: 2,
			dayOfWeek: 'TUESDAY',
			startTime: '10:00',
			endTime: '11:00',
			label: 'Period 2',
		},
		{
			id: 3,
			dayOfWeek: 'WEDNESDAY',
			startTime: '11:00',
			endTime: '12:00',
			label: 'Period 3',
		},
	]

	const schedules = [
		{
			id: 50,
			semester: 'Fall 2026',
			course: courses[0],
			room: rooms[0],
			timeSlot: timeslots[0],
		},
		{
			id: 51,
			semester: 'Fall 2026',
			course: courses[1],
			room: rooms[1],
			timeSlot: timeslots[1],
		},
	]

	const changeRequests = [
		{
			id: 1,
			schedule: schedules[0],
			requestedByInstructor: instructors[0],
			requestedByRole: 'INSTRUCTOR' as const,
			status: 'PENDING' as const,
			reasonCategory: 'OTHER' as const,
			reasonDetails: 'Need a later room',
			proposedRoom: rooms[1],
			proposedTimeSlot: timeslots[1],
			originalRoomId: schedules[0].room.id,
			originalTimeSlotId: schedules[0].timeSlot.id,
			originalSemester: schedules[0].semester,
			decisionNote: null,
			createdAt: '2026-02-12T12:00:00Z',
			reviewedAt: null,
			appliedAt: null,
		},
	]

	return {
		buildings,
		rooms,
		instructors,
		courses,
		timeslots,
		schedules,
		changeRequests,
	}
}

function asJson(route: Route, payload: unknown, status = 200): Promise<void> {
	return route.fulfill({
		status,
		contentType: 'application/json',
		body: JSON.stringify(payload),
	})
}

function getNumericPathId(pathname: string): number | null {
	const match = pathname.match(/\/(\d+)(?:$|\/)/)
	if (!match?.[1]) return null
	return Number(match[1])
}

function filterSchedulesByQuery(
	schedules: MockState['schedules'],
	searchParams: URLSearchParams
): MockState['schedules'] {
	let output = schedules

	const roomId = Number(searchParams.get('roomId') || 0)
	const courseId = Number(searchParams.get('courseId') || 0)
	const instructorId = Number(searchParams.get('instructorId') || 0)
	const semester = searchParams.get('semester')

	if (roomId) output = output.filter(item => item.room.id === roomId)
	if (courseId) output = output.filter(item => item.course.id === courseId)
	if (instructorId) output = output.filter(item => item.course.instructor?.id === instructorId)
	if (semester) output = output.filter(item => item.semester === semester)

	return output
}

function filterChangeRequestsByQuery(
	requests: MockState['changeRequests'],
	searchParams: URLSearchParams
): MockState['changeRequests'] {
	let output = requests

	const status = searchParams.get('status')
	const instructorId = Number(searchParams.get('instructorId') || 0)
	const semester = searchParams.get('semester')
	const scheduleId = Number(searchParams.get('scheduleId') || 0)

	if (status) output = output.filter(item => item.status === status)
	if (instructorId) output = output.filter(item => item.requestedByInstructor.id === instructorId)
	if (semester) output = output.filter(item => item.schedule.semester === semester)
	if (scheduleId) output = output.filter(item => item.schedule.id === scheduleId)

	return output
}

function buildAnalytics(state: MockState) {
	const rooms = state.rooms.map((room, index) => ({
		roomId: room.id,
		roomNumber: room.roomNumber,
		buildingName: room.buildingName || 'Unknown',
		buildingCode: room.buildingCode || 'UNK',
		capacity: room.capacity,
		scheduledSlots: index + 3,
		totalSlots: 20,
		utilizationPercentage: Math.round(((index + 3) / 20) * 1000) / 10,
	}))

	const buildings = state.buildings.map((building, index) => ({
		buildingId: building.id,
		buildingName: building.name,
		buildingCode: building.code,
		roomCount: state.rooms.filter(room => room.buildingId === building.id).length,
		scheduledSlots: (index + 3) * 4,
		totalSlots: 40,
		utilizationPercentage: Math.round((((index + 3) * 4) / 40) * 1000) / 10,
	}))

	const peakHours = state.timeslots.map((slot, index) => ({
		timeSlotId: slot.id,
		dayOfWeek: slot.dayOfWeek,
		startTime: slot.startTime,
		endTime: slot.endTime,
		label: slot.label || `Slot ${slot.id}`,
		bookingCount: index + 2,
	}))

	const summary = {
		semester: 'Fall 2026',
		totalRooms: state.rooms.length,
		totalBuildings: state.buildings.length,
		totalScheduledSlots: 15,
		totalAvailableSlots: 40,
		overallUtilizationPercentage: 37.5,
		topUtilizedRooms: rooms.slice(0, 2),
		leastUtilizedRooms: [...rooms].reverse().slice(0, 2),
	}

	return {
		rooms,
		buildings,
		peakHours,
		summary,
	}
}

function recordGap(
	mockGaps: A11yMockGap[],
	options: MockInstallOptions,
	method: string,
	url: string,
	reason: string
): void {
	mockGaps.push({
		method,
		url,
		reason,
		route: options.route,
		role: options.role,
		theme: options.theme,
	})
}

export async function installA11yMockApi(
	page: Page,
	options: MockInstallOptions
): Promise<{ mockGaps: A11yMockGap[] }> {
	const state = createState()
	const mockGaps: A11yMockGap[] = []

	await page.route('**/ws/info**', async route => {
		await asJson(route, {
			websocket: false,
			cookie_needed: false,
			origins: ['*:*'],
			entropy: 1,
		})
	})

	await page.route('**/api/**', async route => {
		const request = route.request()
		const url = new URL(request.url())
		const method = request.method().toUpperCase()
		const pathname = url.pathname.replace(/^\/api/, '') || '/'
		const analytics = buildAnalytics(state)

		if (pathname === '/buildings' && method === 'GET') {
			await asJson(route, state.buildings)
			return
		}

		if (pathname === '/buildings' && method === 'POST') {
			const payload = request.postDataJSON() as Partial<MockState['buildings'][number]> | null
			const next = {
				id: Math.max(...state.buildings.map(item => item.id)) + 1,
				code: payload?.code || 'NEW',
				name: payload?.name || 'New Building',
				address: payload?.address || null,
			}
			state.buildings.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/buildings\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const building = state.buildings.find(item => item.id === id)
			if (!building) {
				await asJson(route, { message: 'Building not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, building)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(building, payload)
				await asJson(route, building)
				return
			}

			if (method === 'DELETE') {
				state.buildings = state.buildings.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/rooms' && method === 'GET') {
			const buildingId = Number(url.searchParams.get('buildingId') || 0)
			const rooms = buildingId ? state.rooms.filter(room => room.buildingId === buildingId) : state.rooms
			await asJson(route, rooms)
			return
		}

		if (/^\/rooms\/building\/\d+$/.test(pathname) && method === 'POST') {
			const buildingId = getNumericPathId(pathname)
			const building = state.buildings.find(item => item.id === buildingId)
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: Math.max(...state.rooms.map(item => item.id)) + 1,
				roomNumber: String(payload.roomNumber || '999'),
				capacity: Number(payload.capacity || 10),
				type: String(payload.type || 'CLASSROOM'),
				features: (payload.features as string | undefined) ?? null,
				buildingId: building?.id ?? null,
				buildingCode: building?.code ?? null,
				buildingName: building?.name ?? null,
			}
			state.rooms.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/rooms\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const room = state.rooms.find(item => item.id === id)
			if (!room) {
				await asJson(route, { message: 'Room not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, room)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(room, payload)
				await asJson(route, room)
				return
			}

			if (method === 'DELETE') {
				state.rooms = state.rooms.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/instructors' && method === 'GET') {
			const department = url.searchParams.get('department')
			const instructors = department
				? state.instructors.filter(item => item.department === department)
				: state.instructors
			await asJson(route, instructors)
			return
		}

		if (pathname === '/instructors' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: Math.max(...state.instructors.map(item => item.id)) + 1,
				firstName: String(payload.firstName || 'New'),
				lastName: String(payload.lastName || 'Instructor'),
				email: String(payload.email || 'new@campus.edu'),
				department: (payload.department as string | undefined) ?? null,
				officeNumber: (payload.officeNumber as string | undefined) ?? null,
			}
			state.instructors.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/instructors\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const instructor = state.instructors.find(item => item.id === id) ?? state.instructors[0]
			if (!instructor) {
				await asJson(route, { message: 'No instructors available' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, instructor)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(instructor, payload)
				await asJson(route, instructor)
				return
			}

			if (method === 'DELETE') {
				state.instructors = state.instructors.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/courses' && method === 'GET') {
			const department = url.searchParams.get('department')
			const instructorId = Number(url.searchParams.get('instructorId') || 0)

			const courses = state.courses.filter((course) => {
				if (department && course.department !== department) return false
				if (instructorId && course.instructor?.id !== instructorId) return false
				return true
			})

			await asJson(route, courses)
			return
		}

		if (pathname === '/courses' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: Math.max(...state.courses.map(item => item.id)) + 1,
				code: String(payload.code || 'NEW101'),
				name: String(payload.name || 'New Course'),
				description: (payload.description as string | undefined) ?? null,
				credits: Number(payload.credits || 3),
				enrollmentCapacity: Number(payload.enrollmentCapacity || 20),
				department: (payload.department as string | undefined) ?? null,
				instructor: null,
			}
			state.courses.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/courses\/code\/.+/.test(pathname) && method === 'GET') {
			const code = decodeURIComponent(pathname.split('/').pop() || '')
			const course = state.courses.find(item => item.code === code)
			if (!course) {
				await asJson(route, { message: 'Course not found' }, 404)
				return
			}
			await asJson(route, course)
			return
		}

		if (/^\/courses\/instructor\/\d+$/.test(pathname) && method === 'GET') {
			const instructorId = getNumericPathId(pathname)
			const courses = state.courses.filter(item => item.instructor?.id === instructorId)
			await asJson(route, courses)
			return
		}

		if (/^\/courses\/\d+\/instructor\/\d+$/.test(pathname) && method === 'PUT') {
			const ids = pathname.match(/^\/courses\/(\d+)\/instructor\/(\d+)$/)
			const courseId = Number(ids?.[1] || 0)
			const instructorId = Number(ids?.[2] || 0)
			const course = state.courses.find(item => item.id === courseId)
			const instructor = state.instructors.find(item => item.id === instructorId)
			if (!course || !instructor) {
				await asJson(route, { message: 'Not found' }, 404)
				return
			}
			course.instructor = {
				id: instructor.id,
				firstName: instructor.firstName,
				lastName: instructor.lastName,
				email: instructor.email,
			}
			await asJson(route, course)
			return
		}

		if (/^\/courses\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const course = state.courses.find(item => item.id === id) ?? state.courses[0]
			if (!course) {
				await asJson(route, { message: 'No courses available' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, course)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(course, payload)
				await asJson(route, course)
				return
			}

			if (method === 'DELETE') {
				state.courses = state.courses.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/timeslots' && method === 'GET') {
			const dayOfWeek = url.searchParams.get('dayOfWeek')
			const timeSlots = dayOfWeek ? state.timeslots.filter(item => item.dayOfWeek === dayOfWeek) : state.timeslots
			await asJson(route, timeSlots)
			return
		}

		if (pathname === '/timeslots' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const next = {
				id: Math.max(...state.timeslots.map(item => item.id)) + 1,
				dayOfWeek: String(payload.dayOfWeek || 'MONDAY'),
				startTime: String(payload.startTime || '08:00'),
				endTime: String(payload.endTime || '09:00'),
				label: (payload.label as string | undefined) ?? null,
			}
			state.timeslots.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/timeslots\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const slot = state.timeslots.find(item => item.id === id)
			if (!slot) {
				await asJson(route, { message: 'Time slot not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, slot)
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				Object.assign(slot, payload)
				await asJson(route, slot)
				return
			}

			if (method === 'DELETE') {
				state.timeslots = state.timeslots.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/schedules' && method === 'GET') {
			await asJson(route, filterSchedulesByQuery(state.schedules, url.searchParams))
			return
		}

		if (pathname === '/schedules' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const course = state.courses.find(item => item.id === Number(payload.courseId || 0)) ?? state.courses[0]
			const room = state.rooms.find(item => item.id === Number(payload.roomId || 0)) ?? state.rooms[0]
			const timeSlot = state.timeslots.find(item => item.id === Number(payload.timeSlotId || 0)) ?? state.timeslots[0]
			const next = {
				id: Math.max(...state.schedules.map(item => item.id)) + 1,
				course,
				room,
				timeSlot,
				semester: String(payload.semester || 'Fall 2026'),
			}
			state.schedules.push(next)
			await asJson(route, next, 201)
			return
		}

		if (pathname.startsWith('/schedules/conflicts') && method === 'GET') {
			await asJson(route, { hasConflict: false })
			return
		}

		if (/^\/schedules\/\d+$/.test(pathname)) {
			const id = getNumericPathId(pathname)
			const schedule = state.schedules.find(item => item.id === id)
			if (!schedule) {
				await asJson(route, { message: 'Schedule not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, schedule)
				return
			}

			if (method === 'DELETE') {
				state.schedules = state.schedules.filter(item => item.id !== id)
				await route.fulfill({ status: 204 })
				return
			}
		}

		if (pathname === '/change-requests' && method === 'GET') {
			await asJson(route, filterChangeRequestsByQuery(state.changeRequests, url.searchParams))
			return
		}

		if (pathname === '/change-requests' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const schedule = state.schedules.find(item => item.id === Number(payload.scheduleId || 0)) ?? state.schedules[0]
			const instructor =
				state.instructors.find(item => item.id === Number(payload.requestedByInstructorId || 0)) ?? state.instructors[0]
			const proposedRoom = state.rooms.find(item => item.id === Number(payload.proposedRoomId || 0)) ?? null
			const proposedTimeSlot = state.timeslots.find(item => item.id === Number(payload.proposedTimeSlotId || 0)) ?? null

			const next = {
				id: Math.max(...state.changeRequests.map(item => item.id)) + 1,
				schedule,
				requestedByInstructor: instructor,
				requestedByRole: String(payload.requestedByRole || 'INSTRUCTOR') as 'INSTRUCTOR' | 'ADMIN',
				status: 'PENDING' as const,
				reasonCategory: String(payload.reasonCategory || 'OTHER') as
					| 'MEDICAL'
					| 'EQUIPMENT_FAILURE'
					| 'PEDAGOGICAL_CONFLICT'
					| 'OTHER',
				reasonDetails: (payload.reasonDetails as string | undefined) ?? null,
				proposedRoom,
				proposedTimeSlot,
				originalRoomId: schedule.room.id,
				originalTimeSlotId: schedule.timeSlot.id,
				originalSemester: schedule.semester,
				decisionNote: null,
				createdAt: new Date().toISOString(),
				reviewedAt: null,
				appliedAt: null,
			}

			state.changeRequests.push(next)
			await asJson(route, next, 201)
			return
		}

		if (/^\/change-requests\/\d+\/(approve|reject)$/.test(pathname) && method === 'POST') {
			const ids = pathname.match(/^\/change-requests\/(\d+)\/(approve|reject)$/)
			const requestId = Number(ids?.[1] || 0)
			const action = ids?.[2] || 'approve'
			const requestItem = state.changeRequests.find(item => item.id === requestId)
			if (!requestItem) {
				await asJson(route, { message: 'Change request not found' }, 404)
				return
			}

			requestItem.status = action === 'approve' ? 'APPROVED' : 'REJECTED'
			requestItem.reviewedAt = new Date().toISOString()
			const payload = request.postDataJSON() as Record<string, unknown>
			requestItem.decisionNote = (payload.decisionNote as string | undefined) ?? null

			await asJson(route, requestItem)
			return
		}

		if (pathname === '/change-requests/validate' && method === 'POST') {
			await asJson(route, {
				green: true,
				hardConflicts: [],
				softWarnings: [],
			})
			return
		}

		if (pathname === '/instructor-preferences/room-feature-options' && method === 'GET') {
			await asJson(route, [
				{ value: 'projector', label: 'Projector', category: 'Presentation and AV', matchKeywords: ['projector'] },
				{ value: 'microphone', label: 'Microphone', category: 'Presentation and AV', matchKeywords: ['microphone', 'mic'] },
				{ value: 'whiteboard', label: 'Whiteboard', category: 'Teaching setup', matchKeywords: ['whiteboard'] },
			])
			return
		}

		if (/^\/instructor-preferences\/\d+$/.test(pathname)) {
			const instructorId = getNumericPathId(pathname)
			if (!instructorId) {
				await asJson(route, { message: 'Instructor not found' }, 404)
				return
			}

			if (method === 'GET') {
				await asJson(route, {
					instructorId,
					preferredStartTime: '08:00',
					preferredEndTime: '18:00',
					maxGapMinutes: 120,
					minTravelBufferMinutes: 15,
					avoidBuildingHops: true,
					preferredBuildingIds: [],
					requiredRoomFeatures: [],
					updatedAt: new Date().toISOString(),
				})
				return
			}

			if (method === 'PUT') {
				const payload = request.postDataJSON() as Record<string, unknown>
				await asJson(route, {
					instructorId,
					preferredStartTime: payload.preferredStartTime ?? '08:00',
					preferredEndTime: payload.preferredEndTime ?? '18:00',
					maxGapMinutes: payload.maxGapMinutes ?? 120,
					minTravelBufferMinutes: payload.minTravelBufferMinutes ?? 15,
					avoidBuildingHops: payload.avoidBuildingHops ?? true,
					preferredBuildingIds: payload.preferredBuildingIds ?? [],
					requiredRoomFeatures: payload.requiredRoomFeatures ?? [],
					updatedAt: new Date().toISOString(),
				})
				return
			}
		}

		if (pathname === '/instructor-insights/frictions' && method === 'GET') {
			await asJson(route, [])
			return
		}

		if (pathname === '/analytics/rooms' && method === 'GET') {
			await asJson(route, analytics.rooms)
			return
		}

		if (/^\/analytics\/rooms\/\d+$/.test(pathname) && method === 'GET') {
			const id = getNumericPathId(pathname)
			const room = analytics.rooms.find(item => item.roomId === id)
			await asJson(route, room ?? analytics.rooms[0])
			return
		}

		if (pathname === '/analytics/buildings' && method === 'GET') {
			await asJson(route, analytics.buildings)
			return
		}

		if (/^\/analytics\/buildings\/\d+$/.test(pathname) && method === 'GET') {
			const id = getNumericPathId(pathname)
			const building = analytics.buildings.find(item => item.buildingId === id)
			await asJson(route, building ?? analytics.buildings[0])
			return
		}

		if (pathname === '/analytics/peak-hours' && method === 'GET') {
			await asJson(route, analytics.peakHours)
			return
		}

		if (pathname === '/analytics/underused' && method === 'GET') {
			const threshold = Number(url.searchParams.get('threshold') || 30)
			await asJson(route, analytics.rooms.filter(item => item.utilizationPercentage < threshold))
			return
		}

		if (pathname === '/analytics/summary' && method === 'GET') {
			await asJson(route, analytics.summary)
			return
		}

		if (pathname === '/semesters' && method === 'GET') {
			await asJson(route, semesterDefinitions)
			return
		}

		if (pathname === '/generator/stats' && method === 'GET') {
			await asJson(route, {
				buildings: state.buildings.length,
				rooms: state.rooms.length,
				instructors: state.instructors.length,
				courses: state.courses.length,
				schedules: state.schedules.length,
			})
			return
		}

		if (pathname === '/generator/archetypes' && method === 'GET') {
			await asJson(route, [
				{
					id: 'METROPOLIS',
					displayName: 'Metropolis',
					description: 'Dense urban campus',
					studentsPerBuilding: 1000,
					coursesPerBuilding: 60,
					studentsPerCourse: 50,
					minStudents: 10000,
					maxStudents: 60000,
					academicBuildingRatio: 0.7,
					exampleUniversities: ['Metro State'],
				},
				{
					id: 'CAMPUS_SPRAWL',
					displayName: 'Campus Sprawl',
					description: 'Large suburban campus',
					studentsPerBuilding: 700,
					coursesPerBuilding: 45,
					studentsPerCourse: 40,
					minStudents: 5000,
					maxStudents: 30000,
					academicBuildingRatio: 0.6,
					exampleUniversities: ['Suburban Tech'],
				},
				{
					id: 'COMMUNITY',
					displayName: 'Community',
					description: 'Compact campus',
					studentsPerBuilding: 450,
					coursesPerBuilding: 35,
					studentsPerCourse: 35,
					minStudents: 1000,
					maxStudents: 12000,
					academicBuildingRatio: 0.5,
					exampleUniversities: ['Community College'],
				},
			])
			return
		}

		if (pathname === '/generator/preview' && method === 'POST') {
			const payload = request.postDataJSON() as Record<string, unknown>
			const studentPopulation = Number(payload.studentPopulation || 8000)
			await asJson(route, {
				archetype: String(payload.archetype || 'COMMUNITY'),
				archetypeDisplayName: 'Community',
				studentPopulation,
				totalBuildings: 12,
				academicBuildings: 7,
				roomsPerBuilding: 20,
				instructors: 120,
				courses: 220,
				totalRooms: 240,
				ratioInfo: 'Mock preview data',
			})
			return
		}

		if (pathname.startsWith('/generator/university') && method === 'POST') {
			await asJson(route, {
				buildings: state.buildings.length,
				rooms: state.rooms.length,
				instructors: state.instructors.length,
				courses: state.courses.length,
				timeSlots: state.timeslots.length,
				archetype: 'COMMUNITY',
				studentPopulation: 8000,
			})
			return
		}

		if (pathname === '/generator/reset' && method === 'DELETE') {
			await route.fulfill({ status: 204 })
			return
		}

		if (pathname.startsWith('/solver/start') && method === 'POST') {
			await asJson(route, {
				problemId: 1,
				message: 'Solver started',
			})
			return
		}

		if (pathname === '/solver/stop' && method === 'POST') {
			await asJson(route, 'Solver stopped')
			return
		}

		if (pathname === '/solver/status' && method === 'GET') {
			await asJson(route, {
				status: 'NOT_SOLVING',
				score: '0hard/-50soft',
				assignedCourses: 1,
				totalCourses: 2,
				hardViolations: 0,
				softScore: -50,
			})
			return
		}

		if (pathname === '/solver/save' && method === 'POST') {
			await asJson(route, {
				savedCount: state.schedules.length,
				message: 'Saved',
			})
			return
		}

		if (pathname === '/solver/analytics' && method === 'GET') {
			await asJson(route, {
				semester: 'Fall 2026',
				totalRooms: state.rooms.length,
				totalBuildings: state.buildings.length,
				totalScheduledSlots: 15,
				totalAvailableSlots: 40,
				overallUtilizationPercentage: 37.5,
				topUtilizedRooms: analytics.rooms.slice(0, 2),
				leastUtilizedRooms: [...analytics.rooms].reverse().slice(0, 2),
				rooms: analytics.rooms,
				buildings: analytics.buildings,
				peakHours: analytics.peakHours,
			})
			return
		}

		if (pathname === '/solver/impact' && method === 'POST') {
			await asJson(route, {
				status: 'SOLVED',
				score: '0hard/-20soft',
				scoreSummary: 'Mock impact analysis',
				moves: [],
				constraintSummaries: [],
			})
			return
		}

		recordGap(mockGaps, options, method, url.toString(), 'No mock handler matched this request')
		const fallback = method === 'GET' ? [] : { ok: true }
		await asJson(route, fallback)
	})

	page.on('pageerror', error => {
		recordGap(mockGaps, options, 'PAGE', page.url(), `Page error: ${error.message}`)
	})

	return { mockGaps }
}
