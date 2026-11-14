package com.vedansh.shortener.link;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/links")
@Tag(name = "Links", description = "Create and manage short links")
public class LinkController {

    private final LinkService service;

    public LinkController(LinkService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Shorten a URL")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse body = LinkResponse.from(service.create(request), baseUrl(), service.now());
        return ResponseEntity.created(URI.create(baseUrl() + "/api/links/" + body.code())).body(body);
    }

    @GetMapping
    @Operation(summary = "List the 100 most recent links")
    public List<LinkResponse> list() {
        String base = baseUrl();
        return service.recent().stream().map(l -> LinkResponse.from(l, base, service.now())).toList();
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a link and its click statistics")
    public LinkResponse get(@PathVariable String code) {
        return LinkResponse.from(service.get(code), baseUrl(), service.now());
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a link")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private static String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
