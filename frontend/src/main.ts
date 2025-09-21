/**
 * Main Application Entry Point
 * 
 * This file initializes the Vue 3 application with:
 * - Vue Router for single-page application navigation
 * - Main App component and routing configuration
 * - Global styles and application mounting
 */
import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Editor from './views/Editor.vue'
import './style.css'

const routes = [
  { path: '/', component: Editor },
  { path: '/editor', component: Editor }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const app = createApp(App)
app.use(router)
app.mount('#app')
