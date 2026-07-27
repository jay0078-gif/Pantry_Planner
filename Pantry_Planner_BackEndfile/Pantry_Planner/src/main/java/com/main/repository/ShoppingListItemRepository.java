package com.main.repository;

import com.main.model.ShoppingListItem;
import com.main.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ShoppingListItem — each query scoped to a User.
 */
@Repository
public interface ShoppingListItemRepository extends JpaRepository<ShoppingListItem, Long> {

    /** 🔹 Get all items for a specific user (including purchased) */
    List<ShoppingListItem> findByUser(User user);

    /** 🔹 Get all items for a user that have *not* been purchased yet */
    List<ShoppingListItem> findByUserAndPurchasedFalse(User user);
}