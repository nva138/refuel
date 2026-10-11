<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { createMeal, suggestMeal } from '@/api/meals'
import type { CreateMealPartRequest, CreateMealRequest } from '@/types/meal'

interface PartForm {
  name: string
  kcalPer100g: string
  proteinPer100g: string
  carbsPer100g: string
  fatPer100g: string
  amountG: string
}

const router = useRouter()

const form = reactive({
  name: '',
  enteredText: '',
  parts: [emptyPart()],
})
const saving = ref(false)
const suggesting = ref(false)
const veggieHints = ref<string[]>([])
const error = ref<string | null>(null)

function toFormValue(value: number): string {
  return String(value).replace('.', ',')
}

function toPartForm(part: CreateMealPartRequest): PartForm {
  return {
    name: part.name,
    kcalPer100g: toFormValue(part.kcalPer100g),
    proteinPer100g: toFormValue(part.proteinPer100g),
    carbsPer100g: toFormValue(part.carbsPer100g),
    fatPer100g: toFormValue(part.fatPer100g),
    amountG: toFormValue(part.amountG),
  }
}

async function suggest() {
  const text = form.enteredText.trim()
  if (text === '') {
    error.value = 'Describe what you ate first.'
    return
  }

  suggesting.value = true
  error.value = null
  try {
    const suggestion = await suggestMeal({ text })
    form.name = suggestion.name
    form.parts = suggestion.parts.map(toPartForm)
    veggieHints.value = suggestion.veggieHints
  } catch {
    error.value = 'Suggestion failed. Is the backend running?'
  } finally {
    suggesting.value = false
  }
}

function emptyPart(): PartForm {
  return {
    name: '',
    kcalPer100g: '',
    proteinPer100g: '',
    carbsPer100g: '',
    fatPer100g: '',
    amountG: '',
  }
}

function addPart() {
  form.parts.push(emptyPart())
}

function removePart(index: number) {
  form.parts.splice(index, 1)
}

function toNumber(value: string): number {
  return Number(value.replace(',', '.'))
}

function isValid(): boolean {
  if (form.name.trim() === '' || form.parts.length === 0) return false
  return form.parts.every(
    (part) =>
      part.name.trim() !== '' &&
      [
        part.kcalPer100g,
        part.proteinPer100g,
        part.carbsPer100g,
        part.fatPer100g,
        part.amountG,
      ].every((value) => value.trim() !== '' && !Number.isNaN(toNumber(value))),
  )
}

async function save() {
  if (!isValid()) {
    error.value = 'Please fill in a name and all values for every part.'
    return
  }

  const request: CreateMealRequest = {
    name: form.name.trim(),
    veggieHints: veggieHints.value,
    enteredText: form.enteredText.trim() === '' ? null : form.enteredText.trim(),
    parts: form.parts.map((part) => ({
      name: part.name.trim(),
      kcalPer100g: toNumber(part.kcalPer100g),
      proteinPer100g: toNumber(part.proteinPer100g),
      carbsPer100g: toNumber(part.carbsPer100g),
      fatPer100g: toNumber(part.fatPer100g),
      amountG: toNumber(part.amountG),
    })),
  }

  saving.value = true
  error.value = null
  try {
    await createMeal(request)
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
    <h1 class="text-2xl font-semibold">Add meal</h1>
  </div>

  <form class="mt-6 space-y-6 pb-28" @submit.prevent="save">
    <section class="space-y-3 rounded-2xl bg-card p-4">
      <label class="block">
        <span class="text-sm text-muted-foreground">What did you eat?</span>
        <textarea
          v-model="form.enteredText"
          :class="inputClass"
          class="mt-1"
          rows="3"
          placeholder="2 eggs and a slice of wholegrain bread with butter"
        />
      </label>
      <button
        type="button"
        :disabled="suggesting"
        class="w-full rounded-xl border border-emerald-400/60 py-2.5 font-medium text-emerald-400 disabled:opacity-50"
        @click="suggest"
      >
        {{ suggesting ? 'Thinking…' : 'Suggest' }}
      </button>
    </section>

    <section
      v-if="veggieHints.length > 0"
      class="space-y-1 rounded-2xl border border-amber-400/40 bg-amber-400/10 p-4 text-sm text-amber-200"
    >
      <p class="font-medium">Might not be vegetarian</p>
      <p v-for="hint in veggieHints" :key="hint">{{ hint }}</p>
    </section>

    <section class="space-y-3">
      <label class="block">
        <span class="text-sm text-muted-foreground">Name</span>
        <input v-model="form.name" :class="inputClass" class="mt-1" placeholder="Lunch" />
      </label>
    </section>

    <section class="space-y-3">
      <h2 class="text-sm font-medium text-muted-foreground">Parts</h2>

      <div
        v-for="(part, index) in form.parts"
        :key="index"
        class="space-y-3 rounded-2xl bg-card p-4"
      >
        <div class="flex gap-3">
          <input v-model="part.name" :class="inputClass" placeholder="Rice" />
          <button
            v-if="form.parts.length > 1"
            type="button"
            class="shrink-0 rounded-xl px-3 text-muted-foreground"
            @click="removePart(index)"
          >
            ✕
          </button>
        </div>

        <label class="block">
          <span class="text-xs text-muted-foreground">Amount (g)</span>
          <input
            v-model="part.amountG"
            :class="inputClass"
            class="mt-1"
            inputmode="decimal"
            placeholder="200"
          />
        </label>

        <p class="text-xs text-muted-foreground">Per 100 g</p>
        <div class="grid grid-cols-2 gap-3">
          <label>
            <span class="text-xs text-muted-foreground">kcal</span>
            <input
              v-model="part.kcalPer100g"
              :class="inputClass"
              class="mt-1"
              inputmode="decimal"
            />
          </label>
          <label>
            <span class="text-xs text-muted-foreground">Protein (g)</span>
            <input
              v-model="part.proteinPer100g"
              :class="inputClass"
              class="mt-1"
              inputmode="decimal"
            />
          </label>
          <label>
            <span class="text-xs text-muted-foreground">Carbs (g)</span>
            <input
              v-model="part.carbsPer100g"
              :class="inputClass"
              class="mt-1"
              inputmode="decimal"
            />
          </label>
          <label>
            <span class="text-xs text-muted-foreground">Fat (g)</span>
            <input v-model="part.fatPer100g" :class="inputClass" class="mt-1" inputmode="decimal" />
          </label>
        </div>
      </div>

      <button
        type="button"
        class="w-full rounded-2xl border border-dashed border-border py-3 text-sm text-muted-foreground"
        @click="addPart"
      >
        + Add part
      </button>
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
</template>
