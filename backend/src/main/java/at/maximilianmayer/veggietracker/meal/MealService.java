package at.maximilianmayer.veggietracker.meal;


import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

   @Transactional
   public MealResponse setFavourite(Long mealId, boolean favourite) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found"));
        meal.setFavourite(favourite);
        return MealResponse.from(meal);
   }

   @Transactional(readOnly = true)
   public List<MealResponse> getFavourites() {
        return mealRepository.findByFavouriteTrueOrderByNameAsc().stream()
                .map(MealResponse::from)
                .toList();
   }

   @Transactional
   public MealResponse updatePartAmount(Long mealId, Long partId, BigDecimal amountG) {
        if (amountG == null || amountG.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0");
        }

        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found"));
        MealPart part = meal.getParts().stream()
                .filter(candidate -> candidate.getId().equals(partId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Part not found"));

        part.setAmountG(amountG);
        return MealResponse.from(meal);
   }

   public ReadLabelResponse readLabel(byte[] photo) {
        return mealSuggester.readLabel(photo);
   }

    public DaySummaryResponse getToday() {
        return getDay(LocalDate.now(VIENNA));
    }
}
