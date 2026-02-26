import { test, expect, type Page } from '@playwright/test';

test.describe('Solver Page', () => {
	const mockAnalytics = {
		semester: 'Fall 2026',
		totalRooms: 20,
		totalBuildings: 5,
		totalScheduledSlots: 0,
		totalAvailableSlots: 100,
		overallUtilizationPercentage: 0,
		topUtilizedRooms: [],
		leastUtilizedRooms: [],
		rooms: [],
		buildings: [],
		peakHours: [],
	};

	test.beforeEach(async ({ page }) => {
		await page.route(/.*\/api\/semesters.*/, async route => {
			await route.fulfill({
				json: [
					{
						term: 'FALL',
						displayName: 'Fall',
						startMonth: 9,
						startDay: 1,
						endMonth: 12,
						endDay: 31,
						startYearOffset: 0,
						endYearOffset: 0,
					},
				],
			});
		});

		await page.route(/.*\/api\/generator\/stats.*/, async route => {
			await route.fulfill({
				json: {
					buildings: 5,
					rooms: 20,
					instructors: 10,
					courses: 50,
					schedules: 0,
				},
			});
		});

		await page.route(/.*\/api\/generator\/archetypes.*/, async route => {
			await route.fulfill({
				json: [
					{
						id: 'COMMUNITY',
						displayName: 'Community Hub',
						description: 'Balanced campus profile',
						studentsPerBuilding: 200,
						coursesPerBuilding: 55,
						studentsPerCourse: 3.6,
						minStudents: 1000,
						maxStudents: 20000,
						academicBuildingRatio: 0.6,
						exampleUniversities: ['Example U'],
					},
				],
			});
		});

		await page.route(/.*\/api\/generator\/preview.*/, async route => {
			await route.fulfill({
				json: {
					archetype: 'COMMUNITY',
					archetypeDisplayName: 'Community Hub',
					studentPopulation: 8000,
					totalBuildings: 5,
					academicBuildings: 3,
					roomsPerBuilding: 4,
					instructors: 10,
					courses: 50,
					totalRooms: 20,
					ratioInfo: 'Mock preview',
				},
			});
		});

		await page.route(/.*\/api\/generator\/university.*/, async route => {
			await route.fulfill({
				json: {
					buildings: 5,
					rooms: 20,
					instructors: 10,
					courses: 50,
				},
			});
		});

		await page.route(/.*\/api\/solver\/start.*/, async route => {
			await route.fulfill({ json: { problemId: 1, message: 'Solver started successfully' } });
		});

		await page.route(/.*\/api\/solver\/stop.*/, async route => {
			await route.fulfill({ json: { message: 'Solver stopped' } });
		});

		await page.route(/.*\/api\/solver\/save.*/, async route => {
			await route.fulfill({ json: { savedCount: 50, message: 'Saved 50 schedules' } });
		});

		await page.route(/.*\/api\/solver\/analytics.*/, async route => {
			await route.fulfill({ json: mockAnalytics });
		});

		// Mock SockJS info request to allow WebSocket connection
		await page.route('**/ws/info**', async route => {
			await route.fulfill({
				json: {
					websocket: true,
					origins: ["*:*"],
					cookie_needed: false,
					entropy: 1234567890,
				},
			});
		});

		// Inject Mock WebSocket
		await page.addInitScript(() => {
			class EventTarget {
				constructor() {
					this.listeners = {};
				}
				addEventListener(type, callback) {
					if (!this.listeners[type]) this.listeners[type] = [];
					this.listeners[type].push(callback);
				}
				removeEventListener(type, callback) {
					if (!this.listeners[type]) return;
					const index = this.listeners[type].indexOf(callback);
					if (index !== -1) this.listeners[type].splice(index, 1);
				}
				dispatchEvent(event) {
					if (this.listeners[event.type]) {
						this.listeners[event.type].forEach(cb => cb(event));
					}
				}
			}

			class MockWebSocket extends EventTarget {
				constructor(url) {
					super();
					this.url = url;
					this.readyState = 0; // CONNECTING

					// Expose to window for test control
					(window as any).mockWebSocket = this;

					setTimeout(() => {
						this.readyState = 1; // OPEN
						this.dispatchEvent({ type: 'open' } as any);
						if ((this as any).onopen) (this as any).onopen({ type: 'open' });

						// SockJS protocol: Send 'o' frame
						this.receive('o');
					}, 50);
				}

				send(data) {
					const msg = data.toString();

					// SockJS sends JSON array
					let payload = "";
					try {
						const parsed = JSON.parse(msg);
						if (Array.isArray(parsed) && parsed.length > 0) {
							payload = parsed[0];
						}
					} catch (e) {
						payload = msg; // Fallback
					}

					if (payload.startsWith('CONNECT')) {
						setTimeout(() => {
							// Send CONNECTED via SockJS frame 'a["..."]'
							// STOMP CONNECTED frame
							const stompConnected = 'CONNECTED\nversion:1.2\n\n\0';
							const sockjsFrame = 'a' + JSON.stringify([stompConnected]);
							this.receive(sockjsFrame);
						}, 50);
					}
				}

				close() {
					this.readyState = 3; // CLOSED
					this.dispatchEvent({ type: 'close' } as any);
					if ((this as any).onclose) (this as any).onclose({ type: 'close' });
				}

				receive(data) {
					const event = { type: 'message', data };
					this.dispatchEvent(event as any);
					if ((this as any).onmessage) (this as any).onmessage(event);
				}
			}

			(window as any).WebSocket = MockWebSocket;
		});

		await page.goto('/solver');
	});

	async function sendProgress(
		page: Page,
		progress: {
			status: 'SOLVING_ACTIVE' | 'NOT_SOLVING'
			score: string
			assignedCourses: number
			totalCourses: number
			hardViolations: number
			softScore: number
			message: string
		},
		messageId: number
	) {
		await page.evaluate(
			({ incomingProgress, incomingMessageId }) => {
				const payload = JSON.stringify(incomingProgress);
				const stompFrame = `MESSAGE\ndestination:/topic/solver/progress\nsubscription:sub-0\nmessage-id:${incomingMessageId}\ncontent-length:${payload.length}\n\n${payload}\0`;
				const sockjsFrame = 'a' + JSON.stringify([stompFrame]);
				(window as any).mockWebSocket.receive(sockjsFrame);
			},
			{ incomingProgress: progress, incomingMessageId: messageId }
		);
	}

	test('should load and show connected status', async ({ page }) => {
		await expect(page.getByText('Connected', { exact: true })).toBeVisible({ timeout: 5000 });
	});

	test('should generate demo data', async ({ page }) => {
		const generateBtn = page.locator('main').getByRole('button', { name: 'Generate Data' });
		await generateBtn.click();

		await expect(page.getByText('Generated: 5 buildings, 20 rooms, 10 instructors, 50 courses')).toBeVisible();
	});

	test('should start and stop solver, updating UI state', async ({ page }) => {
		const startBtn = page.getByRole('button', { name: 'Start Solver' });
		const stopBtn = page.getByRole('button', { name: 'Stop Solver' });
		await expect(page.getByText('Connected', { exact: true })).toBeVisible({ timeout: 5000 });

		await expect(startBtn).toBeVisible();
		await expect(stopBtn).not.toBeVisible();

		await startBtn.click();
		await expect(page.getByText('Solver started successfully')).toBeVisible();

		// Simulate WebSocket update: SOLVING_ACTIVE
		await sendProgress(
			page,
			{
				status: 'SOLVING_ACTIVE',
				score: '0hard/-100soft',
				assignedCourses: 10,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Solving...',
			},
			100
		);
		// Under full parallel load, dispatch once more to avoid a subscription race.
		await page.waitForTimeout(50);
		await sendProgress(
			page,
			{
				status: 'SOLVING_ACTIVE',
				score: '0hard/-100soft',
				assignedCourses: 10,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Solving...',
			},
			101
		);

		await expect(stopBtn).toBeVisible({ timeout: 10000 });
		await expect(startBtn).not.toBeVisible();
		await expect(page.getByText('Running')).toBeVisible();
		await expect(page.getByText('10/50')).toBeVisible();

		// Stop Solver
		await stopBtn.click();
		await expect(page.getByText('Solver stopped')).toBeVisible();

		// Simulate WebSocket update: NOT_SOLVING
		await sendProgress(
			page,
			{
				status: 'NOT_SOLVING',
				score: '0hard/-100soft',
				assignedCourses: 50,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Finished',
			},
			102
		);
		await page.waitForTimeout(50);
		await sendProgress(
			page,
			{
				status: 'NOT_SOLVING',
				score: '0hard/-100soft',
				assignedCourses: 50,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Finished',
			},
			103
		);

		await expect(startBtn).toBeVisible({ timeout: 10000 });
		await expect(stopBtn).not.toBeVisible();
	});

	test('should keep mobile scroll anchored to progress on start', async ({ page }) => {
		await page.setViewportSize({ width: 390, height: 844 });

		const main = page.locator('main');
		await main.getByRole('button', { name: 'Start Solver' }).click();
		await expect(page.getByText('Solver started successfully')).toBeVisible();
		await page.waitForTimeout(450);

		const layoutMetrics = await page.evaluate(() => {
			const mainEl = document.querySelector('main');
			const progressSection = document.querySelector('[data-testid="solver-progress-section"]');
			const headerEl = document.querySelector('header');
			if (!(mainEl instanceof HTMLElement) || !(progressSection instanceof HTMLElement) || !(headerEl instanceof HTMLElement)) {
				return null;
			}

			const mainRect = mainEl.getBoundingClientRect();
			const progressRect = progressSection.getBoundingClientRect();
			const headerRect = headerEl.getBoundingClientRect();

			return {
				progressTopWithinMain: progressRect.top - mainRect.top,
				headerVisible: headerRect.bottom > 0 && headerRect.top < window.innerHeight,
				pageScrollTop: document.scrollingElement?.scrollTop ?? 0,
			};
		});

		expect(layoutMetrics).not.toBeNull();
		if (!layoutMetrics) return;
		expect(layoutMetrics.progressTopWithinMain).toBeGreaterThanOrEqual(0);
		expect(layoutMetrics.progressTopWithinMain).toBeLessThanOrEqual(24);
		expect(layoutMetrics.headerVisible).toBeTruthy();
		expect(layoutMetrics.pageScrollTop).toBe(0);
	});

	test('should save solution and show view schedule link', async ({ page }) => {
		const main = page.locator('main');
		await expect(page.getByText('Connected', { exact: true })).toBeVisible({ timeout: 5000 });
		await main.getByRole('button', { name: 'Start Solver' }).click();
		await expect(page.getByText('Solver started successfully')).toBeVisible();

		await sendProgress(
			page,
			{
				status: 'NOT_SOLVING',
				score: '0hard/-100soft',
				assignedCourses: 50,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Finished',
			},
			200
		);

		const saveBtn = main.getByRole('button', { name: 'Save Final Schedule' }).first();
		await expect(saveBtn).toBeVisible({ timeout: 10000 });
		await expect(saveBtn).toBeEnabled();
		await saveBtn.click();

		await expect(page.getByText('Saved 50 schedules')).toBeVisible();
		await expect(page.getByRole('link', { name: 'View Schedule' })).toBeVisible();
	});

	test('should navigate to schedules from save confirmation link', async ({ page }) => {
		const main = page.locator('main');
		await expect(page.getByText('Connected', { exact: true })).toBeVisible({ timeout: 5000 });
		await main.getByRole('button', { name: 'Start Solver' }).click();
		await expect(page.getByText('Solver started successfully')).toBeVisible();

		await sendProgress(
			page,
			{
				status: 'NOT_SOLVING',
				score: '0hard/-100soft',
				assignedCourses: 50,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Finished',
			},
			300
		);

		const saveBtn = main.getByRole('button', { name: 'Save Final Schedule' }).first();
		await expect(saveBtn).toBeVisible({ timeout: 10000 });
		await expect(saveBtn).toBeEnabled();
		await saveBtn.click();

		const viewScheduleLink = page.getByRole('link', { name: 'View Schedule' });
		await expect(viewScheduleLink).toBeVisible();
		await viewScheduleLink.click();
		await expect(page).toHaveURL('/schedules');
	});

	test('should show error message when starting solver fails', async ({ page }) => {
		const main = page.locator('main');
		await page.route(/.*\/api\/solver\/start.*/, async route => {
			await route.fulfill({
				status: 500,
				contentType: 'application/json',
				json: { error: 'internal server error' },
			});
		});

		await main.getByRole('button', { name: 'Start Solver' }).click();

		await expect(main.getByText('Request failed with status code 500')).toBeVisible();
		await expect(main.getByRole('button', { name: 'Start Solver' })).toBeVisible();
	});
});
