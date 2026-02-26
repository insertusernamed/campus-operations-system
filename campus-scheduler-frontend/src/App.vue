<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import { useRouteTransition } from '@/composables/useRouteTransition'

const { routeTransition } = useRouteTransition()
</script>

<template>
	<AppLayout>
		<RouterView v-slot="{ Component, route }">
			<Transition :name="routeTransition" mode="out-in" appear>
				<component :is="Component" :key="route.fullPath" />
			</Transition>
		</RouterView>
	</AppLayout>
</template>
