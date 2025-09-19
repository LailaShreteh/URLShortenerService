package com.laila.service;

import com.laila.dto.UrlDto;
import com.laila.entities.Url;
import com.laila.exception.EntityNotFoundException;


import com.laila.repository.UrlRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
public class UrlService {

    private static final int CODE_LENGTH = 8;   // 7 is OK; 8 = virtually zero retries
    private static final int MAX_RETRIES = 5;

    private final UrlRepository repo;
    private final UrlCache cache;

    public UrlService(UrlRepository repo, UrlCache cache) {
        this.repo = repo;
        this.cache = cache;
    }

    /** Create a short code (random Base62).  */
    public String convertToShortUrl(UrlDto req) {
        if (req == null || !StringUtils.hasText(req.getLongUrl())) {
            throw new IllegalArgumentException("Url is required !");
        }

        // 1) Custom alias path: DB enforces uniqueness on code
        if (StringUtils.hasText(req.getAlias())) {
            Url e = new Url();
            e.setCode(req.getAlias().trim());
            e.setLongUrl(normalize(req.getLongUrl()));
            e.setCustom(true);
            e.setCreatedAt(Instant.now());
            e.setExpiresAt(req.getExpirationDate());

            Url saved = repo.save(e);
            cache.set(saved.getCode(), saved.getLongUrl(), saved.getExpiresAt());
            return saved.getCode();
        }

        // 2) Auto-generate random code and retry on very-rare duplicate
        DataIntegrityViolationException last = null;
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = Base62Generator.randomCode(CODE_LENGTH);
            try {
                Url e = new Url();
                e.setCode(code);
                e.setLongUrl(normalize(req.getLongUrl()));
                e.setCustom(false);
                e.setCreatedAt(Instant.now());
                e.setExpiresAt(req.getExpirationDate());

                Url saved = repo.save(e);
                cache.set(saved.getCode(), saved.getLongUrl(), saved.getExpiresAt());
                return saved.getCode();
            } catch (DataIntegrityViolationException dup) {
                last = dup; // collision on code (extremely rare) — try again
            }
        }
        throw last != null ? last : new IllegalStateException("Failed to create short code");
    }

    /** Resolve a short code → original URL. Uses Redis cache first, DB on miss, then warms cache. */
    public String getOriginalUrl(String code) {
        String cached = cache.get(code);
        if (cached != null) return cached;

        Url e = repo.findById(code)
                .filter(it -> it.getExpiresAt() == null || Instant.now().isBefore(it.getExpiresAt()))
                .orElseThrow(() -> new EntityNotFoundException("URL not found or expired: " + code));

        cache.set(e.getCode(), e.getLongUrl(), e.getExpiresAt());
        return e.getLongUrl();
    }

    // add http:// when scheme is missing
    private String normalize(String u) {
        String s = u.trim();
        String lower = s.toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "http://" + s;
        }
        return s;
    }
}