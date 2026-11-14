package com.vedansh.shortener.link;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Redirect")
public class RedirectController {

    private final LinkService service;

    public RedirectController(LinkService service) {
        this.service = service;
    }

    /** Codes never contain a dot, so static files like /index.html are not matched. */
    @GetMapping("/{code:[A-Za-z0-9_-]{3,16}}")
    @Operation(summary = "Follow a short link (302 redirect, counts a click)")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        URI target = URI.create(service.resolve(code));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
