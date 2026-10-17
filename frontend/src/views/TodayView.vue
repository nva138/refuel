<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import {
  Weight as WeightIcon,
  Check,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Pencil,
  RotateCcw,
  Flame,
  Plus,
  Settings as SettingsIcon,
  TriangleAlert,
  Wheat,
  Droplet,
  X,
} from '@lucide/vue'
import ProgressRing from '@/components/ProgressRing.vue'
import { deleteMeal, fetchDay, fetchToday, updatePartAmount } from '@/api/meals'
import { fetchSettings } from '@/api/settings'
import { fetchEnergy } from '@/api/whoop'
import type { DaySummary, Meal, MealPart } from '@/types/meal'
import type { Settings } from '@/types/settings'
import type { Energy } from '@/types/whoop'

const MAINTAIN_TOLERANCE = 0.05

const summary = ref<DaySummary | null>(null)
const settings = ref<Settings | null>(null)
const energy = ref<Energy | null>(null)
const deletingId = ref<number | null>(null)

function viennaToday(): string {
  return new Date().toLocaleDateString('sv-SE', { timeZone: 'Europe/Vienna' })
}

function shiftDate(date: string, days: number): string {
  const shifted = new Date(`${date}T00:00:00Z`)
  shifted.setUTCDate(shifted.getUTCDate() + days)
  return shifted.toISOString().slice(0, 10)
}

const selectedDate = ref(viennaToday())
const isToday = computed(() => selectedDate.value === viennaToday())

const dayLabel = computed(() => {
  if (isToday.value) return 'Today'
  if (selectedDate.value === shiftDate(viennaToday(), -1)) return 'Yesterday'
  return new Date(`${selectedDate.value}T00:00:00Z`).toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  })
})

async function load() {
  const date = selectedDate.value
  const [day, currentSettings, currentEnergy] = await Promise.all([
    isToday.value ? fetchToday() : fetchDay(date),
    fetchSettings(),
    (isToday.value ? fetchEnergy() : fetchEnergy(date)).catch(() => null),
  ])
  if (date !== selectedDate.value) return
  summary.value = day
  settings.value = currentSettings
  energy.value = currentEnergy
}

function goToDay(days: number) {
  const next = shiftDate(selectedDate.value, days)
  if (next > viennaToday()) return
  selectedDate.value = next
  load()
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
  if (!isToday.value) {
    if (Math.abs(difference) < 1) return 'On target'
    return difference > 0 ? `${left} kcal under goal` : `${left} kcal over goal`
  }
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

const router = useRouter()

function logAgain(meal: Meal) {
  router.push({ name: 'add-meal', state: { repeatMeal: JSON.parse(JSON.stringify(meal)) } })
}

const expandedIds = ref(new Set<number>())

function toggleMeal(id: number) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedIds.value = next
}

const editingPartId = ref<number | null>(null)
const editAmount = ref('')
const savingAmount = ref(false)

function startEditing(part: MealPart) {
  editingPartId.value = part.id
  editAmount.value = String(part.amountG).replace('.', ',')
}

async function saveAmount(meal: Meal, part: MealPart) {
  const amountG = Number(editAmount.value.replace(',', '.'))
  if (!(amountG > 0)) return
  savingAmount.value = true
  try {
    await updatePartAmount(meal.id, part.id, amountG)
    editingPartId.value = null
    await load()
  } catch {
    window.alert('Saving failed. Is the backend running?')
  } finally {
    savingAmount.value = false
  }
}

function partValue(part: MealPart, perHundredGrams: number): number {
  return (perHundredGrams * part.amountG) / 100
}

function mealMacro(meal: Meal, key: 'proteinPer100g' | 'carbsPer100g' | 'fatPer100g'): number {
  return meal.parts.reduce((sum, part) => sum + partValue(part, part[key]), 0)
}

function mealKcal(meal: Meal): number {
  return meal.parts.reduce((sum, part) => sum + (part.kcalPer100g * part.amountG) / 100, 0)
}
</script>

