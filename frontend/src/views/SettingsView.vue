<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchSettings, updateSettings } from '@/api/settings'
import type { Settings } from '@/types/settings'

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
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

function toNumber(value: string): number {
  return Number(value.replace(',', '.'))
}

onMounted(async () => {
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
  'w-full rounded-xl border border-input bg-background px-3 py-2.5 text-base outline-none focus:border-ring'
</script>

<template>
  <div class="flex items-center gap-3">
    <RouterLink :to="{ name: 'today' }" class="-ml-2 rounded-full px-2 py-1 text-2xl">←</RouterLink>
    <h1 class="text-2xl font-semibold">Settings</h1>
  </div>

  <form v-if="!loading" class="mt-6 space-y-6 pb-28" @submit.prevent="save">
    <section class="space-y-2">
      <h2 class="text-sm font-medium text-muted-foreground">Goal</h2>
      <div class="grid grid-cols-3 gap-2">
        <button
          v-for="mode in modes"
          :key="mode.value"
          type="button"
          :aria-pressed="form.mode === mode.value"
          class="rounded-xl border py-3 font-medium"
          :class="
            form.mode === mode.value
              ? 'border-emerald-400 bg-emerald-400/10 text-emerald-400'
              : 'border-border'
          "
          @click="form.mode = mode.value"
        >
          {{ mode.label }}
        </button>
      </div>
      <p class="text-xs text-muted-foreground">
        {{ modes.find((mode) => mode.value === form.mode)?.hint }}
      </p>
    </section>

    <section class="space-y-3">
      <label class="block">
        <span class="text-sm text-muted-foreground">Body weight (kg)</span>
        <input v-model="form.bodyWeightKg" :class="inputClass" class="mt-1" inputmode="decimal" />
      </label>
      <p v-if="proteinGoalG !== null" class="text-xs text-muted-foreground">
        Protein goal: {{ Math.round(toNumber(form.bodyWeightKg) * 2) || proteinGoalG }} g (2 g per
        kg)
      </p>

      <label class="block">
        <span class="text-sm text-muted-foreground">Weekly average burn (kcal per day)</span>
        <input v-model="form.kcalReference" :class="inputClass" class="mt-1" inputmode="numeric" />
      </label>
      <p class="text-xs text-muted-foreground">
        Take it from the WHOOP app for now. It will come from WHOOP automatically later.
      </p>
    </section>

    <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

    <div
      class="fixed inset-x-0 bottom-0 mx-auto max-w-md bg-background/90 px-4 pt-3 pb-[max(1rem,env(safe-area-inset-bottom))] backdrop-blur"
    >
      <button
        type="submit"
        :disabled="saving"
        class="w-full rounded-2xl bg-emerald-400 py-3.5 font-semibold text-zinc-950 disabled:opacity-50"
      >
        {{ saving ? 'Saving…' : 'Save' }}
      </button>
    </div>
  </form>

  <p v-else-if="error" class="mt-6 text-sm text-destructive">{{ error }}</p>
</template>
