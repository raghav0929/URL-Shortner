package com.urlshortener.url_shortener_service.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.urlshortener.url_shortener_service.entity.UrlMapping;
import com.urlshortener.url_shortener_service.exception.ShortCodeNotFoundException;
import com.urlshortener.url_shortener_service.repository.UrlMappingRepository;
import com.urlshortener.url_shortener_service.util.Base62Encoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class UrlShortenerService {

    @Autowired
    private IdRangeHolder idRangeHolder;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    public String shorten(String originalUrl) {
        long id = idRangeHolder.nextId();
        String shortCode = Base62Encoder.encode(id);

        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);
        mapping.setCreatedAt(Instant.now());
        mapping.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        mapping.setClickCount(0L);

        urlMappingRepository.save(mapping);
        return shortCode;
    }

    @Cacheable(value = "urlCache", key = "#shortCode")
    public UrlMapping resolve(String shortCode) {
    	return urlMappingRepository.findById(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }
    
    @Async
    public void trackClick(String shortCode) {
        urlMappingRepository.findById(shortCode).ifPresent(mapping -> {
            mapping.setClickCount(mapping.getClickCount() + 1);
            urlMappingRepository.save(mapping);
        });
    }

	public UrlMapping getStats(String shortCode) {
		return urlMappingRepository.findById(shortCode)
				.orElseThrow(()-> new ShortCodeNotFoundException(shortCode));
	}
}