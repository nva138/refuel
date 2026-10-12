export interface Settings {
  mode: 'CUT' | 'MAINTAIN' | 'BULK'
  bodyWeightKg: number
  kcalReference: number
  proteinGoalG: number
}

export interface UpdateSettingsRequest {
  mode: 'CUT' | 'MAINTAIN' | 'BULK'
  bodyWeightKg: number
  kcalReference: number
}
