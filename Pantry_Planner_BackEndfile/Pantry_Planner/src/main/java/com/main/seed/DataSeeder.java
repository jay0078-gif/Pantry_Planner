package com.main.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.main.model.*;
import com.main.repository.*;
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

    public DataSeeder(RecipeRepository recipeRepo,
                      IngredientRepository ingredientRepo,
                      UserRepository userRepo,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper mapper) {
        this.recipeRepo = recipeRepo;
        this.ingredientRepo = ingredientRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        // ensure admin & default user accounts exist
        seedDefaultUsers();

        // seed recipes / patch data
        seedBase("data/recipes.json");
        applyPatches("data/recipes.override.json");
    }

    /**
     * Always create or update default accounts with known bcrypt passwords.
     */
    private void seedDefaultUsers() {
        // OWNER  --------------------------------------------------------------
        User owner = userRepo.findByUsername("owner").orElse(null);
        if (owner == null) {
            owner = new User();
            owner.setUsername("owner");
            owner.setRole(Role.ROLE_ADMIN);
        }
        owner.setPassword(passwordEncoder.encode("owner123"));
        userRepo.save(owner);
        System.out.println("✅  Owner account ready:  owner / owner123");

        // JANE (normal user)  -------------------------------------------------
        User jane = userRepo.findByUsername("jane").orElse(null);
        if (jane == null) {
            jane = new User();
            jane.setUsername("jane");
            jane.setRole(Role.ROLE_USER);
        }
        jane.setPassword(passwordEncoder.encode("user123"));
        userRepo.save(jane);
        System.out.println("✅  Default user ready:  jane / user123");
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

//package com.main.seed;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.main.model.*;
//import com.main.repository.*;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.io.InputStream;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Component
//public class DataSeeder implements CommandLineRunner {
//
//    private final RecipeRepository recipeRepo;
//    private final IngredientRepository ingredientRepo;
//    private final UserRepository userRepo;
//    private final PasswordEncoder passwordEncoder;
//    private final ObjectMapper mapper;
//
//    // Constructor injection for all dependencies
//    public DataSeeder(RecipeRepository recipeRepo,
//                      IngredientRepository ingredientRepo,
//                      UserRepository userRepo,
//                      PasswordEncoder passwordEncoder,
//                      ObjectMapper mapper) {
//        this.recipeRepo = recipeRepo;
//        this.ingredientRepo = ingredientRepo;
//        this.userRepo = userRepo;
//        this.passwordEncoder = passwordEncoder;
//        this.mapper = mapper;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        seedOwnerAccount();              // ensure there’s an admin
//        seedBase("data/recipes.json");   // create if missing
//        applyPatches("data/recipes.override.json"); // update-only if present
//    }
//
//    // --- 🧠 NEW METHOD: create default owner/admin if missing ---
//    private void seedOwnerAccount() {
//        if (userRepo.findByUsername("owner").isEmpty()) {
//            User admin = new User();
//            admin.setUsername("owner");
//            admin.setPassword(passwordEncoder.encode("owner123"));
//            admin.setRole(Role.ROLE_ADMIN);
//            userRepo.save(admin);
//            System.out.println("✅ Default owner account created: username=owner, password=owner123");
//        } else {
//            System.out.println("✅ Owner account already exists, skipping user creation.");
//        }
//    }
//
//    // --- EXISTING RECIPE SEEDING LOGIC BELOW THIS LINE ---
//
//    private void seedBase(String path) throws Exception {
//        ClassPathResource res = new ClassPathResource(path);
//        if (!res.exists()) {
//            System.out.println(path + " not found, skipping base seed.");
//            return;
//        }
//        try (InputStream is = res.getInputStream()) {
//            List<Map<String, Object>> list = mapper.readValue(is, new TypeReference<>() {});
//            for (Map<String, Object> rmap : list) {
//                String name = ((String) rmap.get("name")).trim();
//                Optional<Recipe> existing = recipeRepo.findByNameIgnoreCase(name);
//                if (existing.isPresent()) continue;
//
//                Recipe r = new Recipe();
//                r.setName(name);
//                r.setInstructions((String) rmap.get("instructions"));
//                r.setImageUrl((String) rmap.get("imageUrl"));
//
//                @SuppressWarnings("unchecked")
//                List<String> ingNames = (List<String>) rmap.get("ingredients");
//                if (ingNames != null) {
//                    for (String raw : ingNames) {
//                        String ingName = raw.trim().toLowerCase();
//                        Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//                            Ingredient i = new Ingredient();
//                            i.setName(ingName);
//                            return ingredientRepo.save(i);
//                        });
//                        RecipeIngredient ri = new RecipeIngredient();
//                        ri.setRecipe(r);
//                        ri.setIngredient(ing);
//                        r.getIngredients().add(ri);
//                    }
//                }
//                recipeRepo.save(r);
//            }
//        }
//    }
//
//    private void applyPatches(String path) throws Exception {
//        ClassPathResource res = new ClassPathResource(path);
//        if (!res.exists()) {
//            System.out.println(path + " not found, skipping patches.");
//            return;
//        }
//        try (InputStream is = res.getInputStream()) {
//            List<Map<String, Object>> patches = mapper.readValue(is, new TypeReference<>() {});
//            int updated = 0, created = 0;
//            for (Map<String, Object> p : patches) {
//                String name = ((String) p.get("name")).trim();
//                Optional<Recipe> opt = recipeRepo.findByNameIgnoreCase(name);
//                if (opt.isPresent()) {
//                    Recipe r = opt.get();
//                    if (p.containsKey("instructions")) r.setInstructions((String) p.get("instructions"));
//                    if (p.containsKey("imageUrl")) r.setImageUrl((String) p.get("imageUrl"));
//
//                    @SuppressWarnings("unchecked")
//                    List<String> ingNames = (List<String>) p.get("ingredients");
//                    if (ingNames != null) {
//                        Set<String> existing = r.getIngredients().stream()
//                                .map(ri -> ri.getIngredient().getName())
//                                .collect(Collectors.toSet());
//                        for (String raw : ingNames) {
//                            String ingName = raw.trim().toLowerCase();
//                            if (existing.contains(ingName)) continue;
//                            Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//                                Ingredient i = new Ingredient();
//                                i.setName(ingName);
//                                return ingredientRepo.save(i);
//                            });
//                            RecipeIngredient ri = new RecipeIngredient();
//                            ri.setRecipe(r);
//                            ri.setIngredient(ing);
//                            r.getIngredients().add(ri);
//                        }
//                    }
//                    recipeRepo.save(r);
//                    updated++;
//                } else {
//                    @SuppressWarnings("unchecked")
//                    List<String> ingNames = (List<String>) p.get("ingredients");
//                    if (ingNames == null) continue;
//
//                    Recipe r = new Recipe();
//                    r.setName(name);
//                    r.setInstructions((String) p.get("instructions"));
//                    r.setImageUrl((String) p.get("imageUrl"));
//
//                    for (String raw : ingNames) {
//                        String ingName = raw.trim().toLowerCase();
//                        Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//                            Ingredient i = new Ingredient();
//                            i.setName(ingName);
//                            return ingredientRepo.save(i);
//                        });
//                        RecipeIngredient ri = new RecipeIngredient();
//                        ri.setRecipe(r);
//                        ri.setIngredient(ing);
//                        r.getIngredients().add(ri);
//                    }
//                    recipeRepo.save(r);
//                    created++;
//                }
//            }
//            System.out.printf("Patches applied: updated=%d, created=%d%n", updated, created);
//        }
//    }
//}

//package com.main.seed;
//
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.main.model.Ingredient;
//import com.main.model.Recipe;
//import com.main.model.RecipeIngredient;
//import com.main.repository.IngredientRepository;
//import com.main.repository.RecipeRepository;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Component;
//
//import java.io.InputStream;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//@Component
//public class DataSeeder implements CommandLineRunner {
//
//private final RecipeRepository recipeRepo;
//private final IngredientRepository ingredientRepo;
//private final ObjectMapper mapper;
//
//// Explicit constructor injection (no Lombok)
//public DataSeeder(RecipeRepository recipeRepo,
//IngredientRepository ingredientRepo,
//ObjectMapper mapper) {
//this.recipeRepo = recipeRepo;
//this.ingredientRepo = ingredientRepo;
//this.mapper = mapper;
//}
//
//@Override
//public void run(String... args) throws Exception {
//seedBase("data/recipes.json"); // create if missing
//applyPatches("data/recipes.override.json"); // update-only if present
//}
//
//private void seedBase(String path) throws Exception {
//ClassPathResource res = new ClassPathResource(path);
//if (!res.exists()) {
//System.out.println(path + " not found, skipping base seed.");
//return;
//}
//try (InputStream is = res.getInputStream()) {
//List<Map<String, Object>> list = mapper.readValue(is, new TypeReference<>() {});
//for (Map<String, Object> rmap : list) {
//String name = ((String) rmap.get("name")).trim();
//Optional<Recipe> existing = recipeRepo.findByNameIgnoreCase(name);
//if (existing.isPresent()) continue; // don’t overwrite here
//
//
//
//    Recipe r = new Recipe();
//    r.setName(name);
//    r.setInstructions((String) rmap.get("instructions"));
//    r.setImageUrl((String) rmap.get("imageUrl")); // optional
//
//    @SuppressWarnings("unchecked")
//    List<String> ingNames = (List<String>) rmap.get("ingredients");
//    if (ingNames != null) {
//      for (String raw : ingNames) {
//        String ingName = raw.trim().toLowerCase();
//        Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//          Ingredient i = new Ingredient();
//          i.setName(ingName);
//          return ingredientRepo.save(i);
//        });
//        RecipeIngredient ri = new RecipeIngredient();
//        ri.setRecipe(r);
//        ri.setIngredient(ing);
//        r.getIngredients().add(ri);
//      }
//    }
//    recipeRepo.save(r);
//  }
//}
//}
//
//private void applyPatches(String path) throws Exception {
//ClassPathResource res = new ClassPathResource(path);
//if (!res.exists()) {
//System.out.println(path + " not found, skipping patches.");
//return;
//}
//try (InputStream is = res.getInputStream()) {
//List<Map<String, Object>> patches = mapper.readValue(is, new TypeReference<>() {});
//int updated = 0, created = 0;
//for (Map<String, Object> p : patches) {
//String name = ((String) p.get("name")).trim();
//Optional<Recipe> opt = recipeRepo.findByNameIgnoreCase(name);
//if (opt.isPresent()) {
//Recipe r = opt.get();
//if (p.containsKey("instructions")) r.setInstructions((String) p.get("instructions"));
//if (p.containsKey("imageUrl")) r.setImageUrl((String) p.get("imageUrl"));
//
//
//      @SuppressWarnings("unchecked")
//      List<String> ingNames = (List<String>) p.get("ingredients");
//      if (ingNames != null) {
//        Set<String> existing = r.getIngredients().stream()
//          .map(ri -> ri.getIngredient().getName())
//          .collect(Collectors.toSet());
//        for (String raw : ingNames) {
//          String ingName = raw.trim().toLowerCase();
//          if (existing.contains(ingName)) continue;
//          Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//            Ingredient i = new Ingredient();
//            i.setName(ingName);
//            return ingredientRepo.save(i);
//          });
//          RecipeIngredient ri = new RecipeIngredient();
//          ri.setRecipe(r);
//          ri.setIngredient(ing);
//          r.getIngredients().add(ri);
//        }
//      }
//      recipeRepo.save(r);
//      updated++;
//    } else {
//      // Optionally create new recipe if patch includes ingredients
//      @SuppressWarnings("unchecked")
//      List<String> ingNames = (List<String>) p.get("ingredients");
//      if (ingNames == null) continue;
//
//      Recipe r = new Recipe();
//      r.setName(name);
//      r.setInstructions((String) p.get("instructions"));
//      r.setImageUrl((String) p.get("imageUrl"));
//
//      for (String raw : ingNames) {
//        String ingName = raw.trim().toLowerCase();
//        Ingredient ing = ingredientRepo.findByName(ingName).orElseGet(() -> {
//          Ingredient i = new Ingredient();
//          i.setName(ingName);
//          return ingredientRepo.save(i);
//        });
//        RecipeIngredient ri = new RecipeIngredient();
//        ri.setRecipe(r);
//        ri.setIngredient(ing);
//        r.getIngredients().add(ri);
//      }
//      recipeRepo.save(r);
//      created++;
//    }
//  }
//  System.out.printf("Patches applied: updated=%d, created=%d%n", updated, created);
//}
//}
//}