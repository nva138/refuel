package at.maximilianmayer.veggietracker.meal;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final MealService mealService;
    private final MealSuggester mealSuggester;

    public MealController(MealService mealService, MealSuggester mealSuggester) {
        this.mealService = mealService;
        this.mealSuggester = mealSuggester;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MealResponse createMeal(@RequestBody CreateMealRequest request) {
        return mealService.createMeal(request);
    }

    @GetMapping("/today")
    public DaySummaryResponse getMealsForTheDay() {
        return mealService.getToday();
    }

    @PostMapping("/suggest")
    public SuggestMealResponse suggestMeal(@RequestBody SuggestMealRequest request) {
        return mealService.suggestMeal(request);
    }

    @PostMapping(value = "/suggest-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuggestMealResponse suggestMealPhoto(@RequestPart("photo") MultipartFile photo, @RequestParam(value = "text", required = false) String text)  throws IOException {
        return mealService.suggestMealFromPhoto(photo.getBytes(), text);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMeal(@PathVariable Long id) {
        mealService.deleteMeal(id);
    }
}
