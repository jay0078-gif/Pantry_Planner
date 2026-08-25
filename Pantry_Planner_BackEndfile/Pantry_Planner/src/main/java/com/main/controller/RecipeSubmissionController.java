package com.main.controller;

import com.main.dto.RecipeSubmissionRequest;
import com.main.exception.ResourceNotFoundException;
import com.main.model.RecipeSubmission;
import com.main.model.User;
import com.main.repository.RecipeSubmissionRepository;
import com.main.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class RecipeSubmissionController {

    private final RecipeSubmissionRepository submissionRepository;
    private final UserRepository userRepository;

    public RecipeSubmissionController(
            RecipeSubmissionRepository submissionRepository,
            UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/recipes")
    public ResponseEntity<Map<String, String>> submitRecipe(
            @Valid @RequestBody RecipeSubmissionRequest request,
            Principal principal) {
        User user = getUser(principal);

        RecipeSubmission submission = new RecipeSubmission();
        submission.setTitle(request.title().trim());
        submission.setInstructions(request.instructions().trim());
        submission.setIngredients(request.ingredients().stream().map(String::trim).toList());
        submission.setSubmittedBy(user);
        submissionRepository.save(submission);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Recipe submitted for approval"));
    }

    @GetMapping("/my-submissions")
    public List<RecipeSubmission> mySubmissions(Principal principal) {
        return submissionRepository.findBySubmittedBy(getUser(principal));
    }

    private User getUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }
}
