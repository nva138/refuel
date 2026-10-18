package at.maximilianmayer.veggietracker.balance;

import java.util.List;

public record WeekBalanceResponse(List<DayBalanceResponse> days, int balanceKcal, int loggedDays) {
}
