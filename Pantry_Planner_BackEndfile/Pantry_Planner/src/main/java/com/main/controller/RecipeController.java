package com.main.controller;

import com.main.model.Recipe;
import com.main.repository.RecipeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "http://localhost:5173") // ✅ match frontend dev server port
public class RecipeController {

    private final RecipeRepository recipeRepository;

    @Autowired
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    // ------------------------------------------------------------------
    // 🟢 GET /api/recipes?search=
    // ------------------------------------------------------------------
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAll(@RequestParam(required = false, defaultValue = "") String search) {
        System.out.println("🔍 Incoming search: " + search);
        try {
            List<Recipe> results;

            if (search == null || search.isBlank()) {
                results = recipeRepository.findAll();
            } else {
                results = recipeRepository.searchByNameOrIngredient(search.trim());
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // 🟢 GET /api/recipes/{id}
    // ------------------------------------------------------------------
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return recipeRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}