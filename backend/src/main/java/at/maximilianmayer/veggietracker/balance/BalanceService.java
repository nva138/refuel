package at.maximilianmayer.veggietracker.balance;

import at.maximilianmayer.veggietracker.meal.DaySummaryResponse;
import at.maximilianmayer.veggietracker.meal.MealService;
import at.maximilianmayer.veggietracker.settings.SettingsRepository;
import at.maximilianmayer.veggietracker.whoop.WhoopNotConnectedException;
import at.maximilianmayer.veggietracker.whoop.WhoopService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BalanceService {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");
    private static final int DAYS = 7;

    private final MealService mealService;
    private final WhoopService whoopService;
    private final SettingsRepository settingsRepository;

    public BalanceService(MealService mealService, WhoopService whoopService, SettingsRepository settingsRepository) {
        this.mealService = mealService;
        this.whoopService = whoopService;
        this.settingsRepository = settingsRepository;
    }

    public WeekBalanceResponse getWeek(LocalDate until) {
        LocalDate yesterday = LocalDate.now(VIENNA).minusDays(1);
        LocalDate lastDay = until == null || until.isAfter(yesterday) ? yesterday : until;

        List<LocalDate> days = new ArrayList<>();
        for (int offset = DAYS - 1; offset >= 0; offset--) {
            days.add(lastDay.minusDays(offset));
        }

        Map<LocalDate, Integer> burnByDay = loadBurn(days);
        int fallbackBurn = settingsRepository.findAll().getFirst().getKcalReference();

        List<DayBalanceResponse> dayBalances = new ArrayList<>();
        int balanceKcal = 0;
        int loggedDays = 0;

        for (LocalDate day : days) {
            DaySummaryResponse summary = mealService.getDay(day);
            boolean logged = !summary.meals().isEmpty();
            int eatenKcal = summary.totalKcal().intValue();
            int burnedKcal = burnByDay.getOrDefault(day, fallbackBurn);
            int dayBalance = eatenKcal - burnedKcal;

            if (logged) {
                balanceKcal += dayBalance;
                loggedDays++;
            }
            dayBalances.add(new DayBalanceResponse(day, logged, eatenKcal, burnedKcal, dayBalance));
        }

        return new WeekBalanceResponse(dayBalances, balanceKcal, loggedDays);
    }

    private Map<LocalDate, Integer> loadBurn(List<LocalDate> days) {
        try {
            return whoopService.getBurnByDay(days);
        } catch (WhoopNotConnectedException | RestClientException e) {
            return Map.of();
        }
    }
}
