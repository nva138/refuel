package at.maximilianmayer.veggietracker.meal;

import java.util.List;

public record SuggestMealResponse(String name,
                                  List<CreateMealPartRequest> parts,
                                  List<String> veggieHints) {
}
