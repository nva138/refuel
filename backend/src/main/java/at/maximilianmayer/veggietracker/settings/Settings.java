package at.maximilianmayer.veggietracker.settings;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "settings")
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private GoalMode mode;

    @Column(name = "body_weight_kg", nullable = false)
    private BigDecimal bodyWeightKg;

    @Column(name = "kcal_reference", nullable = false)
    private Integer kcalReference;

    public Settings(){}

    public Settings(GoalMode mode, BigDecimal bodyWeightKg, Integer kcalReference) {
        this.mode = mode;
        this.bodyWeightKg = bodyWeightKg;
        this.kcalReference = kcalReference;
    }

    public Long getId() {
        return id;
    }

    public GoalMode getMode() {
        return mode;
    }

    public void setMode(GoalMode mode) {
        this.mode = mode;
    }

    public BigDecimal getBodyWeightKg() {
        return bodyWeightKg;
    }

    public void setBodyWeightKg(BigDecimal bodyWeightKg) {
        this.bodyWeightKg = bodyWeightKg;
    }

    public Integer getKcalReference() {
        return kcalReference;
    }

    public void setKcalReference(Integer kcalReference) {
        this.kcalReference = kcalReference;
    }
}
