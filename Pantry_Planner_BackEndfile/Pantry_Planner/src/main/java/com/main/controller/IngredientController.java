package com.main.controller;

import com.main.model.Ingredient;
import com.main.repository.IngredientRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

private final IngredientRepository repo;

// Constructor injection
public IngredientController(IngredientRepository repo) {
this.repo = repo;
}

@GetMapping
public List<Ingredient> search(@RequestParam(defaultValue = "") String search) {
if (search == null || search.isBlank()) {
return repo.findAll(PageRequest.of(0, 50)).getContent();
}
return repo.search(search.toLowerCase());
}
}


