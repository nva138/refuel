package at.maximilianmayer.veggietracker.meal;

public interface MealSuggester {
    SuggestMealResponse suggest(String text);
}
