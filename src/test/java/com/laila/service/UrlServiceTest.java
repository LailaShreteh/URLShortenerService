package com.laila.service;

import com.laila.entities.Url;
import com.laila.exception.EntityNotFoundException;
import com.laila.dto.UrlDto;
import com.laila.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;


import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock UrlRepository repo;
    @Mock UrlCache cache;

    @InjectMocks UrlService service;

    @Test
    void create_withAlias_persists_and_warmsCache_noGenerator() {
        UrlDto dto = new UrlDto();
        dto.setLongUrl("https://www.wikipedia.org/");
        dto.setAlias("docs123");
        dto.setExpirationDate(Instant.parse("2026-12-31T23:59:59Z"));

        // repo.save returns the passed entity
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = service.convertToShortUrl(dto);

        assertEquals("docs123", code);
        // saved with alias as PK and normalized long url
        ArgumentCaptor<Url> cap = ArgumentCaptor.forClass(Url.class);
        verify(repo).save(cap.capture());
        assertEquals("docs123", cap.getValue().getCode());
        assertEquals("https://www.wikipedia.org/", cap.getValue().getLongUrl());
        assertEquals(dto.getExpirationDate(), cap.getValue().getExpiresAt());

        // cache warmed with expiresAt
        verify(cache).set("docs123", "https://www.wikipedia.org/", dto.getExpirationDate());
    }

    @Test
    void create_autoGenerates_randomCode_persists_and_warmsCache() {
        UrlDto dto = new UrlDto();
        dto.setLongUrl("wikipedia.org"); // will be normalized to http://wikipedia.org
        dto.setExpirationDate(Instant.parse("2027-01-01T00:00:00Z"));

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = Mockito.mockStatic(Base62Generator.class)) {
            mocked.when(() -> Base62Generator.randomCode(anyInt())).thenReturn("aB9x2Q7");

            String code = service.convertToShortUrl(dto);
            assertEquals("aB9x2Q7", code);

            ArgumentCaptor<Url> cap = ArgumentCaptor.forClass(Url.class);
            verify(repo).save(cap.capture());
            assertEquals("aB9x2Q7", cap.getValue().getCode());
            assertEquals("http://wikipedia.org", cap.getValue().getLongUrl()); // normalized
            assertEquals(dto.getExpirationDate(), cap.getValue().getExpiresAt());

            verify(cache).set("aB9x2Q7", "http://wikipedia.org", dto.getExpirationDate());
        }
    }

    @Test
    void create_retries_on_duplicateKey_then_succeeds() {
        UrlDto dto = new UrlDto();
        dto.setLongUrl("https://example.com/page");

        // First save throws duplicate key (code collision), second succeeds
        when(repo.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate code"))
                .thenAnswer(inv -> inv.getArgument(0));

        try (var mocked = Mockito.mockStatic(Base62Generator.class)) {
            mocked.when(() -> Base62Generator.randomCode(anyInt()))
                    .thenReturn("dupCode", "okCode");

            String code = service.convertToShortUrl(dto);
            assertEquals("okCode", code);
        }

        // verify two attempts to save
        verify(repo, times(2)).save(any(Url.class));
        // cache warmed with the winning code
        verify(cache).set(eq("okCode"), eq("https://example.com/page"), isNull());
    }

    @Test
    void resolve_cacheHit_returnsImmediately() {
        when(cache.get("abc")).thenReturn("https://target.com/");

        String url = service.getOriginalUrl("abc");

        assertEquals("https://target.com/", url);
        verifyNoInteractions(repo);
    }

    @Test
    void resolve_cacheMiss_dbHit_then_warmCache_and_return() {
        when(cache.get("xyz")).thenReturn(null);

        Url e = new Url();
        e.setCode("xyz");
        e.setLongUrl("https://target.com/");
        e.setExpiresAt(Instant.parse("2030-01-01T00:00:00Z"));

        when(repo.findById("xyz")).thenReturn(Optional.of(e));

        String url = service.getOriginalUrl("xyz");

        assertEquals("https://target.com/", url);
        verify(cache).set("xyz", "https://target.com/", e.getExpiresAt());
    }

    @Test
    void resolve_notFound_or_expired_throws() {
        when(cache.get("gone")).thenReturn(null);
        when(repo.findById("gone")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getOriginalUrl("gone"));
    }
}