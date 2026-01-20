<script setup lang="ts">
import Navbar from './Navbar.vue'
import Sidebar from './Sidebar.vue'
</script>

<template>
	<div class="min-h-screen flex flex-col">
		<Navbar />
		<div class="flex flex-1">
			<Sidebar />
			<main class="flex-1 p-6 bg-gray-50">
				<slot />
			</main>
		</div>
	</div>
</template>
