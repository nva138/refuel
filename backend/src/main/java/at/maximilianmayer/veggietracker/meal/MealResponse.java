package at.maximilianmayer.veggietracker.meal;

import java.time.Instant;
import java.util.List;

public record MealResponse(Long id,
                           Instant eatenAt,
                           String name,
                           boolean isVegetarian,
                           String enteredText,
                           List<MealPartResponse> parts) {

    public static MealResponse from(Meal meal) {

        List<MealPartResponse> partResponses = meal.getParts()
                .stream()
                .map(MealPartResponse::from)
                .toList();

        return new MealResponse(
                meal.getId(),
                meal.getEatenAt(),
                meal.getName(),
                meal.isVegetarian(),
                meal.getEnteredText(),
                partResponses
        );
    }
}
