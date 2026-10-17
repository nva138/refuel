package at.maximilianmayer.veggietracker.meal;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
    List<Meal> findByEatenAtGreaterThanEqualAndEatenAtLessThanOrderByEatenAtAsc(Instant start, Instant end);

    List<Meal> findByFavouriteTrueOrderByNameAsc();
}
