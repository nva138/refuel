package at.maximilianmayer.veggietracker.meal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final MealService mealService;

    public MealController(MealService mealService) {
        this.mealService = mealService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MealResponse createMeal(@RequestBody CreateMealRequest request) {
        return mealService.createMeal(request);
    }

}
