package com.main.controller;

import com.main.dto.PurchaseDTO;
import com.main.dto.PurchaseRequest;
import com.main.model.*;
import com.main.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shopping-list")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ShoppingListController {

    private final ShoppingListItemRepository repo;
    private final RecipeRepository recipeRepo;
    private final PantryItemRepository pantryRepo;
    private final PurchaseRepository purchaseRepo;
    private final UserRepository userRepo;

    public ShoppingListController(
            ShoppingListItemRepository repo,
            RecipeRepository recipeRepo,
            PantryItemRepository pantryRepo,
            PurchaseRepository purchaseRepo,
            UserRepository userRepo) {
        this.repo = repo;
        this.recipeRepo = recipeRepo;
        this.pantryRepo = pantryRepo;
        this.purchaseRepo = purchaseRepo;
        this.userRepo = userRepo;
    }

    /** 🟢 Get all active (not yet purchased) items for the logged‑in user */
    @GetMapping
    public List<ShoppingListItem> list(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return Collections.emptyList();
        User user = getUser(principal);
        return repo.findByUserAndPurchasedFalse(user);
    }

    /** 🟢 Add all ingredients from a recipe that this user doesn't yet have */
    @PostMapping("/from-recipe/{recipeId}")
    public List<ShoppingListItem> fromRecipe(@PathVariable Long recipeId,
                                             @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) return Collections.emptyList();
        User user = getUser(principal);

        Recipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found: " + recipeId));

        // Collect ingredient IDs already covered by pantry or shopping list
        Set<Long> pantryIngIds = pantryRepo.findByUser(user).stream()
                .map(p -> p.getIngredient().getId())
                .collect(Collectors.toSet());

        Set<Long> currentIngIds = repo.findByUserAndPurchasedFalse(user).stream()
                .map(sl -> sl.getIngredient().getId())
                .collect(Collectors.toSet());

        List<ShoppingListItem> created = new ArrayList<>();

        for (RecipeIngredient ri : recipe.getIngredients()) {
            if (ri == null || ri.getIngredient() == null) continue;
            Long ingId = ri.getIngredient().getId();

            // Skip ingredients already owned or already in the list
            if (pantryIngIds.contains(ingId) || currentIngIds.contains(ingId)) continue;

            // Create new shopping‑list entry
            ShoppingListItem sli = new ShoppingListItem();
            sli.setUser(user);
            sli.setIngredient(ri.getIngredient());
            sli.setNeededQty(ri.getQuantity() != null ? BigDecimal.valueOf(ri.getQuantity()) : BigDecimal.ZERO);
            sli.setUnit(ri.getUnit() != null ? ri.getUnit() : "");
            sli.setPurchased(false);   // ✅ ensure default value is always false

            created.add(repo.save(sli));
        }

        return created;
    }

    /** 🟢 Delete an item that belongs to the current user */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails principal) {
        if (principal == null) return ResponseEntity.badRequest().build();
        User user = getUser(principal);

        return repo.findById(id)
                .filter(it -> it.getUser().equals(user))
                .map(it -> {
                    repo.delete(it);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** 🟢 Purchase an item: mark bought, add to pantry, create purchase record */
    @PostMapping("/{id}/purchase")
    public PurchaseDTO purchase(@PathVariable Long id,
                                @RequestBody(required = false) PurchaseRequest req,
                                @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) throw new RuntimeException("User not logged in");
        User user = getUser(principal);

        ShoppingListItem sli = repo.findById(id)
                .filter(it -> it.getUser().equals(user))
                .orElseThrow(() -> new RuntimeException("Item not found or not owned by user"));

        // Add or update pantry entry
        PantryItem pantryItem = pantryRepo.findByUserAndIngredientId(user, sli.getIngredient().getId())
                .orElseGet(() -> {
                    PantryItem pi = new PantryItem();
                    pi.setUser(user);
                    pi.setIngredient(sli.getIngredient());
                    return pi;
                });
        pantryRepo.save(pantryItem);

        // Create purchase record
        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setIngredient(sli.getIngredient());
        purchase.setQuantity(req != null && req.quantity() != null ? req.quantity() : sli.getNeededQty());
        purchase.setUnit(req != null && req.unit() != null ? req.unit() : sli.getUnit());
        purchase.setPrice(req != null ? req.price() : null);
        purchaseRepo.save(purchase);

        // Mark as purchased
        sli.setPurchased(true);
        repo.save(sli);

        return new PurchaseDTO(
                purchase.getId(),
                purchase.getIngredient().getId(),
                purchase.getIngredient().getName(),
                purchase.getQuantity(),
                purchase.getUnit(),
                purchase.getPrice(),
                purchase.getPurchasedAt()
        );
    }

    /** Helper: resolve User entity for current principal */
    private User getUser(UserDetails principal) {
        return userRepo.findByUsername(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + principal.getUsername()));
    }
}