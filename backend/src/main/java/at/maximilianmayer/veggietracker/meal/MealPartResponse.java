package at.maximilianmayer.veggietracker.meal;

import java.math.BigDecimal;

public record MealPartResponse(Long id,
                               String name,
                               BigDecimal kcalPer100g,
                               BigDecimal proteinPer100g,
                               BigDecimal carbsPer100g,
                               BigDecimal fatPer100g,
                               BigDecimal amountG
                               ) {
}
