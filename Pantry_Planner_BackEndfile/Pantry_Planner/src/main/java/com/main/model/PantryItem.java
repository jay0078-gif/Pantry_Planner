package com.main.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Each PantryItem belongs to one authenticated User and one Ingredient.
 */
@Entity
@Table(
    name = "pantry_item",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ingredient_id"})
)
@Getter
@Setter
public class PantryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ✅ Logged‑in user who owns this pantry item */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** the ingredient reference */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private BigDecimal quantity;       // optional
    private String unit;
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public PantryItem() {}

    public PantryItem(User user, Ingredient ingredient, BigDecimal quantity, String unit) {
        this.user = user;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.updatedAt = Instant.now();
    }
    
    

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Ingredient getIngredient() {
		return ingredient;
	}

	public void setIngredient(Ingredient ingredient) {
		this.ingredient = ingredient;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	@Override
    public String toString() {
        return "PantryItem{" +
                "id=" + id +
                ", user=" + (user != null ? user.getId() : null) +
                ", ingredient=" + (ingredient != null ? ingredient.getId() : null) +
                ", quantity=" + quantity +
                ", unit='" + unit + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}