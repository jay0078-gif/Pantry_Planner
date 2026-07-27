package com.main.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Each shopping‑list item belongs to exactly one authenticated user.
 */
@Entity
@Table(name = "shopping_list_item")
@Getter
@Setter
public class ShoppingListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Logged‑in user who owns this list item */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Linked ingredient */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    /** Quantity needed; can be null if unspecified */
    @Column(name = "needed_qty", precision = 38, scale = 2)
    private BigDecimal neededQty;

    /** Units of measurement, e.g. "g", "ml", "pcs" */
    @Column(length = 50)
    private String unit = "";

    /**
     * Whether the item was purchased.
     * ✅  Default false both in Java and in DB.
     */
    @Column(name = "is_purchased", nullable = false,
            columnDefinition = "BOOLEAN DEFAULT 0")
    private boolean purchased = false;

    /** Creation timestamp automatically set on persist */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** ✅ Explicit public no‑args constructor */
    public ShoppingListItem() {
        // Required by JPA and by controller
    }

    /** Convenience constructor for manual creation */
    public ShoppingListItem(User user,
                            Ingredient ingredient,
                            BigDecimal neededQty,
                            String unit,
                            boolean purchased) {
        this.user = user;
        this.ingredient = ingredient;
        this.neededQty = neededQty;
        this.unit = unit != null ? unit : "";
        this.purchased = purchased;
        this.createdAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (unit == null) unit = "";
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

	public BigDecimal getNeededQty() {
		return neededQty;
	}

	public void setNeededQty(BigDecimal neededQty) {
		this.neededQty = neededQty;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public boolean isPurchased() {
		return purchased;
	}

	public void setPurchased(boolean purchased) {
		this.purchased = purchased;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	@Override
    public String toString() {
        return "ShoppingListItem{" +
                "id=" + id +
                ", user=" + (user != null ? user.getId() : null) +
                ", ingredient=" + (ingredient != null ? ingredient.getId() : null) +
                ", neededQty=" + neededQty +
                ", unit='" + unit + '\'' +
                ", purchased=" + purchased +
                ", createdAt=" + createdAt +
                '}';
    }
}