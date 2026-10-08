package at.maximilianmayer.veggietracker.meal;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "meal_part")
public class MealPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meal_id")
    private  Meal meal;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "kcal_per_100g", nullable = false)
    private BigDecimal kcalPer100g;

    @Column(name = "protein_per_100g", nullable = false)
    private BigDecimal proteinPer100g;

    @Column(name = "carbs_per_100g", nullable = false)
    private BigDecimal carbsPer100g;

    @Column(name = "fat_per_100g", nullable = false)
    private BigDecimal fatPer100g;

    @Column(name = "amount_g", nullable = false)
    private BigDecimal amountG;

    public MealPart(){}

    public MealPart(
                    Meal meal,
                    String name,
                    BigDecimal kcalPer100g,
                    BigDecimal proteinPer100g,
                    BigDecimal carbsPer100g,
                    BigDecimal fatPer100g,
                    BigDecimal amountG){
        this.meal = meal;
        this.name = name;
        this.kcalPer100g = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.amountG = amountG;
    }

    public Long getId() {
        return id;
    }

    public Meal getMeal() {
        return meal;
    }

    public void setMeal(Meal meal) {
        this.meal = meal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getKcalPer100g() {
        return kcalPer100g;
    }

    public void setKcalPer100g(BigDecimal kcalPer100g) {
        this.kcalPer100g = kcalPer100g;
    }

    public BigDecimal getProteinPer100g() {
        return proteinPer100g;
    }

    public void setProteinPer100g(BigDecimal proteinPer100g) {
        this.proteinPer100g = proteinPer100g;
    }

    public BigDecimal getCarbsPer100g() {
        return carbsPer100g;
    }

    public void setCarbsPer100g(BigDecimal carbsPer100g) {
        this.carbsPer100g = carbsPer100g;
    }

    public BigDecimal getFatPer100g() {
        return fatPer100g;
    }

    public void setFatPer100g(BigDecimal fatPer100g) {
        this.fatPer100g = fatPer100g;
    }

    public BigDecimal getAmountG() {
        return amountG;
    }

    public void setAmountG(BigDecimal amountG) {
        this.amountG = amountG;
    }
}
