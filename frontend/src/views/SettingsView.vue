<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchSettings, updateSettings } from '@/api/settings'
import { fetchEnergy } from '@/api/whoop'
import type { Settings } from '@/types/settings'
import type { Energy } from '@/types/whoop'

type Mode = Settings['mode']

const modes: { value: Mode; label: string; hint: string }[] = [
  { value: 'CUT', label: 'Cut', hint: 'Stay below your weekly burn' },
  { value: 'MAINTAIN', label: 'Maintain', hint: 'Stay within 5 % of your weekly burn' },
  { value: 'BULK', label: 'Bulk', hint: 'Eat above your weekly burn' },
]

const router = useRouter()

const form = reactive({
  mode: 'MAINTAIN' as Mode,
  bodyWeightKg: '',
  kcalReference: '',
})
const proteinGoalG = ref<number | null>(null)
const energy = ref<Energy | null>(null)
const energyChecked = ref(false)
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

const number = new Intl.NumberFormat('de-AT', { maximumFractionDigits: 0 })

function formatNumber(value: number): string {
  return number.format(value)
}

function toNumber(value: string): number {
  return Number(value.replace(',', '.'))
}

onMounted(async () => {
  fetchEnergy()
    .then((currentEnergy) => (energy.value = currentEnergy))
    .catch(() => (energy.value = null))
    .finally(() => (energyChecked.value = true))

  try {
    const settings = await fetchSettings()
    form.mode = settings.mode
    form.bodyWeightKg = String(settings.bodyWeightKg).replace('.', ',')
    form.kcalReference = String(settings.kcalReference)
    proteinGoalG.value = settings.proteinGoalG
  } catch {
    error.value = 'Settings could not be loaded. Is the backend running?'
  } finally {
    loading.value = false
  }
})

async function save() {
  const bodyWeightKg = toNumber(form.bodyWeightKg)
  const kcalReference = Math.round(toNumber(form.kcalReference))
  if (!(bodyWeightKg > 0) || !(kcalReference > 0)) {
    error.value = 'Please enter your weight and your weekly burn.'
    return
  }

  saving.value = true
  error.value = null
  try {
    await updateSettings({ mode: form.mode, bodyWeightKg, kcalReference })
    await router.push({ name: 'today' })
  } catch {
    error.value = 'Saving failed. Is the backend running?'
  } finally {
    saving.value = false
  }
}

const inputClass =
  'figure w-full rounded-xl bg-white/6 px-4 py-3 text-xl outline-none ring-1 ring-white/10 focus:ring-white/40'
</script>

<template>
  <PageHeader title="Settings" />

  <form v-if="!loading" class="mt-8 space-y-3 pb-32" @submit.prevent="save">
    <section class="rounded-2xl bg-card p-4">
      <h2 class="caps text-xs text-muted-foreground">Goal</h2>
      <div class="mt-3 grid grid-cols-3 gap-1 rounded-xl bg-black/25 p-1">
        <button
          v-for="mode in modes"
          :key="mode.value"
          type="button"
          :aria-pressed="form.mode === mode.value"
          class="caps rounded-lg py-2.5 text-sm transition-colors"
          :class="
            form.mode === mode.value ? 'bg-white/12 text-foreground' : 'text-muted-foreground'
          "
          @click="form.mode = mode.value"
        >
          {{ mode.label }}
        </button>
      </div>
      <p class="mt-3 text-sm text-muted-foreground">
        {{ modes.find((mode) => mode.value === form.mode)?.hint }}
      </p>
    </section>

    <section class="rounded-2xl bg-card p-4">
      <label class="block">
        <span class="caps text-xs text-muted-foreground">Body weight (kg)</span>
        <input v-model="form.bodyWeightKg" :class="inputClass" class="mt-3" inputmode="decimal" />
      </label>
      <p v-if="proteinGoalG !== null" class="mt-3 text-sm text-muted-foreground">
        Protein goal
        <span class="figure text-foreground"
          >{{ Math.round(toNumber(form.bodyWeightKg) * 2) || proteinGoalG }} g</span
        >
        · 2 g per kg
      </p>
    </section>

    <section v-if="energyChecked" class="rounded-2xl bg-card p-4">
      <h2 class="caps text-xs text-muted-foreground">WHOOP</h2>

      <div v-if="energy" class="mt-3 flex items-center gap-4">
        <span
          class="flex size-11 shrink-0 items-center justify-center rounded-xl bg-emerald-400/15 text-emerald-400"
        >
          <Check class="size-5" />
        </span>
        <div>
          <p class="caps text-sm text-emerald-400">Connected</p>
          <p class="mt-0.5 text-sm text-muted-foreground">
            Ø
            <span class="figure text-foreground"
              >{{ formatNumber(energy.weeklyAverageKcal) }} kcal</span
            >
            per day, last 7 days
          </p>
        </div>
      </div>

      <template v-else>
        <a
          href="/api/whoop/login"
          class="caps mt-3 block w-full rounded-xl bg-white/8 py-3.5 text-center text-sm"
        >
          Connect WHOOP
        </a>

        <label class="mt-4 block">
          <span class="text-sm text-muted-foreground"
            >Or enter your average burn (kcal per day)</span
          >
          <input
            v-model="form.kcalReference"
            :class="inputClass"
            class="mt-2"
            inputmode="numeric"
          />
        </label>
      </template>
    </section>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div
      class="fixed inset-x-0 bottom-0 mx-auto max-w-md bg-gradient-to-t from-background via-background/80 to-transparent px-4 pt-10 pb-[max(1.25rem,env(safe-area-inset-bottom))]"
    >
      <button
        type="submit"
        :disabled="saving"
        class="caps w-full rounded-full bg-white py-4 text-sm text-zinc-950 disabled:opacity-50"
      >
        {{ saving ? 'Saving…' : 'Save' }}
      </button>
    </div>
  </form>

  <p v-else-if="error" class="mt-6 text-sm text-destructive">{{ error }}</p>
</template>
