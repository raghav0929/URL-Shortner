package com.urlshortener.url_shortener_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.urlshortener.url_shortener_service.dto.RangeResponse;

@FeignClient(name = "token-service")
public interface TokenServiceClient {

    @PostMapping("/api/v1/token/range")
    RangeResponse getNewRange();
}