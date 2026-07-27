package com.main.service;

import com.main.model.PantryItem;
import com.main.model.User;
import com.main.repository.PantryItemRepository;
import com.main.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Business logic for Pantry items.  All queries are scoped to the authenticated user.
 */
@Service
public class PantryService {

    private final PantryItemRepository pantryRepo;
    private final UserRepository userRepo;

    public PantryService(PantryItemRepository pantryRepo, UserRepository userRepo) {
        this.pantryRepo = pantryRepo;
        this.userRepo = userRepo;
    }

    /** 🔹 Get all pantry items belonging to a user identified by username. */
    public List<PantryItem> getPantryByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }

        return userRepo.findByUsername(username)
                       .map(pantryRepo::findByUser)      // ✅ use entity, not ID
                       .orElse(Collections.emptyList());
    }

    /** 🔹 Add a new item for the given user. */
    public PantryItem addItem(User user, PantryItem item) {
        item.setUser(user);                               // ✅ associate logged‑in user
        return pantryRepo.save(item);
    }

    /** 🔹 Remove an item (only if owned by the user). */
    public void removeItem(Long id, User user) {
        pantryRepo.findById(id)
                  .filter(it -> it.getUser().equals(user)) // ✅ ownership check
                  .ifPresent(pantryRepo::delete);
    }
}