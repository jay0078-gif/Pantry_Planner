package com.main.controller;

import com.main.dto.PantryUpsertRequest;
import com.main.exception.ResourceNotFoundException;
import com.main.model.Ingredient;
import com.main.model.PantryItem;
import com.main.model.User;
import com.main.repository.PantryItemRepository;
import com.main.repository.UserRepository;
import com.main.service.IngredientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/pantry")
public class PantryController {

    private final PantryItemRepository repo;
    private final IngredientService ingredientService;
    private final UserRepository userRepo;

    public PantryController(
            PantryItemRepository repo,
            IngredientService ingredientService,
            UserRepository userRepo
    ) {
        this.repo = repo;
        this.ingredientService = ingredientService;
        this.userRepo = userRepo;
    }

    /** 🟢 Get all pantry items for the logged‑in user */
    @GetMapping
    public List<PantryItem> list(Principal principal) {
        return repo.findByUser(getUser(principal));
    }

    /** 🟢 Add new ingredient or update existing one for the logged‑in user */
    @PostMapping
    public ResponseEntity<PantryItem> add(
            Principal principal,
            @RequestBody PantryUpsertRequest req
    ) {
        User user = getUser(principal);

        Ingredient ing = ingredientService.findOrCreate(req.ingredientName());

        // look up by current user + ingredient
        PantryItem item = repo.findByUserAndIngredientId(user, ing.getId())
                .orElseGet(() -> {
                    PantryItem pi = new PantryItem();
                    pi.setUser(user);             // ✅ associate the entity
                    pi.setIngredient(ing);
                    return pi;
                });

        item.setQuantity(req.quantity());
        item.setUnit(req.unit());

        PantryItem saved = repo.save(item);
        return ResponseEntity.ok(saved);
    }

    /** 🟢 Partial update of an item (scoped to current user) */
    @PatchMapping("/{id}")
    public ResponseEntity<PantryItem> update(
            @PathVariable Long id,
            @RequestBody PantryUpsertRequest req,
            Principal principal
    ) {
        User user = getUser(principal);

        PantryItem item = repo.findById(id)
                .filter(i -> i.getUser().equals(user))     // ✅ don’t allow editing others’ items
                .orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        if (req.quantity() != null) item.setQuantity(req.quantity());
        if (req.unit() != null) item.setUnit(req.unit());

        PantryItem saved = repo.save(item);
        return ResponseEntity.ok(saved);
    }

    /** 🟢 Delete an item (scoped to logged‑in user) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        User user = getUser(principal);

        PantryItem item = repo.findById(id)
                .filter(i -> i.getUser().equals(user))     // ✅ delete only your own
                .orElse(null);
        if (item == null) return ResponseEntity.notFound().build();

        repo.delete(item);
        return ResponseEntity.noContent().build();
    }

    private User getUser(Principal principal) {
        return userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }
}
