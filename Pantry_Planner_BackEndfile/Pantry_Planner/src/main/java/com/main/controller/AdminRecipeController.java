package com.main.controller;

import com.main.exception.ResourceNotFoundException;
import com.main.model.Ingredient;
import com.main.model.Recipe;
import com.main.model.RecipeIngredient;
import com.main.model.RecipeSubmission;
import com.main.repository.IngredientRepository;
import com.main.repository.RecipeRepository;
import com.main.repository.RecipeSubmissionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    private final RecipeSubmissionRepository submissionRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;

    public AdminRecipeController(
            RecipeSubmissionRepository submissionRepository,
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository) {
        this.submissionRepository = submissionRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @GetMapping("/pending")
    public List<RecipeSubmission> getPendingSubmissions() {
        return submissionRepository.findByApprovedFalseAndRejectedFalse();
    }

    @PatchMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<Map<String, String>> approveRecipe(@PathVariable Long id) {
        RecipeSubmission submission = getSubmission(id);
        if (submission.isApproved()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipe is already approved"));
        }
        if (submission.isRejected()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A rejected recipe cannot be approved"));
        }

        Recipe recipe = new Recipe();
        recipe.setName(submission.getTitle());
        recipe.setInstructions(submission.getInstructions());
        recipe.setUser(submission.getSubmittedBy());

        for (String name : parseIngredientList(submission.getIngredients())) {
            String normalizedName = name.toLowerCase(Locale.ROOT);
            Ingredient ingredient = ingredientRepository.findByName(normalizedName)
                    .orElseGet(() -> {
                        Ingredient created = new Ingredient();
                        created.setName(normalizedName);
                        return ingredientRepository.save(created);
                    });
            RecipeIngredient recipeIngredient = new RecipeIngredient();
            recipeIngredient.setIngredient(ingredient);
            recipe.addIngredient(recipeIngredient);
        }

        recipeRepository.save(recipe);
        submission.setApproved(true);
        submission.setApprovedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        return ResponseEntity.ok(Map.of(
                "message", "Recipe approved and published",
                "title", recipe.getName()));
    }

    @PatchMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<Map<String, String>> rejectRecipe(@PathVariable Long id) {
        RecipeSubmission submission = getSubmission(id);
        if (submission.isApproved()) {
            return ResponseEntity.badRequest().body(Map.of("error", "An approved recipe cannot be rejected"));
        }
        if (submission.isRejected()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipe is already rejected"));
        }

        submission.setRejected(true);
        submission.setRejectedAt(LocalDateTime.now());
        submissionRepository.save(submission);
        return ResponseEntity.ok(Map.of("message", "Recipe rejected"));
    }

    private RecipeSubmission getSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe submission was not found"));
    }

    private List<String> parseIngredientList(List<String> ingredients) {
        if (ingredients == null) return Collections.emptyList();
        List<String> clean = new ArrayList<>();
        for (String ingredient : ingredients) {
            if (ingredient != null && !ingredient.isBlank()) {
                clean.add(ingredient.trim());
            }
        }
        return clean;
    }
}
