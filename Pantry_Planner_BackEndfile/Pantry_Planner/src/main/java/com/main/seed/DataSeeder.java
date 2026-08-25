package com.main.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.*;
import com.main.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RecipeRepository recipeRepo;
    private final IngredientRepository ingredientRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper mapper;
    private final String ownerPassword;
    private final String defaultUserPassword;

    public DataSeeder(RecipeRepository recipeRepo,
                      IngredientRepository ingredientRepo,
                      UserRepository userRepo,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper mapper,
                      @Value("${app.seed.owner-password}") String ownerPassword,
                      @Value("${app.seed.user-password}") String defaultUserPassword) {
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.ownerPassword = ownerPassword;
        this.defaultUserPassword = defaultUserPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        seedDefaultUsers();

        // seed recipes / patch data
        seedBase("data/recipes.json");
        applyPatches("data/recipes.override.json");
    }

    private void seedDefaultUsers() {
        User owner = userRepo.findByUsername("owner").orElse(null);
        if (owner == null) {
            if (ownerPassword.isBlank()) {
                throw new IllegalStateException(
                        "SEED_OWNER_PASSWORD is required when the owner account does not exist."
                );
            }
            owner = new User();
            owner.setUsername("owner");
            owner.setRole(Role.ROLE_ADMIN);
            owner.setPassword(passwordEncoder.encode(ownerPassword));
            userRepo.save(owner);
        }

        if (!defaultUserPassword.isBlank() && userRepo.findByUsername("jane").isEmpty()) {
            User jane = new User();
            jane.setUsername("jane");
            jane.setRole(Role.ROLE_USER);
            jane.setPassword(passwordEncoder.encode(defaultUserPassword));
            userRepo.save(jane);
        }
    }

    // ------------------------------------------------------------------------
    //  🥫 Base recipe seeding
    // ------------------------------------------------------------------------
    private void seedBase(String path) throws Exception {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            System.out.println(path + " not found, skipping base seed.");
            return;
        }

        // 👑 Assign all seeded recipes to the admin user by default
        User owner = userRepo.findByUsername("owner")
                .orElseThrow(() -> new RuntimeException("Default 'owner' user missing, cannot seed recipes."));

        try (InputStream is = res.getInputStream()) {
            List<Map<String, Object>> list = mapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> rmap : list) {
                String name = ((String) rmap.get("name")).trim();
                Optional<Recipe> existing = recipeRepo.findByNameIgnoreCase(name);
                if (existing.isPresent()) continue;

                Recipe r = new Recipe();
                r.setName(name);
                r.setInstructions((String) rmap.get("instructions"));
                r.setImageUrl((String) rmap.get("imageUrl"));
                r.setUser(owner);  // ✅ assign valid user for FK constraint

                @SuppressWarnings("unchecked")
                List<String> ingNames = (List<String>) rmap.get("ingredients");
                if (ingNames != null) {
                    for (String raw : ingNames) {
                        String ingName = raw.trim().toLowerCase();
                        Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
                            Ingredient i = new Ingredient();
                            i.setName(ingName);
                            return ingredientRepo.save(i);
                        });

                        RecipeIngredient ri = new RecipeIngredient();
                        ri.setRecipe(r);
                        ri.setIngredient(ing);
                        r.getIngredients().add(ri);
                    }
                }
                recipeRepo.save(r);
            }
        }
    }

    // ------------------------------------------------------------------------
    //  🔧 Optional recipe patch updates
    // ------------------------------------------------------------------------
    private void applyPatches(String path) throws Exception {
        ClassPathResource res = new ClassPathResource(path);
        if (!res.exists()) {
            System.out.println(path + " not found, skipping patches.");
            return;
        }

        User owner = userRepo.findByUsername("owner")
                .orElseThrow(() -> new RuntimeException("Default 'owner' user missing, cannot patch recipes."));

        try (InputStream is = res.getInputStream()) {
            List<Map<String, Object>> patches = mapper.readValue(is, new TypeReference<>() {});
            int updated = 0;
            for (Map<String, Object> p : patches) {
                String name = ((String) p.get("name")).trim();
                Optional<Recipe> opt = recipeRepo.findByNameIgnoreCase(name);
                if (opt.isPresent()) {
                    Recipe r = opt.get();
                    if (p.containsKey("instructions")) r.setInstructions((String) p.get("instructions"));
                    if (p.containsKey("imageUrl")) r.setImageUrl((String) p.get("imageUrl"));

                    // if we added user later in schema, enforce it here too
                    if (r.getUser() == null) r.setUser(owner);

                    @SuppressWarnings("unchecked")
                    List<String> ingNames = (List<String>) p.get("ingredients");
                    if (ingNames != null) {
                        Set<String> existing = r.getIngredients().stream()
                                .map(ri -> ri.getIngredient().getName())
                                .collect(Collectors.toSet());
                        for (String raw : ingNames) {
                            String ingName = raw.trim().toLowerCase();
                            if (existing.contains(ingName)) continue;
                            Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
                                Ingredient i = new Ingredient();
                                i.setName(ingName);
                                return ingredientRepo.save(i);
                            });
                            RecipeIngredient ri = new RecipeIngredient();
                            ri.setRecipe(r);
                            ri.setIngredient(ing);
                            r.getIngredients().add(ri);
                        }
                    }
                    recipeRepo.save(r);
                    updated++;
                }
            }
            System.out.printf("Patches applied: updated=%d%n", updated);
        }
    }
}
