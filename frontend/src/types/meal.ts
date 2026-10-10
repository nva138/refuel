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
  isVegetarian: boolean
  enteredText: string | null
  parts: MealPart[]
}

export interface DaySummary {
  date: string
  meals: Meal[]
  totalKcal: number
  totalProtein: number
  totalCarbs: number
  totalFat: number
}
