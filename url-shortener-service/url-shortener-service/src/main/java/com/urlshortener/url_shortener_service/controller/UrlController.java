package com.urlshortener.url_shortener_service.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.urlshortener.url_shortener_service.entity.UrlMapping;
import com.urlshortener.url_shortener_service.service.UrlShortenerService;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class UrlController {

    @Autowired
    private UrlShortenerService service;

    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shorten(@RequestBody Map<String, String> body) {
        String originalUrl = body.get("original_url");
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("original_url is required");
        }
        String shortCode = service.shorten(originalUrl);
        return ResponseEntity.ok(Map.of("short_code", shortCode));
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        UrlMapping mapping = service.resolve(shortCode);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(mapping.getOriginalUrl()))
                .build();
    }
}