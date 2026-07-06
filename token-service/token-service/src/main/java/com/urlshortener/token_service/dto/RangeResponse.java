package com.urlshortener.token_service.dto;



public class RangeResponse {
    private Long start;
    private Long end;

    public RangeResponse() {}
    public RangeResponse(Long start, Long end) {
        this.start = start;
        this.end = end;
    }

    public Long getStart() { return start; }
    public void setStart(Long start) { this.start = start; }
    public Long getEnd() { return end; }
    public void setEnd(Long end) { this.end = end; }
}