package at.maximilianmayer.veggietracker.meal;

import java.util.List;

public record ReadLabelResponse(CreateMealPartRequest part,
                                List<String> veggieHints) {
}
