package at.maximilianmayer.veggietracker.meal;

import java.util.List;

public record CreateMealRequest(String name,
                                String enteredText,
                                List<String> veggieHints,
                                List<CreateMealPartRequest> parts) {
}
