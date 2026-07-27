package com.main.repository;

import com.main.model.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
Optional<Ingredient> findByName(String name);

@Query("select i from Ingredient i where i.name like concat('%', :q, '%')")
List<Ingredient> search(@Param("q") String q);
}
