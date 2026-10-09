package at.maximilianmayer.veggietracker.meal;

import java.time.Instant;
import java.util.List;

public record MealResponse(Long id,
                           Instant eatenAt,
                           String name,
                           boolean isVegetarian,
                           String enteredText,
                           List<MealPartResponse> parts) {
}
