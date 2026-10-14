package at.maximilianmayer.veggietracker.weight;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightEntryResponse(LocalDate measuredOn, BigDecimal weightKg) {
    public static WeightEntryResponse from(WeightEntry entry) {
        return new WeightEntryResponse(entry.getMeasuredOn(), entry.getWeightKg());
    }
}
