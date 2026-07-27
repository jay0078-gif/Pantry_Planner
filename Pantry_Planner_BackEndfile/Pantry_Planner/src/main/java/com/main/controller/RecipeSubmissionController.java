package com.main.controller;

import com.main.model.RecipeSubmission;
import com.main.model.User;
import com.main.repository.RecipeSubmissionRepository;
import com.main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class RecipeSubmissionController {

    @Autowired
    private RecipeSubmissionRepository submissionRepo;

    @Autowired
    private UserRepository userRepo;

    // ✅ USER: submit a recipe for approval
    @PostMapping("/recipes")
    public ResponseEntity<String> submitRecipe(@RequestBody RecipeSubmission submission, Principal principal) {
        // identify current user
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        submission.setSubmittedBy(user);
        submission.setApproved(false);
        submissionRepo.save(submission);

        return ResponseEntity.ok("Recipe submitted for approval!");
    }

    // ✅ USER: get only this user's submissions
    @GetMapping("/my-submissions")
    public List<RecipeSubmission> mySubmissions(Principal principal) {
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        // Direct query instead of filtering everything in memory
        return submissionRepo.findBySubmittedBy(user);
    }
}