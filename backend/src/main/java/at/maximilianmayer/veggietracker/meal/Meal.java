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

    @Column(name = "entered_text")
    private String enteredText;

    @Column(name = "favourite", nullable = false)
    private boolean favourite;

    @ElementCollection
    @CollectionTable(name = "meal_veggie_hint", joinColumns = @JoinColumn(name = "meal_id"))
    @Column(name = "hint")
    private List<String> veggieHints = new ArrayList<>();

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealPart> parts = new ArrayList<>();

    public Meal(){}

    public Meal(Instant eatenAt, String name, String enteredText, List<String> veggieHints) {
        this.eatenAt = eatenAt;
        this.name = name;
        this.enteredText = enteredText;
        this.veggieHints = new ArrayList<>(veggieHints);

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

    public String getEnteredText() {
        return enteredText;
    }

    public void setEnteredText(String enteredText) {
        this.enteredText = enteredText;
    }

    public List<MealPart> getParts() {
        return parts;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public List<String> getVeggieHints() {
        return veggieHints;
    }
}


