package at.maximilianmayer.veggietracker.settings;

import java.math.BigDecimal;

public record UpdateSettingsRequest (GoalMode mode,
                                     BigDecimal bodyWeightKg,
                                     Integer kcalReference){
}
