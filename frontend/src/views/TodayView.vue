<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { deleteMeal, fetchToday } from '@/api/meals'
import { fetchSettings } from '@/api/settings'
import { fetchEnergy } from '@/api/whoop'
import type { DaySummary, Meal } from '@/types/meal'
import type { Settings } from '@/types/settings'
import type { Energy } from '@/types/whoop'

const MAINTAIN_TOLERANCE = 0.05

const summary = ref<DaySummary | null>(null)
const settings = ref<Settings | null>(null)
const energy = ref<Energy | null>(null)
const deletingId = ref<number | null>(null)

async function load() {
  const [today, currentSettings, currentEnergy] = await Promise.all([
    fetchToday(),
    fetchSettings(),
    fetchEnergy().catch(() => null),
  ])
  summary.value = today
  settings.value = currentSettings
  energy.value = currentEnergy
}

const kcalGoal = computed(() => energy.value?.weeklyAverageKcal ?? settings.value?.kcalReference)

const kcalOnTrack = computed(() => {
  if (!summary.value || !settings.value || kcalGoal.value === undefined) return null
  const eaten = summary.value.totalKcal
  const reference = kcalGoal.value
  switch (settings.value.mode) {
    case 'CUT':
      return eaten <= reference
    case 'BULK':
      return eaten >= reference
    default:
      return Math.abs(eaten - reference) <= reference * MAINTAIN_TOLERANCE
  }
})

const proteinOnTrack = computed(() => {
  if (!summary.value || !settings.value) return null
  return summary.value.totalProtein >= settings.value.proteinGoalG
})

const modeLabel = computed(() => {
  switch (settings.value?.mode) {
    case 'CUT':
      return 'Cut'
    case 'BULK':
      return 'Bulk'
    case 'MAINTAIN':
      return 'Maintain'
    default:
      return ''
  }
})

function statusColor(onTrack: boolean | null): string {
  if (onTrack === null) return ''
  return onTrack ? 'text-emerald-400' : 'text-red-400'
}

function barColor(onTrack: boolean | null): string {
  if (onTrack === null) return 'bg-muted-foreground'
  return onTrack ? 'bg-emerald-400' : 'bg-red-400'
}

function progress(value: number | undefined, goal: number | undefined): string {
  if (!value || !goal) return '0%'
  return `${Math.min(100, (value / goal) * 100)}%`
}

onMounted(load)

async function remove(meal: Meal) {
  if (!window.confirm(`Delete "${meal.name}"?`)) return

  deletingId.value = meal.id
  try {
    await deleteMeal(meal.id)
    await load()
  } catch {
    window.alert('Deleting failed. Is the backend running?')
  } finally {
    deletingId.value = null
  }
}

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
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-semibold">Today</h1>
    <RouterLink
      :to="{ name: 'settings' }"
      aria-label="Settings"
      class="-mr-2 flex size-10 items-center justify-center rounded-full text-xl"
    >
      ⚙️
    </RouterLink>
  </div>

  <section class="mt-6 rounded-2xl bg-card p-6 text-center">
    <p class="text-6xl font-bold tabular-nums" :class="statusColor(kcalOnTrack)">
      {{ formatNumber(summary?.totalKcal) }}
    </p>
    <p class="mt-1 text-sm text-muted-foreground">
      <template v-if="settings"> of {{ formatNumber(kcalGoal) }} kcal · {{ modeLabel }} </template>
      <template v-else>kcal</template>
    </p>
    <div v-if="settings" class="mt-4 h-2 overflow-hidden rounded-full bg-muted">
      <div
        class="h-full rounded-full transition-all"
        :class="barColor(kcalOnTrack)"
        :style="{ width: progress(summary?.totalKcal, kcalGoal) }"
      />
    </div>
    <p v-if="energy" class="mt-3 text-sm text-muted-foreground">
      🔥 {{ formatNumber(energy.todayKcal) }} kcal burned today
    </p>
  </section>

  <section class="mt-3 grid grid-cols-3 gap-3">
    <div class="rounded-2xl bg-card p-4 text-center">
      <p class="text-2xl font-semibold tabular-nums" :class="statusColor(proteinOnTrack)">
        {{ formatNumber(summary?.totalProtein) }} g
      </p>
      <p class="mt-1 text-xs text-muted-foreground">
        Protein<template v-if="settings"> / {{ formatNumber(settings.proteinGoalG) }}</template>
      </p>
      <div v-if="settings" class="mt-2 h-1.5 overflow-hidden rounded-full bg-muted">
        <div
          class="h-full rounded-full transition-all"
          :class="barColor(proteinOnTrack)"
          :style="{ width: progress(summary?.totalProtein, settings.proteinGoalG) }"
        />
      </div>
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

  <section class="mt-8 pb-24">
    <h2 class="text-sm font-medium text-muted-foreground">Meals</h2>

    <p v-if="summary && summary.meals.length === 0" class="mt-3 text-sm text-muted-foreground">
      Nothing logged yet today.
    </p>

    <ul class="mt-3 space-y-2">
      <li v-for="meal in summary?.meals" :key="meal.id" class="rounded-2xl bg-card px-4 py-3">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="font-medium">{{ meal.name }}</p>
            <p class="text-xs text-muted-foreground">{{ formatTime(meal.eatenAt) }}</p>
          </div>
          <div class="flex shrink-0 items-center gap-1">
            <p class="tabular-nums">{{ formatNumber(mealKcal(meal)) }} kcal</p>
            <button
              type="button"
              :aria-label="`Delete ${meal.name}`"
              :disabled="deletingId === meal.id"
              class="-mr-2 flex size-9 items-center justify-center rounded-full text-muted-foreground disabled:opacity-40"
              @click="remove(meal)"
            >
              ✕
            </button>
          </div>
        </div>
        <ul v-if="meal.veggieHints.length > 0" class="mt-2 space-y-1 text-xs text-amber-200">
          <li v-for="hint in meal.veggieHints" :key="hint">⚠️ {{ hint }}</li>
        </ul>
      </li>
    </ul>
  </section>

  <RouterLink
    :to="{ name: 'add-meal' }"
    aria-label="Add meal"
    class="fixed right-4 bottom-[max(1.5rem,env(safe-area-inset-bottom))] flex size-14 items-center justify-center rounded-full bg-emerald-400 text-3xl text-zinc-950 shadow-lg"
  >
    +
  </RouterLink>
</template>
