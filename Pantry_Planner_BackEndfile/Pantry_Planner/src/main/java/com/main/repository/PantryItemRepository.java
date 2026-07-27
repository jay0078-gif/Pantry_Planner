package com.main.repository;

import com.main.model.PantryItem;
import com.main.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PantryItem — scoped to users.
 */
@Repository
public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    /** 🔹 All items belonging to a specific user */
    List<PantryItem> findByUser(User user);

    /** 🔹 A single item by a user + ingredient combination */
    Optional<PantryItem> findByUserAndIngredientId(User user, Long ingredientId);
}