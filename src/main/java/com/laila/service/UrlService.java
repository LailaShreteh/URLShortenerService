package com.laila.service;

import com.laila.dto.UrlDto;
import com.laila.entities.Url;
import com.laila.exception.EntityNotFoundException;
import com.laila.repository.UrlRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;

@Service
public class UrlService {
    private final UrlRepository urlRepository;
    private final Base62Converter base62Converter;
    private final StringRedisTemplate redisTemplate;
    private final IdSequence idSequence;

    private static final String CODE_KEY_PREFIX = "url:code:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(1);

    public UrlService(UrlRepository urlRepository,
                      Base62Converter base62Converter,
                      StringRedisTemplate redisTemplate,
                      IdSequence idSequence) {
        this.urlRepository = urlRepository;
        this.base62Converter = base62Converter;
        this.redisTemplate = redisTemplate;
        this.idSequence = idSequence;
    }

    /** Creates a short code for the given long URL (normalizes scheme, writes through cache). */
    public String convertToShortUrl(UrlDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getLongUrl())) {
            throw new IllegalArgumentException("longUrl is required");
        }

        // normalize long URL
        String normalized = normalize(dto.getLongUrl());

        // generate numeric id (keeps Base62 algo unchanged)
        long id = idSequence.next();

        // build entity
        Url url = new Url();
        url.setId(id);
        url.setLongUrl(normalized);
        if (StringUtils.hasText(dto.getUserId())) {
            url.setUserId(dto.getUserId());
        }
        url.setCreatedDate(Instant.now());
        if (dto.getTtlSeconds() != null && dto.getTtlSeconds() > 0) {
            url.setTtlSeconds(dto.getTtlSeconds());
        }

        // persist
        urlRepository.save(url);

        // encode and cache
        String code = base62Converter.encode(id);
        Duration ttl = (url.getTtlSeconds() != null && url.getTtlSeconds() > 0)
                ? Duration.ofSeconds(url.getTtlSeconds())
                : DEFAULT_CACHE_TTL;

        redisTemplate.opsForValue().set(CODE_KEY_PREFIX + code, normalized, ttl);

        return code;
    }

    /** Resolves a short code back to the original URL (reads cache first, repopulates if miss). */
    public String getOriginalUrl(String code) {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // cache read (your test verifies this call)
        String cached = ops.get(CODE_KEY_PREFIX + code);
        if (cached != null) {
            return cached;
        }

        long id = base62Converter.decode(code);
        Url url = urlRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("URL not found for code: " + code));

        Duration ttl = (url.getTtlSeconds() != null && url.getTtlSeconds() > 0)
                ? Duration.ofSeconds(url.getTtlSeconds())
                : DEFAULT_CACHE_TTL;

        ops.set(CODE_KEY_PREFIX + code, url.getLongUrl(), ttl);
        return url.getLongUrl();
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