<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { fetchToday } from '@/api/meals'
import type { DaySummary, Meal } from '@/types/meal'

const summary = ref<DaySummary | null>(null)

onMounted(async () => {
  summary.value = await fetchToday()
})

const number = new Intl.NumberFormat('de-AT', { maximumFractionDigits: 0 })

function formatNumber(value: number | undefined): string {
  return number.format(value ?? 0)
}

function formatTime(eatenAt: string): string {
  return new Date(eatenAt).toLocaleTimeString('de-AT', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Vienna',
  })
}

function mealKcal(meal: Meal): number {
  return meal.parts.reduce((sum, part) => sum + (part.kcalPer100g * part.amountG) / 100, 0)
}
</script>

<template>
  <h1 class="text-2xl font-semibold">Today</h1>

  <section class="mt-6 rounded-2xl bg-card p-6 text-center">
    <p class="text-6xl font-bold tabular-nums">{{ formatNumber(summary?.totalKcal) }}</p>
    <p class="mt-1 text-sm text-muted-foreground">kcal</p>
  </section>

  <section class="mt-3 grid grid-cols-3 gap-3">
    <div class="rounded-2xl bg-card p-4 text-center">
      <p class="text-2xl font-semibold tabular-nums">{{ formatNumber(summary?.totalProtein) }} g</p>
      <p class="mt-1 text-xs text-muted-foreground">Protein</p>
    </div>
    <div class="rounded-2xl bg-card p-4 text-center">
      <p class="text-2xl font-semibold tabular-nums">{{ formatNumber(summary?.totalCarbs) }} g</p>
      <p class="mt-1 text-xs text-muted-foreground">Carbs</p>
    </div>
    <div class="rounded-2xl bg-card p-4 text-center">
      <p class="text-2xl font-semibold tabular-nums">{{ formatNumber(summary?.totalFat) }} g</p>
      <p class="mt-1 text-xs text-muted-foreground">Fat</p>
    </div>
  </section>

  <section class="mt-8">
    <h2 class="text-sm font-medium text-muted-foreground">Meals</h2>

    <p v-if="summary && summary.meals.length === 0" class="mt-3 text-sm text-muted-foreground">
      Nothing logged yet today.
    </p>

    <ul class="mt-3 space-y-2">
      <li
        v-for="meal in summary?.meals"
        :key="meal.id"
        class="flex items-center justify-between rounded-2xl bg-card px-4 py-3"
      >
        <div>
          <p class="font-medium">{{ meal.name }}</p>
          <p class="text-xs text-muted-foreground">{{ formatTime(meal.eatenAt) }}</p>
        </div>
        <p class="tabular-nums">{{ formatNumber(mealKcal(meal)) }} kcal</p>
      </li>
    </ul>
  </section>
</template>
