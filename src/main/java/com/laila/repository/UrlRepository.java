package com.laila.repository;

import com.laila.entities.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<Url, String> {
    Optional<Url> findByUrlHash(String urlHash); // optional idempotency
}