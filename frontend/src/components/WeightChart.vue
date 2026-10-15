<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Weight } from '@/types/weight'

const props = defineProps<{ entries: Weight[]; days: number }>()

const WIDTH = 343
const HEIGHT = 200
const PADDING = { top: 16, right: 12, bottom: 28, left: 36 }

const DAY_MS = 24 * 60 * 60 * 1000

function toTime(date: string): number {
  return new Date(`${date}T00:00:00Z`).getTime()
}

const endTime = computed(() => {
  const now = new Date()
  return Date.UTC(now.getFullYear(), now.getMonth(), now.getDate())
})
const startTime = computed(() => endTime.value - (props.days - 1) * DAY_MS)

const GRID_STEPS = 3

const range = computed(() => {
  const values = props.entries.map((entry) => entry.weightKg)
  if (values.length === 0) return { min: 0, max: GRID_STEPS, step: 1 }
  const low = Math.floor(Math.min(...values) - 0.5)
  const high = Math.ceil(Math.max(...values) + 0.5)
  const step = Math.max(1, Math.ceil((high - low) / GRID_STEPS))
  return { min: low, max: low + step * GRID_STEPS, step }
})

function x(date: string): number {
  const span = endTime.value - startTime.value || 1
  const ratio = (toTime(date) - startTime.value) / span
  return PADDING.left + ratio * (WIDTH - PADDING.left - PADDING.right)
}

function y(weightKg: number): number {
  const { min, max } = range.value
  const ratio = (weightKg - min) / (max - min)
  return HEIGHT - PADDING.bottom - ratio * (HEIGHT - PADDING.top - PADDING.bottom)
}

const points = computed(() =>
  props.entries.map((entry) => ({ entry, x: x(entry.measuredOn), y: y(entry.weightKg) })),
)

const linePath = computed(() =>
  points.value.map((point, index) => `${index === 0 ? 'M' : 'L'}${point.x},${point.y}`).join(' '),
)

const areaPath = computed(() => {
  if (points.value.length < 2) return ''
  const first = points.value[0]!
  const last = points.value[points.value.length - 1]!
  const baseline = HEIGHT - PADDING.bottom
  return `${linePath.value} L${last.x},${baseline} L${first.x},${baseline} Z`
})

const gridLines = computed(() => {
  const { min, step } = range.value
  return Array.from({ length: GRID_STEPS + 1 }, (_, index) => {
    const value = min + step * index
    return { value, y: y(value) }
  })
})

const dateLabels = computed(() => {
  const labelCount = 3
  return Array.from({ length: labelCount }, (_, index) => {
    const time = startTime.value + ((endTime.value - startTime.value) * index) / (labelCount - 1)
    const date = new Date(time).toISOString().slice(0, 10)
    return { label: formatDate(date), x: x(date) }
  })
})

const activeIndex = ref<number | null>(null)
const active = computed(() => {
  if (points.value.length === 0) return null
  return points.value[activeIndex.value ?? points.value.length - 1] ?? null
})

const svg = ref<SVGSVGElement | null>(null)

function onPointer(event: PointerEvent) {
  if (!svg.value || points.value.length === 0) return
  const box = svg.value.getBoundingClientRect()
  const pointerX = ((event.clientX - box.left) / box.width) * WIDTH
  let nearest = 0
  points.value.forEach((point, index) => {
    if (Math.abs(point.x - pointerX) < Math.abs(points.value[nearest]!.x - pointerX)) {
      nearest = index
    }
  })
  activeIndex.value = nearest
}

const oneDecimal = new Intl.NumberFormat('de-AT', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})
const wholeNumber = new Intl.NumberFormat('de-AT', { maximumFractionDigits: 0 })

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00Z`).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'short',
    timeZone: 'UTC',
  })
}
</script>

<template>
  <figure>
    <p class="h-11" aria-live="polite">
      <template v-if="active">
        <span class="figure text-2xl">{{ oneDecimal.format(active.entry.weightKg) }}</span>
        <span class="text-sm text-muted-foreground">
          kg · {{ formatDate(active.entry.measuredOn) }}</span
        >
      </template>
    </p>

    <svg
      ref="svg"
      :viewBox="`0 0 ${WIDTH} ${HEIGHT}`"
      class="w-full touch-none select-none"
      role="img"
      :aria-label="`Weight over the last ${days} days`"
      @pointerdown="onPointer"
      @pointermove="onPointer"
      @pointerleave="activeIndex = null"
    >
      <defs>
        <linearGradient id="weight-area" x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stop-color="#34d399" stop-opacity="0.25" />
          <stop offset="100%" stop-color="#34d399" stop-opacity="0" />
        </linearGradient>
      </defs>

      <g v-for="line in gridLines" :key="line.value">
        <line
          :x1="PADDING.left"
          :x2="WIDTH - PADDING.right"
          :y1="line.y"
          :y2="line.y"
          class="stroke-white/8"
          stroke-width="1"
        />
        <text
          :x="PADDING.left - 8"
          :y="line.y + 4"
          text-anchor="end"
          class="fill-muted-foreground text-[10px] tabular-nums"
        >
          {{ wholeNumber.format(line.value) }}
        </text>
      </g>

      <text
        v-for="(label, index) in dateLabels"
        :key="label.label"
        :x="label.x"
        :y="HEIGHT - 8"
        :text-anchor="index === 0 ? 'start' : index === dateLabels.length - 1 ? 'end' : 'middle'"
        class="fill-muted-foreground text-[10px]"
      >
        {{ label.label }}
      </text>

      <path v-if="areaPath" :d="areaPath" fill="url(#weight-area)" />
      <path
        v-if="points.length > 1"
        :d="linePath"
        fill="none"
        class="stroke-emerald-400"
        stroke-width="2"
        stroke-linejoin="round"
        stroke-linecap="round"
      />

      <line
        v-if="active"
        :x1="active.x"
        :x2="active.x"
        :y1="PADDING.top"
        :y2="HEIGHT - PADDING.bottom"
        class="stroke-white/25"
        stroke-width="1"
        stroke-dasharray="3 3"
      />
      <circle
        v-if="active"
        :cx="active.x"
        :cy="active.y"
        r="5"
        class="fill-emerald-400 stroke-card"
        stroke-width="2"
      />
    </svg>

    <table class="sr-only">
      <caption>
        Weight entries
      </caption>
      <tr v-for="entry in entries" :key="entry.measuredOn">
        <th scope="row">{{ formatDate(entry.measuredOn) }}</th>
        <td>{{ oneDecimal.format(entry.weightKg) }} kg</td>
      </tr>
    </table>
  </figure>
</template>
