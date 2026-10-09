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

    public static MealPartResponse from(MealPart part) {
        return new MealPartResponse(
                part.getId(),
                part.getName(),
                part.getKcalPer100g(),
                part.getProteinPer100g(),
                part.getCarbsPer100g(),
                part.getFatPer100g(),
                part.getAmountG()
        );
    }

}
