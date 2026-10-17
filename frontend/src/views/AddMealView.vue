<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, Plus, ScanLine, Sparkles, Star, TriangleAlert, X } from '@lucide/vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  createMeal,
  fetchFavourites,
  readLabel,
  suggestMeal,
  suggestMealFromPhoto,
} from '@/api/meals'
import type {
  CreateMealPartRequest,
  CreateMealRequest,
  Meal,
  SuggestMealResponse,
} from '@/types/meal'

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

function fillFromMeal(meal: Meal) {
  form.name = meal.name
  form.enteredText = meal.enteredText ?? ''
  form.parts = meal.parts.map(toPartForm)
  veggieHints.value = meal.veggieHints
  error.value = null
}

const repeatMeal = (history.state as { repeatMeal?: Meal } | null)?.repeatMeal
if (repeatMeal) {
  fillFromMeal(repeatMeal)
}

const favourites = ref<Meal[]>([])

onMounted(async () => {
  try {
    const all = await fetchFavourites()
    favourites.value = all.filter(
      (meal, index) => all.findIndex((other) => other.name === meal.name) === index,
    )
  } catch {
    favourites.value = []
  }
})

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
  'w-full rounded-xl bg-white/6 px-3.5 py-3 text-base outline-none ring-1 ring-white/10 placeholder:text-muted-foreground/60 focus:ring-white/40'
const numberInputClass = `${inputClass} figure text-lg`
</script>

