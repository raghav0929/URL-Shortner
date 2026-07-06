package com.urlshortener.token_service.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urlshortener.token_service.dto.RangeResponse;
import com.urlshortener.token_service.service.TokenAllocationService;

@RestController
@RequestMapping("/api/v1/token")
public class TokenController {

    @Autowired
    private TokenAllocationService service;

    @PostMapping("/range")
    public ResponseEntity<RangeResponse> getRange() {
        return ResponseEntity.ok(service.allocateRange());
    }
}