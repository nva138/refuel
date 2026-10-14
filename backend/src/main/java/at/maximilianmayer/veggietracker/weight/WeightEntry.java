package at.maximilianmayer.veggietracker.weight;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "weight_entry")
public class WeightEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "measured_on", nullable = false)
    private LocalDate measuredOn;

    @Column(name = "weight_kg", nullable = false)
    private BigDecimal weightKg;

    public WeightEntry() {
    }
    public WeightEntry(LocalDate measuredOn, BigDecimal weightKg) {
        this.measuredOn = measuredOn;
        this.weightKg = weightKg;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getMeasuredOn() {
        return measuredOn;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }
}
