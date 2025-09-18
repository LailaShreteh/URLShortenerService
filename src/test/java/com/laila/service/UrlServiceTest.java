package com.laila.service;

import com.laila.exception.EntityNotFoundException;
import com.laila.dto.UrlDto;
import com.laila.entities.Url;
import com.laila.repository.UrlRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class UrlServiceTest {

    @Mock
    UrlRepository mockUrlRepository;

    @Mock
    Base62Converter mockBase62Conversion;

    @Mock
    StringRedisTemplate mockRedisTemplate;

    @Mock
    ValueOperations<String, String> mockValueOps;

    @Mock
    IdSequence mockIdSequence;

    @InjectMocks
    UrlService urlService;

    @Test
    public void convertToShortUrl_usesIdSequence_Base62_andSetsExactTTL() {
        // Arrange
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockIdSequence.next()).thenReturn(5L);
        when(mockBase62Conversion.encode(5L)).thenReturn("f");

        // Capture the entity we persist
        ArgumentCaptor<Url> urlCaptor = ArgumentCaptor.forClass(Url.class);
        when(mockUrlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        // Request with explicit TTL (e.g., 1 hour)
        UrlDto dto = new UrlDto();
        dto.setLongUrl("https://www.wikipedia.org/");
        dto.setTtlSeconds(3600L);

        // Act
        String code = urlService.convertToShortUrl(dto);

        // Assert
        assertEquals("f", code);

        // 1) Id generator used
        verify(mockIdSequence).next();

        // 2) Base62 encode
        verify(mockBase62Conversion).encode(5L);

        // 3) Persisted with ttlSeconds = 3600 and created date set
        verify(mockUrlRepository).save(urlCaptor.capture());
        Url saved = urlCaptor.getValue();
        assertEquals(Long.valueOf(3600L), saved.getTtlSeconds());

        // 4) Cached with the exact TTL
        verify(mockValueOps).set(eq("url:code:f"),
                eq("https://www.wikipedia.org/"),
                eq(Duration.ofSeconds(3600)));
    }

    @Test
    public void getOriginalUrl_cacheMiss_decodesToId_loadsFromRepo_andRepopulatesCacheWithTTL() {
        // Arrange
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get("url:code:h")).thenReturn(null); // cache miss
        when(mockBase62Conversion.decode("h")).thenReturn(7L);

        Url entity = new Url();
        entity.setId(7L);
        entity.setLongUrl("https://www.wikipedia.org/");
        entity.setCreatedDate(Instant.now());
        entity.setTtlSeconds(300L);
        when(mockUrlRepository.findById(7L)).thenReturn(Optional.of(entity));

        // Act
        String original = urlService.getOriginalUrl("h");

        // Assert
        assertEquals("https://www.wikipedia.org/", original);
        verify(mockValueOps).get("url:code:h");
        verify(mockBase62Conversion).decode("h");
        verify(mockUrlRepository).findById(7L);

        // Service reuses the entity TTL when repopulating the cache:
        verify(mockValueOps).set(eq("url:code:h"),
                eq("https://www.wikipedia.org/"),
                eq(Duration.ofSeconds(300)));
    }

    @Test
    public void getOriginalUrl_cacheHit_returnsDirectlyWithoutRepo() {
        // Arrange
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get("url:code:h")).thenReturn("https://www.wikipedia.org/");

        // Act
        String original = urlService.getOriginalUrl("h");

        // Assert
        assertEquals("https://www.wikipedia.org/", original);
        verify(mockValueOps).get("url:code:h");
        verifyNoInteractions(mockUrlRepository);
    }

    @Test(expected = EntityNotFoundException.class)
    public void getOriginalUrl_expiredByTTL_behavesAsNotFound() {
        // Arrange: cache miss, decode ok, but repo returns empty because Redis TTL evicted it
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get("url:code:x")).thenReturn(null);
        when(mockBase62Conversion.decode("x")).thenReturn(42L);
        when(mockUrlRepository.findById(42L)).thenReturn(Optional.empty());

        // Act + Assert
        urlService.getOriginalUrl("x");
    }

    @Test
    public void convertToShortUrl_normalizesScheme_andSetsDefaultTTLIfMissing() {
        // Arrange
        when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockIdSequence.next()).thenReturn(9L);
        when(mockBase62Conversion.encode(9L)).thenReturn("j");
        when(mockUrlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlDto dto = new UrlDto();
        dto.setLongUrl("wikipedia.org"); // no scheme; service should normalize

        // Act
        String code = urlService.convertToShortUrl(dto);

        // Assert
        assertEquals("j", code);
        verify(mockIdSequence).next();
        verify(mockValueOps).set(eq("url:code:j"),
                eq("http://wikipedia.org"),
                any(Duration.class));
    }
}