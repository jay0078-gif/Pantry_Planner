package com.main.controller;

import com.main.dto.PurchaseDTO;
import com.main.model.Purchase;
import com.main.repository.PurchaseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseRepository repo;

    public PurchaseController(PurchaseRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDTO> get(@PathVariable Long id, Principal principal) {
        return repo.findByIdAndUser_Username(id, principal.getName())
                .map(purchase -> new PurchaseDTO(
                        purchase.getId(),
                        purchase.getIngredient().getId(),
                        purchase.getIngredient().getName(),
                        purchase.getQuantity(),
                        purchase.getUnit(),
                        purchase.getPrice(),
                        purchase.getPurchasedAt()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
