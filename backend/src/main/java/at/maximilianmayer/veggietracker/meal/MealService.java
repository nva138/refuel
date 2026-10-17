package at.maximilianmayer.veggietracker.meal;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MealService {

    private static final ZoneId VIENNA = ZoneId.of("Europe/Vienna");

    private final MealRepository mealRepository;
    private final MealSuggester mealSuggester;

    public MealService(MealRepository mealRepository, MealSuggester mealSuggester) {
        this.mealRepository = mealRepository;
        this.mealSuggester = mealSuggester;
    }


   public MealResponse createMeal(CreateMealRequest request) {
        Meal meal = new Meal(Instant.now(),
                request.name(),
                request.enteredText(),
                request.veggieHints());

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

        Meal saved = mealRepository.save(meal);
        return MealResponse.from(saved);
    }


    @Transactional(readOnly = true)
   public DaySummaryResponse getDay(LocalDate date){
       Instant start = date.atStartOfDay(VIENNA).toInstant();
       Instant end = date.plusDays(1).atStartOfDay(VIENNA).toInstant();

       List<Meal> meals = mealRepository.findByEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(start, end);

       BigDecimal totalKcal = BigDecimal.ZERO;
       BigDecimal totalProtein = BigDecimal.ZERO;
       BigDecimal totalCarbs = BigDecimal.ZERO;
       BigDecimal totalFats = BigDecimal.ZERO;

       for(Meal meal : meals) {
           for(MealPart part : meal.getParts()) {
               BigDecimal factor = part.getAmountG().divide(BigDecimal.valueOf(100));
               totalKcal = totalKcal.add(part.getKcalPer100g().multiply(factor));
               totalProtein = totalProtein.add(part.getProteinPer100g().multiply(factor));
               totalCarbs = totalCarbs.add(part.getCarbsPer100g().multiply(factor));
               totalFats = totalFats.add(part.getFatPer100g().multiply(factor));
            }

       }
       List<MealResponse> mealResponses = meals.stream().map(MealResponse::from).toList();
       return new DaySummaryResponse(date, mealResponses, totalKcal, totalProtein, totalCarbs, totalFats);
   }

   public SuggestMealResponse suggestMeal(SuggestMealRequest request) {
        return mealSuggester.suggest(request.text());
   }

   public SuggestMealResponse suggestMealFromPhoto(byte[] photo, String text) {
        return mealSuggester.suggestFromPhoto(photo, text);
   }

   public void deleteMeal(Long id) {
        mealRepository.deleteById(id);
   }

   public ReadLabelResponse readLabel(byte[] photo) {
        return mealSuggester.readLabel(photo);
   }

    public DaySummaryResponse getToday() {
        return getDay(LocalDate.now(VIENNA));
    }
}