<template>
  <PageHeader title="Add meal" />

  <form class="mt-8 space-y-3 pb-32" @submit.prevent="save">
    <section v-if="favourites.length > 0" class="rounded-2xl bg-card p-4">
      <h2 class="caps flex items-center gap-2 text-xs text-muted-foreground">
        <Star class="size-4 text-amber-300" fill="currentColor" />
        Favourites
      </h2>
      <ul class="mt-3 flex flex-wrap gap-2">
        <li v-for="meal in favourites" :key="meal.id">
          <button
            type="button"
            class="rounded-full bg-white/8 px-3.5 py-2 text-sm"
            @click="fillFromMeal(meal)"
          >
            {{ meal.name }}
          </button>
        </li>
      </ul>
    </section>

    <section class="space-y-3 rounded-2xl bg-card p-4">
      <label class="block">
        <span class="caps text-xs text-muted-foreground">What did you eat?</span>
        <textarea
          v-model="form.enteredText"
          :class="inputClass"
          class="mt-3 resize-none"
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
          class="absolute top-2 right-2 flex size-9 items-center justify-center rounded-full bg-black/60 backdrop-blur"
          @click="removePhoto"
        >
          <X class="size-4" />
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
          class="caps flex items-center justify-center gap-2 rounded-xl bg-white/8 py-3.5 text-sm"
          @click="photoInput?.click()"
        >
          <Camera class="size-5" />
          {{ photo ? 'Change' : 'Photo' }}
        </button>
        <button
          type="button"
          :disabled="suggesting"
          class="caps flex items-center justify-center gap-2 rounded-xl bg-emerald-400/15 py-3.5 text-sm text-emerald-400 disabled:opacity-50"
          @click="suggest"
        >
          <Sparkles class="size-5" :class="{ 'animate-pulse': suggesting }" />
          {{ suggesting ? 'Thinking…' : 'Suggest' }}
        </button>
      </div>
    </section>

    <section
      v-if="veggieHints.length > 0"
      class="space-y-1.5 rounded-2xl bg-amber-400/10 p-4 text-sm text-amber-200 ring-1 ring-amber-400/30"
    >
      <p class="caps flex items-center gap-2 text-xs text-amber-300">
        <TriangleAlert class="size-4" />
        Might not be vegetarian
      </p>
      <p v-for="hint in veggieHints" :key="hint">{{ hint }}</p>
    </section>

    <section class="rounded-2xl bg-card p-4">
      <p class="caps text-xs text-muted-foreground">Total</p>
      <p class="figure mt-1 text-5xl">
        {{ wholeNumber.format(totals.kcal) }}
        <span class="text-lg font-medium text-muted-foreground">kcal</span>
      </p>
      <div class="mt-4 grid grid-cols-3 gap-2 border-t border-white/8 pt-3">
        <div>
          <p class="figure text-xl">{{ wholeNumber.format(totals.protein) }} g</p>
          <p class="caps mt-0.5 text-[11px] text-muted-foreground">Protein</p>
        </div>
        <div>
          <p class="figure text-xl">{{ wholeNumber.format(totals.carbs) }} g</p>
          <p class="caps mt-0.5 text-[11px] text-muted-foreground">Carbs</p>
        </div>
        <div>
          <p class="figure text-xl">{{ wholeNumber.format(totals.fat) }} g</p>
          <p class="caps mt-0.5 text-[11px] text-muted-foreground">Fat</p>
        </div>
      </div>
    </section>

    <section class="rounded-2xl bg-card p-4">
      <label class="block">
        <span class="caps text-xs text-muted-foreground">Name</span>
        <input v-model="form.name" :class="inputClass" class="mt-3" placeholder="Lunch" />
      </label>
    </section>

    <section class="space-y-3 pt-5">
      <h2 class="text-2xl font-semibold">Parts</h2>

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
        class="space-y-4 rounded-2xl bg-card p-4"
      >
        <div class="flex gap-2">
          <input v-model="part.name" :class="inputClass" placeholder="Rice" />
          <button
            type="button"
            :aria-label="`Read nutrition label for part ${index + 1}`"
            :disabled="readingIndex !== null"
            class="flex w-12 shrink-0 items-center justify-center rounded-xl bg-white/8 disabled:opacity-50"
            @click="chooseLabelPhoto(index)"
          >
            <ScanLine class="size-5" :class="{ 'animate-pulse': readingIndex === index }" />
          </button>
          <button
            v-if="form.parts.length > 1"
            type="button"
            :aria-label="`Remove part ${index + 1}`"
            class="flex w-10 shrink-0 items-center justify-center rounded-xl text-muted-foreground"
            @click="removePart(index)"
          >
            <X class="size-5" />
          </button>
        </div>

        <label class="block">
          <span class="caps text-[11px] text-muted-foreground">Amount (g)</span>
          <input
            v-model="part.amountG"
            :class="numberInputClass"
            class="mt-2"
            inputmode="decimal"
            placeholder="200"
          />
        </label>

        <div>
          <p class="caps text-[11px] text-muted-foreground">Per 100 g</p>
          <div class="mt-2 grid grid-cols-2 gap-3">
            <label>
              <span class="text-xs text-muted-foreground">kcal</span>
              <input
                v-model="part.kcalPer100g"
                :class="numberInputClass"
                class="mt-1"
                inputmode="decimal"
              />
            </label>
            <label>
              <span class="text-xs text-muted-foreground">Protein (g)</span>
              <input
                v-model="part.proteinPer100g"
                :class="numberInputClass"
                class="mt-1"
                inputmode="decimal"
              />
            </label>
            <label>
              <span class="text-xs text-muted-foreground">Carbs (g)</span>
              <input
                v-model="part.carbsPer100g"
                :class="numberInputClass"
                class="mt-1"
                inputmode="decimal"
              />
            </label>
            <label>
              <span class="text-xs text-muted-foreground">Fat (g)</span>
              <input
                v-model="part.fatPer100g"
                :class="numberInputClass"
                class="mt-1"
                inputmode="decimal"
              />
            </label>
          </div>
        </div>
      </div>

      <button
        type="button"
        class="caps flex w-full items-center justify-center gap-2 rounded-2xl border border-dashed border-white/15 py-3.5 text-sm text-muted-foreground"
        @click="addPart"
      >
        <Plus class="size-4" />
        Add part
      </button>
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
        {{ saving ? 'Saving…' : 'Save meal' }}
      </button>
    </div>
  </form>
</template>
