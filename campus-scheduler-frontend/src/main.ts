import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './style.css'
import Vue3Toastify, { type ToastContainerOptions } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'
import FloatingVue from 'floating-vue'
import 'floating-vue/dist/style.css'

const app = createApp(App)
const toastTheme = document.documentElement.getAttribute('data-theme') === 'slate' ? 'dark' : 'light'
app.use(router)
app.use(Vue3Toastify, {
	autoClose: 3000,
	position: 'bottom-right',
	theme: toastTheme,
	hideProgressBar: false,
	closeOnClick: false,
	pauseOnHover: false,
} as ToastContainerOptions)
app.use(FloatingVue)
app.mount('#app')
