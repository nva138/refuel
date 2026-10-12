<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { createMeal, readLabel, suggestMeal, suggestMealFromPhoto } from '@/api/meals'
import type { CreateMealPartRequest, CreateMealRequest, SuggestMealResponse } from '@/types/meal'

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

const MAX_PHOTO_SIZE = 1600

const photoInput = ref<HTMLInputElement | null>(null)
const photo = ref<Blob | null>(null)
const photoPreview = ref<string | null>(null)

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve(image)
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('Could not read the image'))
    }
    image.src = url
  })
}

async function toResizedJpeg(file: File): Promise<Blob> {
  const image = await loadImage(file)
  const scale = Math.min(1, MAX_PHOTO_SIZE / Math.max(image.width, image.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.round(image.width * scale)
  canvas.height = Math.round(image.height * scale)
  canvas.getContext('2d')?.drawImage(image, 0, 0, canvas.width, canvas.height)

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Could not convert the image'))),
      'image/jpeg',
      0.85,
    )
  })
}

async function onPhotoSelected(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  error.value = null
  try {
    removePhoto()
    photo.value = await toResizedJpeg(file)
    photoPreview.value = URL.createObjectURL(photo.value)
  } catch {
    error.value = 'This photo could not be read. Try another one.'
  }
}

function removePhoto() {
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value)
  photo.value = null
  photoPreview.value = null
  if (photoInput.value) photoInput.value.value = ''
}

function applySuggestion(suggestion: SuggestMealResponse) {
  form.name = suggestion.name
  form.parts = suggestion.parts.map(toPartForm)
  veggieHints.value = suggestion.veggieHints
}

const labelInput = ref<HTMLInputElement | null>(null)
const labelTargetIndex = ref<number | null>(null)
const readingIndex = ref<number | null>(null)

function chooseLabelPhoto(index: number) {
  labelTargetIndex.value = index
  labelInput.value?.click()
}

async function onLabelSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  const index = labelTargetIndex.value
  input.value = ''
  if (!file || index === null) return

  readingIndex.value = index
  error.value = null
  try {
    const result = await readLabel(await toResizedJpeg(file))
    form.parts[index] = toPartForm(result.part)
    const newHints = result.veggieHints.filter((hint) => !veggieHints.value.includes(hint))
    veggieHints.value = [...veggieHints.value, ...newHints]
  } catch {
    error.value = 'The label could not be read. Try a sharper photo.'
  } finally {
    readingIndex.value = null
    labelTargetIndex.value = null
  }
}

async function suggest() {
  const text = form.enteredText.trim()
  if (text === '' && !photo.value) {
    error.value = 'Describe what you ate or add a photo first.'
    return
  }

  suggesting.value = true
  error.value = null
  try {
    const suggestion = photo.value
      ? await suggestMealFromPhoto(photo.value, text)
      : await suggestMeal({ text })
    applySuggestion(suggestion)
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

function partValue(perHundredGrams: string, amountG: string): number {
  const value = (toNumber(perHundredGrams) * toNumber(amountG)) / 100
  return Number.isFinite(value) ? value : 0
}

const totals = computed(() =>
  form.parts.reduce(
    (sum, part) => ({
      kcal: sum.kcal + partValue(part.kcalPer100g, part.amountG),
      protein: sum.protein + partValue(part.proteinPer100g, part.amountG),
      carbs: sum.carbs + partValue(part.carbsPer100g, part.amountG),
      fat: sum.fat + partValue(part.fatPer100g, part.amountG),
    }),
    { kcal: 0, protein: 0, carbs: 0, fat: 0 },
  ),
)

const wholeNumber = new Intl.NumberFormat('de-AT', { maximumFractionDigits: 0 })

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

      <div v-if="photoPreview" class="relative">
        <img
          :src="photoPreview"
          alt="Selected meal photo"
          class="max-h-56 w-full rounded-xl object-cover"
        />
        <button
          type="button"
          aria-label="Remove photo"
          class="absolute top-2 right-2 flex size-8 items-center justify-center rounded-full bg-background/80 text-sm"
          @click="removePhoto"
        >
          ✕
        </button>
      </div>

      <input
        ref="photoInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="onPhotoSelected"
      />

      <div class="grid grid-cols-2 gap-3">
        <button
          type="button"
          class="rounded-xl border border-border py-2.5 font-medium"
          @click="photoInput?.click()"
        >
          {{ photo ? 'Change photo' : '📷 Photo' }}
        </button>
        <button
          type="button"
          :disabled="suggesting"
          class="rounded-xl border border-emerald-400/60 py-2.5 font-medium text-emerald-400 disabled:opacity-50"
          @click="suggest"
        >
          {{ suggesting ? 'Thinking…' : 'Suggest' }}
        </button>
      </div>
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

    <section class="rounded-2xl bg-card p-4">
      <div class="flex items-baseline justify-between">
        <span class="text-sm text-muted-foreground">Total</span>
        <span class="text-3xl font-bold tabular-nums">
          {{ wholeNumber.format(totals.kcal) }}
          <span class="text-sm font-normal text-muted-foreground">kcal</span>
        </span>
      </div>
      <div class="mt-3 grid grid-cols-3 gap-2 text-center">
        <div>
          <p class="font-semibold tabular-nums">{{ wholeNumber.format(totals.protein) }} g</p>
          <p class="text-xs text-muted-foreground">Protein</p>
        </div>
        <div>
          <p class="font-semibold tabular-nums">{{ wholeNumber.format(totals.carbs) }} g</p>
          <p class="text-xs text-muted-foreground">Carbs</p>
        </div>
        <div>
          <p class="font-semibold tabular-nums">{{ wholeNumber.format(totals.fat) }} g</p>
          <p class="text-xs text-muted-foreground">Fat</p>
        </div>
      </div>
    </section>

    <section class="space-y-3">
      <h2 class="text-sm font-medium text-muted-foreground">Parts</h2>

      <input
        ref="labelInput"
        type="file"
        accept="image/*"
        class="hidden"
        @change="onLabelSelected"
      />

      <div
        v-for="(part, index) in form.parts"
        :key="index"
        class="space-y-3 rounded-2xl bg-card p-4"
      >
        <div class="flex gap-3">
          <input v-model="part.name" :class="inputClass" placeholder="Rice" />
          <button
            type="button"
            :aria-label="`Read nutrition label for part ${index + 1}`"
            :disabled="readingIndex !== null"
            class="shrink-0 rounded-xl border border-border px-3 disabled:opacity-50"
            @click="chooseLabelPhoto(index)"
          >
            {{ readingIndex === index ? '…' : '📷' }}
          </button>
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
