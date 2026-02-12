<script setup lang="ts">
import Navbar from './Navbar.vue'
import Sidebar from './Sidebar.vue'
</script>

<template>
	<div class="h-screen flex flex-col overflow-hidden">
		<Navbar />
		<div class="flex flex-1 min-h-0 overflow-hidden">
			<Sidebar />
			<main class="flex-1 min-h-0 overflow-y-auto p-6 bg-gray-50">
				<slot />
			</main>
		</div>
	</div>
</template>
