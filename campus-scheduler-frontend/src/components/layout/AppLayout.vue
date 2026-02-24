<script setup lang="ts">
import { ref } from 'vue'
import Navbar from './Navbar.vue'
import Sidebar from './Sidebar.vue'

const sidebarOpen = ref(false)

function toggleSidebar() {
	sidebarOpen.value = !sidebarOpen.value
}

function closeSidebar() {
	sidebarOpen.value = false
}
</script>

<template>
	<div class="h-screen flex flex-col overflow-hidden">
		<Navbar @toggle-sidebar="toggleSidebar" />
		<div class="flex flex-1 min-h-0 overflow-hidden relative">
			<!-- Mobile backdrop -->
			<Transition name="fade">
				<div v-if="sidebarOpen" class="fixed inset-0 z-30 bg-black/40 md:hidden" @click="closeSidebar" />
			</Transition>
			<Sidebar :is-open="sidebarOpen" @close="closeSidebar" />
			<main class="flex-1 min-h-0 overflow-y-auto p-3 sm:p-6 bg-gray-50">
				<slot />
			</main>
		</div>
	</div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
	transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
	opacity: 0;
}
</style>
