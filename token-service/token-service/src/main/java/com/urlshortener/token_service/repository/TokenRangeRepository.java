package com.urlshortener.token_service.repository;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.urlshortener.token_service.entity.TokenRange;

import java.util.Optional;

public interface TokenRangeRepository extends JpaRepository<TokenRange, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenRange t WHERE t.id = 1")
    Optional<TokenRange> findByIdForUpdate();
}