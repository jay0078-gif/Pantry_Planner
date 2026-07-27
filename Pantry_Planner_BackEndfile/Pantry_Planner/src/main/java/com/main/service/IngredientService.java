package com.main.service;

import com.main.model.Ingredient;
import com.main.repository.IngredientRepository;
import org.springframework.stereotype.Service;

@Service
public class IngredientService {

  private final IngredientRepository repo;

  // Constructor injection initializes the final field
  public IngredientService(IngredientRepository repo) {
    this.repo = repo;
  }

  public Ingredient findOrCreate(String rawName) {
    String name = rawName.trim().toLowerCase();
    return repo.findByName(name).orElseGet(() -> {
      Ingredient i = new Ingredient();
      i.setName(name);
      return repo.save(i);
    });
  }
}