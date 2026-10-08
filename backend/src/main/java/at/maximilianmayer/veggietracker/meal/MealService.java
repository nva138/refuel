package at.maximilianmayer.veggietracker.meal;


import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MealService {

    private final MealRepository mealRepository;

    public MealService(MealRepository mealRepository) {
        this.mealRepository = mealRepository;
    }


   public Meal createMeal(CreateMealRequest request) {
        Meal meal = new Meal(Instant.now(),
                request.name(),
                request.isVegetarian(),
                request.enteredText());

        for(CreateMealPartRequest partRequest : request.parts()) {
            MealPart part = new MealPart(meal,
                    partRequest.name(),
                    partRequest.kcalPer100g(),
                    partRequest.proteinPer100g(),
                    partRequest.carbsPer100g(),
                    partRequest.fatPer100g(),
                    partRequest.amountG());

            meal.addPart(part);
        }

       return mealRepository.save(meal);
    }
}
