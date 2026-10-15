import { createRouter, createWebHistory } from 'vue-router'
import TodayView from '@/views/TodayView.vue'
import AddMealView from '@/views/AddMealView.vue'
import SettingsView from '@/views/SettingsView.vue'
import WeightView from '@/views/WeightView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'today', component: TodayView },
    { path: '/add', name: 'add-meal', component: AddMealView },
    { path: '/settings', name: 'settings', component: SettingsView },
    { path: '/weight', name: 'weight', component: WeightView },
  ],
})

export default router
