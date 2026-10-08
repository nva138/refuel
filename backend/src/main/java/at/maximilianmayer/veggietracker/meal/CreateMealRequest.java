package at.maximilianmayer.veggietracker.meal;

import java.util.List;

public record CreateMealRequest(String name,
                                boolean isVegetarian,
                                String enteredText,
                                List<CreateMealPartRequest> parts) {
}
