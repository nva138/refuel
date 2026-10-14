package at.maximilianmayer.veggietracker.weight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {
    Optional<WeightEntry> findByMeasuredOn(LocalDate measuredOn);
    List<WeightEntry> findByMeasuredOnGreaterThanEqualOrderByMeasuredOnAsc(LocalDate from);
}
