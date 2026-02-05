import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// Eagerly load lightweight nav routes (~15KB total)
// Heavy routes (Schedules, Solver, Analytics) stay lazy-loaded
import HomeView from '@/views/HomeView.vue'
import BuildingsList from '@/views/buildings/BuildingsList.vue'
import RoomsList from '@/views/rooms/RoomsList.vue'
import InstructorsList from '@/views/instructors/InstructorsList.vue'
import CoursesList from '@/views/courses/CoursesList.vue'
import TimeSlotsList from '@/views/timeslots/TimeSlotsList.vue'
import { useRole } from '@/composables/useRole'

const routes: RouteRecordRaw[] = [
	{ path: '/', name: 'home', component: HomeView },

	// Analytics (lazy - has charts)
	{ path: '/analytics', name: 'analytics', component: () => import('@/views/analytics/AnalyticsDashboard.vue') },

	// Buildings
	{ path: '/buildings', name: 'buildings', component: BuildingsList },
	{ path: '/buildings/new', name: 'building-create', component: () => import('@/views/buildings/BuildingForm.vue') },
	{ path: '/buildings/:id', name: 'building-detail', component: () => import('@/views/buildings/BuildingDetail.vue') },
	{ path: '/buildings/:id/edit', name: 'building-edit', component: () => import('@/views/buildings/BuildingForm.vue') },

	// Rooms
	{ path: '/rooms', name: 'rooms', component: RoomsList },
	{ path: '/rooms/new', name: 'room-create', component: () => import('@/views/rooms/RoomForm.vue') },
	{ path: '/rooms/:id', name: 'room-detail', component: () => import('@/views/rooms/RoomDetail.vue') },
	{ path: '/rooms/:id/edit', name: 'room-edit', component: () => import('@/views/rooms/RoomForm.vue') },

	// Instructors
	{ path: '/instructors', name: 'instructors', component: InstructorsList },
	{ path: '/instructors/new', name: 'instructor-create', component: () => import('@/views/instructors/InstructorForm.vue') },
	{ path: '/instructors/:id', name: 'instructor-detail', component: () => import('@/views/instructors/InstructorDetail.vue') },
	{ path: '/instructors/:id/edit', name: 'instructor-edit', component: () => import('@/views/instructors/InstructorForm.vue') },

	// Courses
	{ path: '/courses', name: 'courses', component: CoursesList },
	{ path: '/courses/new', name: 'course-create', component: () => import('@/views/courses/CourseForm.vue') },
	{ path: '/courses/:id', name: 'course-detail', component: () => import('@/views/courses/CourseDetail.vue') },
	{ path: '/courses/:id/edit', name: 'course-edit', component: () => import('@/views/courses/CourseForm.vue') },

	// Time Slots
	{ path: '/timeslots', name: 'timeslots', component: TimeSlotsList },
	{ path: '/timeslots/new', name: 'timeslot-create', component: () => import('@/views/timeslots/TimeSlotForm.vue') },
	{ path: '/timeslots/:id/edit', name: 'timeslot-edit', component: () => import('@/views/timeslots/TimeSlotForm.vue') },

	// Schedules (lazy - has calendar component ~240KB)
	{ path: '/schedules', name: 'schedules', component: () => import('@/views/schedules/SchedulesList.vue') },
	{ path: '/schedules/new', name: 'schedule-create', component: () => import('@/views/schedules/ScheduleForm.vue') },

	// Solver (lazy - heavy component ~88KB)
	{ path: '/solver', name: 'solver', component: () => import('@/views/solver/SolverPage.vue') },

	// Change Requests
	{ path: '/requests', name: 'requests', component: () => import('@/views/requests/MyRequests.vue') },
	{ path: '/requests/new', name: 'requests-new', component: () => import('@/views/requests/RequestChangeForm.vue') },
	{ path: '/requests/admin', name: 'requests-admin', component: () => import('@/views/requests/ChangeRequestsAdmin.vue') },
]

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
	routes,
})

router.beforeEach((to, _from, next) => {
	const { role } = useRole()

	if (to.path === '/requests/admin' && role.value !== 'admin') {
		return next('/requests')
	}
	if (to.path === '/requests/new' && role.value === 'admin') {
		return next('/requests/admin')
	}
	if (to.path === '/requests' && role.value === 'admin') {
		return next('/requests/admin')
	}
	if ((to.path === '/requests' || to.path === '/requests/new') && role.value === 'student') {
		return next('/schedules')
	}
	if (to.path === '/schedules/new' && role.value !== 'admin') {
		return next('/schedules')
	}

	return next()
})

export default router
