import { test, expect } from '@playwright/test';

test.describe('Solver Page', () => {

	test.beforeEach(async ({ page }) => {

		await page.route(/.*\/api\/generator\/university\/small.*/, async route => {
			await route.fulfill({
				json: {
					buildings: 5,
					rooms: 20,
					instructors: 10,
					courses: 50
				}
			});
		});

		await page.route(/.*\/api\/solver\/start.*/, async route => {
			await route.fulfill({ json: { message: 'Solver started successfully' } });
		});

		await page.route(/.*\/api\/solver\/stop.*/, async route => {
			await route.fulfill({ json: { message: 'Solver stopped' } });
		});

		await page.route(/.*\/api\/solver\/save.*/, async route => {
			await route.fulfill({ json: { message: 'Solution Saved successfully' } });
		});

		// Mock SockJS info request to allow WebSocket connection
		await page.route('**/ws/info**', async route => {
			await route.fulfill({
				json: {
					websocket: true,
					origins: ["*:*"],
					cookie_needed: false,
					entropy: 1234567890
				}
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

	test('should load and show connected status', async ({ page }) => {
		await expect(page.getByText('Connected', { exact: true })).toBeVisible({ timeout: 5000 });
	});

	test('should generate demo data', async ({ page }) => {
		const generateBtn = page.getByRole('button', { name: 'Generate Demo Data' });
		await generateBtn.click();

		await expect(page.getByText('Generated: 5 buildings, 20 rooms, 10 instructors, 50 courses')).toBeVisible();
	});

	test('should start and stop solver, updating UI state', async ({ page }) => {
		const startBtn = page.getByRole('button', { name: 'Start Solver' });
		const stopBtn = page.getByRole('button', { name: 'Stop Solver' });

		await expect(startBtn).toBeVisible();
		await expect(stopBtn).not.toBeVisible();

		await startBtn.click();
		await expect(page.getByText('Solver started successfully')).toBeVisible();

		// Simulate WebSocket update: SOLVING_ACTIVE
		await page.evaluate(() => {
			const progress = {
				status: 'SOLVING_ACTIVE',
				score: '0hard/-100soft',
				assignedCourses: 10,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Solving...'
			};
			const payload = JSON.stringify(progress);
			// Construct STOMP MESSAGE frame
			const stompFrame = `MESSAGE\ndestination:/topic/solver/progress\nsubscription:sub-0\nmessage-id:100\ncontent-length:${payload.length}\n\n${payload}\0`;
			// Wrap in SockJS frame
			const sockjsFrame = 'a' + JSON.stringify([stompFrame]);
			(window as any).mockWebSocket.receive(sockjsFrame);
		});

		await expect(stopBtn).toBeVisible();
		await expect(startBtn).not.toBeVisible();
		await expect(page.getByText('Running')).toBeVisible();
		await expect(page.getByText('10/50')).toBeVisible();

		// Stop Solver
		await stopBtn.click();
		await expect(page.getByText('Solver stopped')).toBeVisible();

		// Simulate WebSocket update: NOT_SOLVING
		await page.evaluate(() => {
			const progress = {
				status: 'NOT_SOLVING',
				score: '0hard/-100soft',
				assignedCourses: 50,
				totalCourses: 50,
				hardViolations: 0,
				softScore: -100,
				message: 'Finished'
			};
			const payload = JSON.stringify(progress);
			const stompFrame = `MESSAGE\ndestination:/topic/solver/progress\nsubscription:sub-0\nmessage-id:101\ncontent-length:${payload.length}\n\n${payload}\0`;
			const sockjsFrame = 'a' + JSON.stringify([stompFrame]);
			(window as any).mockWebSocket.receive(sockjsFrame);
		});

		await expect(startBtn).toBeVisible();
		await expect(stopBtn).not.toBeVisible();
	});

	test('should save solution and show view schedule link', async ({ page }) => {
		const saveBtn = page.getByRole('button', { name: 'Save Solution' });
		await saveBtn.click();

		await expect(page.getByText('Solution Saved successfully')).toBeVisible();
		await expect(page.getByRole('link', { name: 'View Schedule' })).toBeVisible();
	});
});
