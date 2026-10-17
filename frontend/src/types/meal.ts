export interface MealPart {
  id: number
  name: string
  kcalPer100g: number
  proteinPer100g: number
  carbsPer100g: number
  fatPer100g: number
  amountG: number
}

export interface Meal {
  id: number
  eatenAt: string
  name: string
  enteredText: string | null
  veggieHints: string[]
  parts: MealPart[]
  favourite: boolean
}

export interface DaySummary {
  date: string
  meals: Meal[]
  totalKcal: number
  totalProtein: number
  totalCarbs: number
  totalFat: number
}

export interface CreateMealPartRequest {
  name: string
  kcalPer100g: number
  proteinPer100g: number
  carbsPer100g: number
  fatPer100g: number
  amountG: number
}

export interface CreateMealRequest {
  name: string
  enteredText: string | null
  veggieHints: string[]
  parts: CreateMealPartRequest[]
}

export interface SuggestMealRequest {
  text: string
}

export interface SuggestMealResponse {
  name: string
  parts: CreateMealPartRequest[]
  veggieHints: string[]
}

export interface ReadLabelResponse {
  part: CreateMealPartRequest
  veggieHints: string[]
}
