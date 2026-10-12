package at.maximilianmayer.veggietracker.settings;

import java.math.BigDecimal;

public record SettingsResponse(GoalMode mode,
                               BigDecimal bodyWeightKg,
                               Integer kcalReference,
                               BigDecimal proteinGoalG) {

    public static SettingsResponse from(Settings settings) {
        return new SettingsResponse(
                settings.getMode(),
                settings.getBodyWeightKg(),
                settings.getKcalReference(),
                settings.getBodyWeightKg().multiply(BigDecimal.valueOf(2))
        );
    }
}
