package com.main.controller;

import com.main.dto.SuggestionDTO;
import com.main.service.SuggestionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService service;

    public SuggestionController(SuggestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<SuggestionDTO> get(
            @RequestParam(defaultValue = "2") int maxMissing,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal UserDetails user
    ) {
        // 🧩 get current username
        if (user == null) {
            // not logged in or missing principal → return empty list
            return Collections.emptyList();
        }

        try {
            // your service should look up the user’s id internally
            return service.getSuggestionsByUsername(user.getUsername(), maxMissing, limit);
        } catch (Exception e) {
            e.printStackTrace();
            // hide internal failure from frontend
            return Collections.emptyList();
        }
    }
}