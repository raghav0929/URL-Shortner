package com.urlshortener.token_service.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urlshortener.token_service.dto.RangeResponse;
import com.urlshortener.token_service.entity.TokenRange;
import com.urlshortener.token_service.repository.TokenRangeRepository;

import java.time.LocalDateTime;

@Service
public class TokenAllocationService {

    @Autowired
    private TokenRangeRepository repository;

    @Transactional
    public RangeResponse allocateRange() {
        TokenRange range = repository.findByIdForUpdate()
                .orElseThrow(() -> new IllegalStateException("Token range row missing — did you seed it?"));

        long start = range.getNextAvailable();
        long end = start + range.getRangeSize() - 1;

        range.setNextAvailable(end + 1);
        range.setUpdatedAt(LocalDateTime.now());
        repository.save(range);

        return new RangeResponse(start, end);
    }
}