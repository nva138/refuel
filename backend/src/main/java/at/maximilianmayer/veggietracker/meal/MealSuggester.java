package at.maximilianmayer.veggietracker.meal;

public interface MealSuggester {
    SuggestMealResponse suggest(String text);
    SuggestMealResponse suggestFromPhoto(byte[] photo, String text);
    ReadLabelResponse readLabel(byte[] photo);
}
