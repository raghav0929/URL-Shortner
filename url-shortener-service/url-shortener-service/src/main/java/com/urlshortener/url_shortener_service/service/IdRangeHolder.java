package com.urlshortener.url_shortener_service.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.urlshortener.url_shortener_service.client.TokenServiceClient;
import com.urlshortener.url_shortener_service.dto.RangeResponse;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class IdRangeHolder {

    private final AtomicLong current = new AtomicLong(0);
    private volatile long rangeEnd = -1;

    @Autowired
    private TokenServiceClient tokenServiceClient;

    public synchronized long nextId() {
        if (current.get() > rangeEnd) {
            RangeResponse newRange = tokenServiceClient.getNewRange();
            current.set(newRange.getStart());
            rangeEnd = newRange.getEnd();
        }
        return current.getAndIncrement();
    }
}