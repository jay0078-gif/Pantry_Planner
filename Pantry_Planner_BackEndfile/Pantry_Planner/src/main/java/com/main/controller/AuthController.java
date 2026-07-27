package com.main.controller;

import com.main.model.User;
import com.main.model.Role;
import com.main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ Register a normal user
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    // ✅ Optional: Register an admin/owner manually
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ROLE_ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok("Admin registered successfully");
    }

    // ✅ Endpoint used by frontend to check who is logged in
    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> currentUser(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(null);
        }
        Map<String, String> info = new HashMap<>();
        info.put("username", auth.getName());
        info.put("role", auth.getAuthorities().iterator().next().getAuthority());
        return ResponseEntity.ok(info);
    }
}