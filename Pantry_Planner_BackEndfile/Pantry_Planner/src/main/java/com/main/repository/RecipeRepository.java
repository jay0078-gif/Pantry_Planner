package com.main.repository;

import com.main.model.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    /** Exact match (case-insensitive). */
    Optional<Recipe> findByNameIgnoreCase(String name);

    /** Partial match on recipe name (case-insensitive). */
    List<Recipe> findByNameContainingIgnoreCase(String name);

    /**
     * Search by recipe name OR any ingredient name (case-insensitive).
     */
    @Query("""
        SELECT DISTINCT r FROM Recipe r
        LEFT JOIN FETCH r.ingredients ri
        LEFT JOIN FETCH ri.ingredient ing
        WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(ing.name) LIKE LOWER(CONCAT('%', :term, '%'))
        """)
    List<Recipe> searchByNameOrIngredient(String term);
}