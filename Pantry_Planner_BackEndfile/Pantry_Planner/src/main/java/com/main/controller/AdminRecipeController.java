//package com.main.controller;
//
//import com.main.model.*;
//import com.main.repository.*;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//@RestController
//@RequestMapping("/api/admin/recipes")
//public class AdminRecipeController {
//
//    private final RecipeSubmissionRepository submissionRepo;
//    private final RecipeRepository recipeRepo;
//    private final IngredientRepository ingredientRepo;
//    private final RecipeIngredientRepository recipeIngredientRepo;
//    private final UserRepository userRepo;
//
//    public AdminRecipeController(
//            RecipeSubmissionRepository submissionRepo,
//            RecipeRepository recipeRepo,
//            IngredientRepository ingredientRepo,
//            RecipeIngredientRepository recipeIngredientRepo,
//            UserRepository userRepo
//    ) {
//        this.submissionRepo = submissionRepo;
//        this.recipeRepo = recipeRepo;
//        this.ingredientRepo = ingredientRepo;
//        this.recipeIngredientRepo = recipeIngredientRepo;
//        this.userRepo = userRepo;
//    }
//
//    /** 🟢  Get all unapproved recipe submissions for admin review */
//    @GetMapping("/pending")
//    public List<RecipeSubmission> getPendingSubmissions() {
//        return submissionRepo.findByApprovedFalse();
//    }
//
//    /** 🟩  Approve a recipe submission and publish it as a Recipe */
//    @PatchMapping("/{id}/approve")
//    public ResponseEntity<Map<String, String>> approveRecipe(@PathVariable Long id) {
//        Map<String, String> resp = new HashMap<>();
//
//        try {
//            // 1️⃣  Find submission
//            RecipeSubmission sub = submissionRepo.findById(id)
//                    .orElseThrow(() ->
//                            new RuntimeException("Submission not found (id=" + id + ")"));
//
//            // 2️⃣  Prevent re‑approval of same submission
//            if (sub.isApproved()) {
//                resp.put("message", "Already approved");
//                return ResponseEntity.badRequest().body(resp);
//            }
//
//            // 3️⃣  Construct new Recipe from submission
//            Recipe recipe = new Recipe();
//            recipe.setName(sub.getTitle());
//            recipe.setInstructions(sub.getInstructions());
//
//            // 👇 set the recipe's owner to the submitting user (fixes user_id NOT NULL)
//            recipe.setUser(sub.getSubmittedBy());
//
//            // 4️⃣  Link ingredients safely
//            List<String> ingredientNames = parseIngredientList(sub.getIngredients());
//            for (String name : ingredientNames) {
//                if (name == null || name.trim().isEmpty()) continue;
//                String lower = name.toLowerCase();
//
//                // find or create ingredient
//                Ingredient ingredient = ingredientRepo.findByName(lower)
//                        .orElseGet(() -> {
//                            Ingredient i = new Ingredient();
//                            i.setName(lower);
//                            return ingredientRepo.save(i);
//                        });
//
//                RecipeIngredient ri = new RecipeIngredient();
//                ri.setIngredient(ingredient);
//                recipe.addIngredient(ri);
//            }
//
//            // 5️⃣  Save recipe and mark submission approved
//            recipeRepo.save(recipe);
//            sub.setApproved(true);
//            sub.setApprovedAt(LocalDateTime.now());
//            submissionRepo.save(sub);
//
//            resp.put("message", "✅ Recipe approved and published!");
//            resp.put("title", recipe.getName());
//            return ResponseEntity.ok(resp);
//
//        } catch (Exception e) {
//            // 6️⃣  Log and return readable error JSON
//            e.printStackTrace();
//            resp.put("error", "Approval failed: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
//        }
//    }
//
//    /** Helper to clean and trim the ingredients list */
//    private List<String> parseIngredientList(List<String> ingredients) {
//        if (ingredients == null) return Collections.emptyList();
//        List<String> clean = new ArrayList<>();
//        for (String ing : ingredients) {
//            if (ing != null && !ing.trim().isEmpty()) {
//                clean.add(ing.trim());
//            }
//        }
//        return clean;
//    }
//}


