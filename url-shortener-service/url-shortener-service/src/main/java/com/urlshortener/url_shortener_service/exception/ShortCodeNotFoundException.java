package com.urlshortener.url_shortener_service.exception;

public class ShortCodeNotFoundException extends RuntimeException{

    public ShortCodeNotFoundException(String shortCode) {
        super("Short code not found: " + shortCode);
    }
}
