import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },

    // Buildings
    { path: '/buildings', name: 'buildings', component: () => import('@/views/buildings/BuildingsList.vue') },
    { path: '/buildings/new', name: 'building-create', component: () => import('@/views/buildings/BuildingForm.vue') },
    { path: '/buildings/:id', name: 'building-detail', component: () => import('@/views/buildings/BuildingDetail.vue') },
    { path: '/buildings/:id/edit', name: 'building-edit', component: () => import('@/views/buildings/BuildingForm.vue') },

    // Rooms
    { path: '/rooms', name: 'rooms', component: () => import('@/views/rooms/RoomsList.vue') },
    { path: '/rooms/new', name: 'room-create', component: () => import('@/views/rooms/RoomForm.vue') },
    { path: '/rooms/:id', name: 'room-detail', component: () => import('@/views/rooms/RoomDetail.vue') },
    { path: '/rooms/:id/edit', name: 'room-edit', component: () => import('@/views/rooms/RoomForm.vue') },

    // Instructors
    { path: '/instructors', name: 'instructors', component: () => import('@/views/instructors/InstructorsList.vue') },
    { path: '/instructors/new', name: 'instructor-create', component: () => import('@/views/instructors/InstructorForm.vue') },
    { path: '/instructors/:id', name: 'instructor-detail', component: () => import('@/views/instructors/InstructorDetail.vue') },
    { path: '/instructors/:id/edit', name: 'instructor-edit', component: () => import('@/views/instructors/InstructorForm.vue') },

    // Courses
    { path: '/courses', name: 'courses', component: () => import('@/views/courses/CoursesList.vue') },
    { path: '/courses/new', name: 'course-create', component: () => import('@/views/courses/CourseForm.vue') },
    { path: '/courses/:id', name: 'course-detail', component: () => import('@/views/courses/CourseDetail.vue') },
    { path: '/courses/:id/edit', name: 'course-edit', component: () => import('@/views/courses/CourseForm.vue') },

    // Time Slots
    { path: '/timeslots', name: 'timeslots', component: () => import('@/views/timeslots/TimeSlotsList.vue') },
    { path: '/timeslots/new', name: 'timeslot-create', component: () => import('@/views/timeslots/TimeSlotForm.vue') },
    { path: '/timeslots/:id/edit', name: 'timeslot-edit', component: () => import('@/views/timeslots/TimeSlotForm.vue') },

    // Schedules
    { path: '/schedules', name: 'schedules', component: () => import('@/views/schedules/SchedulesList.vue') },
    { path: '/schedules/new', name: 'schedule-create', component: () => import('@/views/schedules/ScheduleForm.vue') },
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
})

export default router