<template>
  <header class="grid grid-cols-[2.5rem_1fr_2.5rem] items-center">
    <RouterLink
      :to="{ name: 'weight' }"
      aria-label="Weight"
      class="flex size-10 items-center justify-center rounded-full text-muted-foreground"
    >
      <WeightIcon class="size-6" />
    </RouterLink>
    <div class="flex items-center justify-self-center rounded-full bg-white/8">
      <button
        type="button"
        aria-label="Previous day"
        class="flex size-9 items-center justify-center rounded-full text-muted-foreground"
        @click="goToDay(-1)"
      >
        <ChevronLeft class="size-5" />
      </button>
      <p class="caps min-w-28 text-center text-sm" aria-live="polite">{{ dayLabel }}</p>
      <button
        type="button"
        aria-label="Next day"
        :disabled="isToday"
        class="flex size-9 items-center justify-center rounded-full text-muted-foreground disabled:opacity-25"
        @click="goToDay(1)"
      >
        <ChevronRight class="size-5" />
      </button>
    </div>
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
      <p class="mt-0.5 text-xs text-muted-foreground">{{ isToday ? 'today' : 'that day' }}</p>
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
      <p class="mt-1 text-sm text-muted-foreground">
        {{ isToday ? 'Tap Add meal to log what you ate.' : 'No meals were logged that day.' }}
      </p>
    </div>

    <ul class="mt-4 space-y-2">
      <li v-for="meal in summary?.meals" :key="meal.id" class="rounded-2xl bg-card p-2">
        <div class="flex items-center gap-1">
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-3 text-left"
            :aria-expanded="expandedIds.has(meal.id)"
            :aria-controls="`meal-${meal.id}-details`"
            @click="toggleMeal(meal.id)"
          >
            <span
              class="flex min-w-[4.75rem] flex-col items-center rounded-xl bg-white/8 px-3 py-2"
            >
              <span class="figure text-xl leading-tight">{{ formatNumber(mealKcal(meal)) }}</span>
              <span class="text-[11px] text-muted-foreground">kcal</span>
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate font-medium">{{ meal.name }}</span>
              <span class="figure block text-sm font-medium text-muted-foreground">
                {{ formatTime(meal.eatenAt) }}
              </span>
            </span>
            <ChevronDown
              class="size-5 shrink-0 text-muted-foreground transition-transform"
              :class="{ 'rotate-180': expandedIds.has(meal.id) }"
            />
          </button>
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
        <div
          v-if="expandedIds.has(meal.id)"
          :id="`meal-${meal.id}-details`"
          class="mx-2 mt-3 mb-1 border-t border-white/8 pt-3"
        >
          <ul class="space-y-2.5">
            <li
              v-for="part in meal.parts"
              :key="part.id"
              class="flex items-baseline justify-between gap-3"
            >
              <form
                v-if="editingPartId === part.id"
                class="flex w-full items-center gap-2"
                @submit.prevent="saveAmount(meal, part)"
              >
                <span class="min-w-0 flex-1 truncate text-sm">{{ part.name }}</span>
                <label class="flex items-center gap-1">
                  <span class="sr-only">Amount of {{ part.name }} in grams</span>
                  <input
                    v-model="editAmount"
                    inputmode="decimal"
                    class="figure w-20 rounded-lg bg-white/8 px-2 py-1.5 text-right text-sm outline-none ring-1 ring-white/15 focus:ring-white/40"
                  />
                  <span class="text-xs text-muted-foreground">g</span>
                </label>
                <button
                  type="submit"
                  :disabled="savingAmount"
                  class="caps rounded-lg bg-white px-3 py-1.5 text-[11px] text-zinc-950 disabled:opacity-50"
                >
                  Save
                </button>
                <button
                  type="button"
                  aria-label="Cancel"
                  class="flex size-8 items-center justify-center text-muted-foreground"
                  @click="editingPartId = null"
                >
                  <X class="size-4" />
                </button>
              </form>
              <template v-else>
                <span class="min-w-0">
                  <span class="block truncate text-sm">{{ part.name }}</span>
                  <button
                    type="button"
                    class="figure flex items-center gap-1.5 text-xs font-medium text-muted-foreground"
                    :aria-label="`Change amount of ${part.name}, currently ${formatNumber(part.amountG)} g`"
                    @click="startEditing(part)"
                  >
                    <span class="underline decoration-dotted underline-offset-2"
                      >{{ formatNumber(part.amountG) }} g</span
                    >
                    <Pencil class="size-3" />
                    <span
                      >· {{ formatNumber(partValue(part, part.proteinPer100g)) }} g protein</span
                    >
                  </button>
                </span>
                <span class="figure shrink-0 text-sm">
                  {{ formatNumber(partValue(part, part.kcalPer100g)) }} kcal
                </span>
              </template>
            </li>
          </ul>
          <p class="figure mt-3 flex gap-4 text-xs font-medium text-muted-foreground">
            <span>P {{ formatNumber(mealMacro(meal, 'proteinPer100g')) }} g</span>
            <span>C {{ formatNumber(mealMacro(meal, 'carbsPer100g')) }} g</span>
            <span>F {{ formatNumber(mealMacro(meal, 'fatPer100g')) }} g</span>
          </p>
          <p v-if="meal.enteredText" class="mt-2 text-xs text-muted-foreground italic">
            “{{ meal.enteredText }}”
          </p>
          <button
            type="button"
            class="caps mt-3 flex w-full items-center justify-center gap-2 rounded-xl bg-white/8 py-3 text-xs"
            @click="logAgain(meal)"
          >
            <RotateCcw class="size-4" />
            Log again today
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
    v-if="isToday"
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
