package com.main.controller;

import com.main.dto.SuggestionDTO;
import com.main.service.SuggestionService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
            Principal principal
    ) {
        return service.getSuggestionsByUsername(principal.getName(), maxMissing, limit);
    }
}
