package at.maximilianmayer.veggietracker.meal;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meal")
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "eaten_at", nullable = false)
    private Instant eatenAt;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_vegetarian", nullable = false)
    private boolean isVegetarian;

    @Column(name = "entered_text")
    private String enteredText;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPart> parts = new ArrayList<>();

    public Meal(){}

    public Meal(Instant eatenAt, String name, boolean isVegetarian, String enteredText) {
        this.eatenAt = eatenAt;
        this.name = name;
        this.isVegetarian = isVegetarian;
        this.enteredText = enteredText;
    }

    public void addPart(MealPart part) {
        parts.add(part);
        part.setMeal(this);
    }

    public Long getId() {
        return id;
    }

    public Instant getEatenAt() {
        return eatenAt;
    }

    public void setEatenAt(Instant eatenAt) {
        this.eatenAt = eatenAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVegetarian() {
        return isVegetarian;
    }

    public void setVegetarian(boolean vegetarian) {
        isVegetarian = vegetarian;
    }

    public String getEnteredText() {
        return enteredText;
    }

    public void setEnteredText(String enteredText) {
        this.enteredText = enteredText;
    }
}


