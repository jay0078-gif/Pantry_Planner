package com.main.service;

import com.main.dto.SuggestionDTO;
import com.main.model.*;
import com.main.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuggestionService {

    private final RecipeRepository recipeRepo;
    private final PantryItemRepository pantryRepo;
    private final PhotoService photoService;
    private final UserRepository userRepo;

    public SuggestionService(RecipeRepository recipeRepo,
                             PantryItemRepository pantryRepo,
                             PhotoService photoService,
                             UserRepository userRepo) {
        this.recipeRepo = recipeRepo;
        this.pantryRepo = pantryRepo;
        this.photoService = photoService;
        this.userRepo = userRepo;
    }

    /** 🔹 Core algorithm using a User entity (scoped pantry) */
    private List<SuggestionDTO> getSuggestions(User user, int maxMissing, int limit) {
        if (user == null) return Collections.emptyList();

        // set of ingredient IDs current user has in pantry
        Set<Long> pantrySet = pantryRepo.findByUser(user).stream()
                .filter(Objects::nonNull)
                .map(PantryItem::getIngredient)
                .filter(Objects::nonNull)
                .map(Ingredient::getId)
                .collect(Collectors.toSet());

        List<Recipe> recipes = recipeRepo.findAll();
        if (recipes == null || recipes.isEmpty()) return Collections.emptyList();

        List<SuggestionDTO> out = new ArrayList<>();

        for (Recipe r : recipes) {
            List<RecipeIngredient> ris = r.getIngredients();
            if (ris == null || ris.isEmpty()) continue;

            int total = ris.size();
            int matched = 0;
            List<String> missingNames = new ArrayList<>();

            for (RecipeIngredient ri : ris) {
                if (ri == null || ri.getIngredient() == null) continue;
                Long ingId = ri.getIngredient().getId();
                if (ingId != null && pantrySet.contains(ingId)) {
                    matched++;
                } else {
                    missingNames.add(ri.getIngredient().getName());
                }
            }

            int missing = total - matched;
            if (missing <= maxMissing) {
                String image = photoService.getUrl(r.getImageUrl());
                out.add(new SuggestionDTO(
                        r.getId(),
                        r.getName(),
                        image,
                        r.getImageSourceUrl(),
                        r.getImagePhotographer(),
                        r.getImagePhotographerUrl(),
                        total,
                        matched,
                        missing,
                        missingNames
                ));
            }
        }

        // sort: fewest missing, then most matched
        out.sort(Comparator.comparingInt(SuggestionDTO::missing)
                .thenComparing(Comparator.comparingInt(SuggestionDTO::matched).reversed()));

        if (limit > 0 && out.size() > limit) return out.subList(0, limit);
        return out;
    }

    /** 🔹 Public helper called from SuggestionController */
    public List<SuggestionDTO> getSuggestionsByUsername(String username, int maxMissing, int limit) {
        if (username == null || username.isBlank()) return Collections.emptyList();

        return userRepo.findByUsername(username)
                       .map(u -> getSuggestions(u, maxMissing, limit))   // ✅ use entity, not ID
                       .orElse(Collections.emptyList());
    }
}
