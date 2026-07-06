package com.urlshortener.url_shortener_service.repository;


import org.springframework.data.cassandra.repository.CassandraRepository;

import com.urlshortener.url_shortener_service.entity.UrlMapping;

public interface UrlMappingRepository extends CassandraRepository<UrlMapping, String> {
}