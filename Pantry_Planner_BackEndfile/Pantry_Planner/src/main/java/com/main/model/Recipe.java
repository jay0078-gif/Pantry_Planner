package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Entity representing a recipe created by a user. */

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "recipe")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 5000)
    private String instructions;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 1000)
    private String imageSourceUrl;

    @Column(length = 200)
    private String imagePhotographer;

    @Column(length = 1000)
    private String imagePhotographerUrl;

    @JsonIgnore
    private Instant imageLookupAttemptedAt;

    @JsonIgnore
    private Instant imageLookupRetryAt;

    @JsonIgnore
    private int imageLookupFailures;

    // 🧑‍🍳  Recipe belongs to a user (creator)
    // removed 'nullable = false' so seeding/test data without user won't crash
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    // 🍅 recipe ingredients
    @OneToMany(mappedBy = "recipe",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    // ---------- ctors ----------
    public Recipe() {}
    public Recipe(String name, String instructions) {
        this.name = name;
        this.instructions = instructions;
    }

    // ---------- getters/setters ----------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getImageSourceUrl() { return imageSourceUrl; }
    public void setImageSourceUrl(String imageSourceUrl) { this.imageSourceUrl = imageSourceUrl; }

    public String getImagePhotographer() { return imagePhotographer; }
    public void setImagePhotographer(String imagePhotographer) { this.imagePhotographer = imagePhotographer; }

    public String getImagePhotographerUrl() { return imagePhotographerUrl; }
    public void setImagePhotographerUrl(String imagePhotographerUrl) {
        this.imagePhotographerUrl = imagePhotographerUrl;
    }

    public Instant getImageLookupAttemptedAt() { return imageLookupAttemptedAt; }
    public void setImageLookupAttemptedAt(Instant imageLookupAttemptedAt) {
        this.imageLookupAttemptedAt = imageLookupAttemptedAt;
    }

    public Instant getImageLookupRetryAt() { return imageLookupRetryAt; }
    public void setImageLookupRetryAt(Instant imageLookupRetryAt) {
        this.imageLookupRetryAt = imageLookupRetryAt;
    }

    public int getImageLookupFailures() { return imageLookupFailures; }
    public void setImageLookupFailures(int imageLookupFailures) {
        this.imageLookupFailures = imageLookupFailures;
    }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients.clear();
        if (ingredients != null) ingredients.forEach(this::addIngredient);
    }

    public void addIngredient(RecipeIngredient ri) {
        if (ri == null) return;
        if (!ingredients.contains(ri)) {
            ingredients.add(ri);
            ri.setRecipe(this);
        }
    }

    public void removeIngredient(RecipeIngredient ri) {
        if (ri == null) return;
        ingredients.remove(ri);
        ri.setRecipe(null);
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", ingredientCount=" + (ingredients != null ? ingredients.size() : 0) +
                '}';
    }
}
