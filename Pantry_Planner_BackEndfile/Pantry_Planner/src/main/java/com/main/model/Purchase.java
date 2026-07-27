package com.main.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Each Purchase record belongs to one User and one Ingredient.
 */
@Entity
@Table(name = "purchase")
@Getter
@Setter
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ✅ Logged‑in user who made this purchase */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Ingredient that was purchased */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private BigDecimal quantity;
    private String unit;
    private BigDecimal price;
    private Instant purchasedAt;

    @PrePersist
    void onCreate() {
        purchasedAt = Instant.now();
    }

    public Purchase() {}

    public Purchase(User user,
                    Ingredient ingredient,
                    BigDecimal quantity,
                    String unit,
                    BigDecimal price) {
        this.user = user;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.price = price;
        this.purchasedAt = Instant.now();
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

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Instant getPurchasedAt() {
		return purchasedAt;
	}

	public void setPurchasedAt(Instant purchasedAt) {
		this.purchasedAt = purchasedAt;
	}

	@Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", user=" + (user != null ? user.getId() : null) +
                ", ingredient=" + (ingredient != null ? ingredient.getId() : null) +
                ", quantity=" + quantity +
                ", unit='" + unit + '\'' +
                ", price=" + price +
                ", purchasedAt=" + purchasedAt +
                '}';
    }
}