package com.main.controller;

import com.main.model.*;
import com.main.repository.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin/recipes")
public class AdminRecipeController {

    private final RecipeSubmissionRepository submissionRepo;
    private final RecipeRepository recipeRepo;
    private final IngredientRepository ingredientRepo;
    private final RecipeIngredientRepository recipeIngredientRepo;
    private final UserRepository userRepo;

    public AdminRecipeController(
            RecipeSubmissionRepository submissionRepo,
            RecipeRepository recipeRepo,
            IngredientRepository ingredientRepo,
            RecipeIngredientRepository recipeIngredientRepo,
            UserRepository userRepo
    ) {
        this.submissionRepo = submissionRepo;
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
        this.recipeIngredientRepo = recipeIngredientRepo;
        this.userRepo = userRepo;
    }

    /** 🟢  Get all unapproved recipe submissions for owner/admin review */
    @GetMapping("/pending")
    public List<RecipeSubmission> getPendingSubmissions() {
        return submissionRepo.findByApprovedFalseAndRejectedFalse();
    }

    /** 🟩  Approve a recipe submission and publish it as a Recipe */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Map<String, String>> approveRecipe(@PathVariable Long id) {
        Map<String, String> resp = new HashMap<>();
        try {
            RecipeSubmission sub = submissionRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Submission not found (id=" + id + ")"));

            if (sub.isApproved()) {
                resp.put("message", "Already approved");
                return ResponseEntity.badRequest().body(resp);
            }
            if (sub.isRejected()) {
                resp.put("message", "Cannot approve — already rejected");
                return ResponseEntity.badRequest().body(resp);
            }

            // Construct new Recipe from submission
            Recipe recipe = new Recipe();
            recipe.setName(sub.getTitle());
            recipe.setInstructions(sub.getInstructions());
            recipe.setUser(sub.getSubmittedBy());

            // Link ingredients
            List<String> ingredientNames = parseIngredientList(sub.getIngredients());
            for (String name : ingredientNames) {
                if (name == null || name.trim().isEmpty()) continue;
                String lower = name.toLowerCase();

                Ingredient ingredient = ingredientRepo.findByName(lower)
                        .orElseGet(() -> {
                            Ingredient i = new Ingredient();
                            i.setName(lower);
                            return ingredientRepo.save(i);
                        });

                RecipeIngredient ri = new RecipeIngredient();
                ri.setIngredient(ingredient);
                recipe.addIngredient(ri);
            }

            recipeRepo.save(recipe);
            sub.setApproved(true);
            sub.setApprovedAt(LocalDateTime.now());
            submissionRepo.save(sub);

            resp.put("message", "✅ Recipe approved and published!");
            resp.put("title", recipe.getName());
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.put("error", "Approval failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }
    }

    /** 🟥  Reject a recipe submission */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Map<String, String>> rejectRecipe(@PathVariable Long id) {
        Map<String, String> resp = new HashMap<>();
        try {
            RecipeSubmission sub = submissionRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Submission not found (id=" + id + ")"));

            if (sub.isApproved()) {
                resp.put("message", "Cannot reject — already approved");
                return ResponseEntity.badRequest().body(resp);
            }
            if (sub.isRejected()) {
                resp.put("message", "Already rejected");
                return ResponseEntity.badRequest().body(resp);
            }

            sub.setRejected(true);
            sub.setRejectedAt(LocalDateTime.now()); // add this field to entity if not there
            submissionRepo.save(sub);

            resp.put("message", "❌ Recipe rejected and removed from pending list.");
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            e.printStackTrace();
            resp.put("error", "Rejection failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }
    }

    /** Helper to clean and trim ingredients list */
    private List<String> parseIngredientList(List<String> ingredients) {
        if (ingredients == null) return Collections.emptyList();
        List<String> clean = new ArrayList<>();
        for (String ing : ingredients) {
            if (ing != null && !ing.trim().isEmpty()) {
                clean.add(ing.trim());
            }
        }
        return clean;
    }
}