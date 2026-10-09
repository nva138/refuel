import { createRouter, createWebHistory } from 'vue-router'
import TodayView from '@/views/TodayView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [{ path: '/', name: 'today', component: TodayView }],
})

export default router
