<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Minus, TrendingDown, TrendingUp } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import WeightChart from '@/components/WeightChart.vue'
import { fetchWeightHistory, logWeight } from '@/api/weight'
import type { Weight } from '@/types/weight'

const ranges = [
  { label: 'W', days: 7 },
  { label: 'M', days: 30 },
  { label: '3M', days: 90 },
]

const history = ref<Weight[]>([])
const days = ref(30)
const input = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

async function load() {
  history.value = await fetchWeightHistory()
}

onMounted(async () => {
  try {
    await load()
  } catch {
    error.value = 'Weight history could not be loaded. Is the backend running?'
  } finally {
    loading.value = false
  }
})

const visible = computed(() => {
  const from = new Date()
  from.setDate(from.getDate() - (days.value - 1))
  const fromDate = from.toLocaleDateString('sv-SE')
  return history.value.filter((entry) => entry.measuredOn >= fromDate)
})

const latest = computed(() => history.value[history.value.length - 1] ?? null)

const change = computed(() => {
  if (visible.value.length < 2) return null
  const first = visible.value[0]!
  const last = visible.value[visible.value.length - 1]!
  return last.weightKg - first.weightKg
})

const oneDecimal = new Intl.NumberFormat('de-AT', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
})

const rangeLabel = computed(() => {
  if (days.value === 7) return 'this week'
  if (days.value === 30) return 'in 30 days'
  return 'in 3 months'
})

async function save() {
  const weightKg = Number(input.value.replace(',', '.'))
  if (!(weightKg > 0)) {
    error.value = 'Please enter your weight in kg.'
    return
  }

  saving.value = true
  error.value = null
  try {
    await logWeight({ weightKg })
    input.value = ''
    await load()
  } catch {
    error.value = 'Saving failed. Is the backend running?'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="Weight" />

  <template v-if="!loading">
    <section class="mt-8 flex items-end justify-between gap-4">
      <div>
        <p class="caps text-xs text-muted-foreground">Current</p>
        <p class="figure mt-1 text-5xl">
          {{ latest ? oneDecimal.format(latest.weightKg) : '–' }}
          <span class="text-lg font-medium text-muted-foreground">kg</span>
        </p>
      </div>
      <p
        v-if="change !== null"
        class="mb-1 flex items-center gap-1.5 rounded-lg bg-white/8 px-2.5 py-1.5 text-sm"
      >
        <TrendingDown v-if="change < 0" class="size-4" />
        <TrendingUp v-else-if="change > 0" class="size-4" />
        <Minus v-else class="size-4" />
        <span class="figure">{{ change > 0 ? '+' : '' }}{{ oneDecimal.format(change) }} kg</span>
        <span class="text-muted-foreground">{{ rangeLabel }}</span>
      </p>
    </section>

    <section class="mt-6 rounded-2xl bg-card p-4">
      <div class="grid grid-cols-3 gap-1 rounded-xl bg-black/25 p-1">
        <button
          v-for="range in ranges"
          :key="range.days"
          type="button"
          :aria-pressed="days === range.days"
          class="caps rounded-lg py-2 text-sm transition-colors"
          :class="days === range.days ? 'bg-white/12 text-foreground' : 'text-muted-foreground'"
          @click="days = range.days"
        >
          {{ range.label }}
        </button>
      </div>

      <WeightChart class="mt-4" :entries="visible" :days="days" />

      <p v-if="visible.length < 2" class="mt-2 text-center text-sm text-muted-foreground">
        Weigh yourself a few mornings in a row to see your trend.
      </p>
    </section>

    <form class="mt-3 rounded-2xl bg-card p-4" @submit.prevent="save">
      <label class="block">
        <span class="caps text-xs text-muted-foreground">Today's weight (kg)</span>
        <div class="mt-3 flex gap-2">
          <input
            v-model="input"
            inputmode="decimal"
            :placeholder="latest ? oneDecimal.format(latest.weightKg) : '95,0'"
            class="figure w-full rounded-xl bg-white/6 px-4 py-3 text-xl outline-none ring-1 ring-white/10 placeholder:text-muted-foreground/50 focus:ring-white/40"
          />
          <button
            type="submit"
            :disabled="saving"
            class="caps shrink-0 rounded-xl bg-white px-6 text-sm text-zinc-950 disabled:opacity-50"
          >
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </label>
      <p class="mt-3 text-sm text-muted-foreground">
        Your protein goal follows your latest weight. Saving again today replaces today's value.
      </p>
    </form>
  </template>

  <p v-if="error" class="mt-6 text-sm text-destructive">{{ error }}</p>
</template>
