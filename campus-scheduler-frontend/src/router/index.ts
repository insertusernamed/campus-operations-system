import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
	{
		path: '/',
		name: 'home',
		component: () => import('@/views/HomeView.vue'),
	},
	{
		path: '/rooms',
		name: 'rooms',
		component: () => import('@/views/rooms/RoomsList.vue'),
	},
	{
		path: '/rooms/:id',
		name: 'room-detail',
		component: () => import('@/views/rooms/RoomDetail.vue'),
	},
]

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
	routes,
})

export default router
