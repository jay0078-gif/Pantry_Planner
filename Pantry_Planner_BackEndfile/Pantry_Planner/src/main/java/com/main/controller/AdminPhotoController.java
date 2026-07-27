package com.main.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.main.service.PhotoService;

@RestController
@RequestMapping("/api/admin/photos")
public class AdminPhotoController {

    @Autowired
    private PhotoService photoService;   // ✅ service class exists under com.main.service

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        String url = photoService.uploadPhoto(file);
        return ResponseEntity.ok(url);
    }
}