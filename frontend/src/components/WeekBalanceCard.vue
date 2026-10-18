<script setup lang="ts">
import { computed } from 'vue'
import type { DayBalance, WeekBalance } from '@/types/balance'
import type { Settings } from '@/types/settings'

const props = defineProps<{ week: WeekBalance; mode: Settings['mode'] }>()
const emit = defineEmits<{ selectDay: [date: string] }>()

const KCAL_PER_KG = 7700
const MAINTAIN_TOLERANCE = 0.05
const BAR_HEIGHT = 28

const number = new Intl.NumberFormat('de-AT', { maximumFractionDigits: 0 })
const oneDecimal = new Intl.NumberFormat('de-AT', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

function signed(value: number, formatter: Intl.NumberFormat): string {
  if (value === 0) return formatter.format(0)
  return `${value > 0 ? '+' : '−'}${formatter.format(Math.abs(value))}`
}

const maxBalance = computed(() =>
  Math.max(
    1,
    ...props.week.days.filter((day) => day.logged).map((day) => Math.abs(day.balanceKcal)),
  ),
)

function onPlan(day: DayBalance): boolean {
  if (props.mode === 'CUT') return day.balanceKcal <= 0
  if (props.mode === 'BULK') return day.balanceKcal >= 0
  return Math.abs(day.balanceKcal) <= day.burnedKcal * MAINTAIN_TOLERANCE
}

function barHeight(day: DayBalance): string {
  return `${Math.max(3, (Math.abs(day.balanceKcal) / maxBalance.value) * BAR_HEIGHT)}px`
}

function weekday(date: string): string {
  return new Date(`${date}T00:00:00Z`)
    .toLocaleDateString('en-GB', { weekday: 'short', timeZone: 'UTC' })
    .slice(0, 2)
}

function dayLabel(day: DayBalance): string {
  if (!day.logged) return `${weekday(day.date)}: nothing logged`
  return `${weekday(day.date)}: ${signed(day.balanceKcal, number)} kcal, ate ${number.format(day.eatenKcal)}, burned ${number.format(day.burnedKcal)}`
}
</script>

<template>
  <section class="rounded-2xl bg-card px-4 pt-3.5 pb-3">
    <div class="flex items-baseline justify-between gap-3">
      <p class="caps text-xs text-muted-foreground">Last 7 days</p>
      <p class="figure text-xl">
        {{ signed(week.balanceKcal, number) }}
        <span class="text-xs font-medium text-muted-foreground">
          kcal · ≈ {{ signed(week.balanceKcal / KCAL_PER_KG, oneDecimal) }} kg
        </span>
      </p>
    </div>

    <ul class="mt-3 grid grid-cols-7 gap-1.5" aria-label="Calorie balance per day">
      <li v-for="day in week.days" :key="day.date">
        <button
          type="button"
          class="flex w-full flex-col items-center"
          :aria-label="dayLabel(day)"
          @click="emit('selectDay', day.date)"
        >
          <span
            class="flex w-full flex-col items-center"
            :style="{ height: `${BAR_HEIGHT * 2 + 2}px` }"
          >
            <span class="flex w-full flex-1 items-end justify-center">
              <span
                v-if="day.logged && day.balanceKcal > 0"
                class="w-full max-w-5 rounded-t-[4px]"
                :class="onPlan(day) ? 'bg-emerald-400' : 'bg-red-400'"
                :style="{ height: barHeight(day) }"
              />
            </span>
            <span class="h-0.5 w-full bg-white/15" />
            <span class="flex w-full flex-1 items-start justify-center">
              <span
                v-if="day.logged && day.balanceKcal <= 0"
                class="w-full max-w-5 rounded-b-[4px]"
                :class="onPlan(day) ? 'bg-emerald-400' : 'bg-red-400'"
                :style="{ height: barHeight(day) }"
              />
              <span
                v-if="!day.logged"
                class="mt-1 size-1.5 rounded-full bg-white/20"
                aria-hidden="true"
              />
            </span>
          </span>
          <span class="mt-1 text-[10px] text-muted-foreground">{{ weekday(day.date) }}</span>
        </button>
      </li>
    </ul>

    <p v-if="week.loggedDays < week.days.length" class="mt-1.5 text-[11px] text-muted-foreground">
      {{ week.loggedDays }} of {{ week.days.length }} days logged, empty days are not counted
    </p>
  </section>
</template>
