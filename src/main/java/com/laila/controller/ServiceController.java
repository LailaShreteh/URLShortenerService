package com.laila.controller;

import com.laila.dto.UrlDto;
import com.laila.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "URL Shortener")
@RestController
@RequestMapping("/")
public class ServiceController {

    private final UrlService urlService;

    public ServiceController(UrlService urlService) {
        this.urlService = urlService;
    }

    @Operation(
            summary = "Convert new url",
            description = "Converts a long URL to a short code"
    )
    @ApiResponse(responseCode = "200", description = "Short code created")
    @PostMapping(
            path = "/create-short",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public ResponseEntity<String> convertToShortUrl(@RequestBody @Valid UrlDto request) {
        String code = urlService.convertToShortUrl(request);
        return ResponseEntity.ok(code);
    }

    @Operation(
            summary = "Redirect",
            description = "Finds original URL from short code and redirects"
    )
    @ApiResponse(responseCode = "302", description = "Redirected to original URL")
    @GetMapping("{shortUrl}")
    public ResponseEntity<Void> getAndRedirect(@PathVariable String shortUrl) {
        String url = urlService.getOriginalUrl(shortUrl);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}