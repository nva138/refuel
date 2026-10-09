package at.maximilianmayer.veggietracker.meal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DaySummaryResponse(
        LocalDate date,
        List<MealResponse> meals,
        BigDecimal totalKcal,
        BigDecimal totalProtein,
        BigDecimal totalCarbs,
        BigDecimal totalFat
) {
}
