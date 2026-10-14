<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  Check,
  Flame,
  Plus,
  Settings as SettingsIcon,
  TriangleAlert,
  Wheat,
  Droplet,
  X,
} from '@lucide/vue'
import ProgressRing from '@/components/ProgressRing.vue'
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

function ringColor(onTrack: boolean | null): string {
  if (onTrack === null) return 'stroke-white/30'
  return onTrack ? 'stroke-emerald-400' : 'stroke-red-400'
}

function ratio(value: number | undefined, goal: number | undefined): number {
  if (!value || !goal) return 0
  return value / goal
}

const kcalStatus = computed(() => {
  if (!summary.value || !settings.value || kcalGoal.value === undefined) return null
  const difference = Math.round(kcalGoal.value - summary.value.totalKcal)
  const left = formatNumber(Math.abs(difference))
  if (kcalOnTrack.value) {
    return settings.value.mode === 'CUT' ? `${left} kcal left` : 'On target'
  }
  return difference > 0 ? `${left} kcal to go` : `${left} kcal over`
})

const goalSource = computed(() =>
  energy.value ? 'Goal from WHOOP, last 7 days' : 'Goal from your settings',
)

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
  <header class="grid grid-cols-[2.5rem_1fr_2.5rem] items-center">
    <span />
    <p class="caps justify-self-center rounded-full bg-white/8 px-6 py-2 text-sm">Today</p>
    <RouterLink
      :to="{ name: 'settings' }"
      aria-label="Settings"
      class="flex size-10 items-center justify-center rounded-full text-muted-foreground"
    >
      <SettingsIcon class="size-6" />
    </RouterLink>
  </header>

  <section
    class="mt-8 flex items-end"
    :class="energy ? 'justify-between' : 'justify-center gap-10'"
  >
    <div class="flex w-24 flex-col items-center">
      <ProgressRing
        :progress="ratio(summary?.totalProtein, settings?.proteinGoalG)"
        :color-class="ringColor(proteinOnTrack)"
        :size="92"
        :stroke="6"
      >
        <span class="figure text-2xl">{{ formatNumber(summary?.totalProtein) }}</span>
        <span class="text-[11px] text-muted-foreground">g</span>
      </ProgressRing>
      <p class="caps mt-3 text-xs">Protein</p>
      <p v-if="settings" class="mt-0.5 text-xs text-muted-foreground">
        of {{ formatNumber(settings.proteinGoalG) }} g
      </p>
    </div>

    <div class="flex flex-col items-center">
      <ProgressRing
        :progress="ratio(summary?.totalKcal, kcalGoal)"
        :color-class="ringColor(kcalOnTrack)"
        :size="132"
        :stroke="8"
      >
        <span class="figure text-[2.5rem] leading-none">{{
          formatNumber(summary?.totalKcal)
        }}</span>
        <span v-if="kcalGoal !== undefined" class="mt-1 text-xs text-muted-foreground">
          of {{ formatNumber(kcalGoal) }} kcal
        </span>
      </ProgressRing>
      <p class="caps mt-3 text-xs">Calories</p>
      <p v-if="settings" class="mt-0.5 text-xs text-muted-foreground">{{ modeLabel }}</p>
    </div>

    <div v-if="energy" class="flex w-24 flex-col items-center">
      <ProgressRing
        :progress="ratio(energy.todayKcal, energy.weeklyAverageKcal)"
        color-class="stroke-orange-400"
        :size="92"
        :stroke="6"
      >
        <span class="figure text-2xl">{{ formatNumber(energy.todayKcal) }}</span>
        <span class="text-[11px] text-muted-foreground">kcal</span>
      </ProgressRing>
      <p class="caps mt-3 text-xs">Burned</p>
      <p class="mt-0.5 text-xs text-muted-foreground">today</p>
    </div>
  </section>

  <section v-if="kcalStatus" class="mt-8 flex items-center gap-4 rounded-2xl bg-card p-4">
    <span
      class="flex size-11 shrink-0 items-center justify-center rounded-xl"
      :class="kcalOnTrack ? 'bg-emerald-400/15 text-emerald-400' : 'bg-red-400/15 text-red-400'"
    >
      <Check v-if="kcalOnTrack" class="size-5" />
      <Flame v-else class="size-5" />
    </span>
    <div class="min-w-0">
      <p class="caps text-sm" :class="statusColor(kcalOnTrack)">{{ kcalStatus }}</p>
      <p class="mt-0.5 truncate text-sm text-muted-foreground">
        {{ goalSource }}
      </p>
    </div>
  </section>

  <section class="mt-3 grid grid-cols-2 gap-3">
    <div class="rounded-2xl bg-card px-4 py-3.5">
      <p class="caps flex items-center gap-2 text-xs text-muted-foreground">
        <Wheat class="size-4" />
        Carbs
      </p>
      <p class="figure mt-1 text-2xl">
        {{ formatNumber(summary?.totalCarbs) }}
        <span class="text-sm font-medium text-muted-foreground">g</span>
      </p>
    </div>
    <div class="rounded-2xl bg-card px-4 py-3.5">
      <p class="caps flex items-center gap-2 text-xs text-muted-foreground">
        <Droplet class="size-4" />
        Fat
      </p>
      <p class="figure mt-1 text-2xl">
        {{ formatNumber(summary?.totalFat) }}
        <span class="text-sm font-medium text-muted-foreground">g</span>
      </p>
    </div>
  </section>

  <section class="mt-8 pb-32">
    <h2 class="text-2xl font-semibold">Meals</h2>

    <div
      v-if="summary && summary.meals.length === 0"
      class="mt-4 rounded-2xl bg-card px-4 py-6 text-center"
    >
      <p class="caps text-sm">Nothing logged yet</p>
      <p class="mt-1 text-sm text-muted-foreground">Tap Add meal to log what you ate.</p>
    </div>

    <ul class="mt-4 space-y-2">
      <li v-for="meal in summary?.meals" :key="meal.id" class="rounded-2xl bg-card p-2">
        <div class="flex items-center gap-3">
          <div class="flex min-w-[4.75rem] flex-col items-center rounded-xl bg-white/8 px-3 py-2">
            <span class="figure text-xl leading-tight">{{ formatNumber(mealKcal(meal)) }}</span>
            <span class="text-[11px] text-muted-foreground">kcal</span>
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate font-medium">{{ meal.name }}</p>
            <p class="figure text-sm font-medium text-muted-foreground">
              {{ formatTime(meal.eatenAt) }}
            </p>
          </div>
          <button
            type="button"
            :aria-label="`Delete ${meal.name}`"
            :disabled="deletingId === meal.id"
            class="flex size-10 shrink-0 items-center justify-center rounded-full text-muted-foreground disabled:opacity-40"
            @click="remove(meal)"
          >
            <X class="size-5" />
          </button>
        </div>
        <ul v-if="meal.veggieHints.length > 0" class="mt-2 space-y-1 px-2 pb-1">
          <li
            v-for="hint in meal.veggieHints"
            :key="hint"
            class="flex items-start gap-2 text-xs text-amber-200"
          >
            <TriangleAlert class="mt-px size-3.5 shrink-0" />
            {{ hint }}
          </li>
        </ul>
      </li>
    </ul>
  </section>

  <div
    class="pointer-events-none fixed inset-x-0 bottom-0 mx-auto flex max-w-md justify-center bg-gradient-to-t from-background via-background/80 to-transparent px-4 pt-10 pb-[max(1.25rem,env(safe-area-inset-bottom))]"
  >
    <RouterLink
      :to="{ name: 'add-meal' }"
      class="caps pointer-events-auto flex items-center gap-2 rounded-full bg-white px-8 py-4 text-sm text-zinc-950 shadow-[0_8px_30px_rgb(0_0_0/0.45)]"
    >
      <Plus class="size-5" :stroke-width="2.5" />
      Add meal
    </RouterLink>
  </div>
</template>
