package com.urlshortener.token_service.entity;



import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_ranges")
public class TokenRange {

    @Id
    private Long id;

    private Long nextAvailable;
    private Integer rangeSize;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNextAvailable() { return nextAvailable; }
    public void setNextAvailable(Long nextAvailable) { this.nextAvailable = nextAvailable; }

    public Integer getRangeSize() { return rangeSize; }
    public void setRangeSize(Integer rangeSize) { this.rangeSize = rangeSize; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}