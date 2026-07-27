package com.main.controller;

import com.main.dto.PurchaseDTO;
import com.main.model.Purchase;
import com.main.repository.PurchaseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

private final PurchaseRepository repo;

public PurchaseController(PurchaseRepository repo) {
this.repo = repo;
}

@GetMapping("/{id}")
public ResponseEntity<PurchaseDTO> get(@PathVariable Long id) {
return repo.findById(id)
.map(p -> new PurchaseDTO(
p.getId(),
p.getIngredient().getId(),
p.getIngredient().getName(),
p.getQuantity(),
p.getUnit(),
p.getPrice(),
p.getPurchasedAt()
))
.map(ResponseEntity::ok)
.orElse(ResponseEntity.notFound().build());
}
}
