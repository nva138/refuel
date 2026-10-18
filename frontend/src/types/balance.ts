export interface DayBalance {
  date: string
  logged: boolean
  eatenKcal: number
  burnedKcal: number
  balanceKcal: number
}

export interface WeekBalance {
  days: DayBalance[]
  balanceKcal: number
  loggedDays: number
}
