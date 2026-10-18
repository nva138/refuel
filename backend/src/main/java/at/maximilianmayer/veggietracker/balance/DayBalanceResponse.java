package at.maximilianmayer.veggietracker.balance;

import java.time.LocalDate;

public record DayBalanceResponse(LocalDate date, boolean logged, int eatenKcal, int burnedKcal, int balanceKcal) {
}
