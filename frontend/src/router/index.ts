import { createRouter, createWebHistory } from 'vue-router'
import TodayView from '@/views/TodayView.vue'
import AddMealView from '@/views/AddMealView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'today', component: TodayView },
    { path: '/add', name: 'add-meal', component: AddMealView },
  ],
})

export default router
