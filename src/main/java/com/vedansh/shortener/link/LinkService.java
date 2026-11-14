package com.vedansh.shortener.link;

import com.vedansh.shortener.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LinkService {

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 7;
    /** Single-segment paths the app itself serves; a custom alias must not shadow them. */
    private static final Set<String> RESERVED = Set.of("api", "swagger-ui", "v3", "actuator", "error", "index");

    private final ShortLinkRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public LinkService(ShortLinkRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ShortLink create(CreateLinkRequest request) {
        String url = validateUrl(request.url());
        String code;
        if (request.alias() != null) {
            code = request.alias();
            if (RESERVED.contains(code.toLowerCase(Locale.ROOT))) {
                throw ApiException.badRequest("Alias '" + code + "' is reserved");
            }
            if (repository.existsByCode(code)) {
                throw ApiException.conflict("Alias '" + code + "' is already taken");
            }
        } else {
            code = generateUniqueCode();
        }
        Instant now = clock.instant();
        Instant expiresAt = request.expiresInDays() == null ? null : now.plus(Duration.ofDays(request.expiresInDays()));
        return repository.save(new ShortLink(code, url, now, expiresAt));
    }

    /** Looks up a code for a redirect and counts the click. */
    @Transactional
    public String resolve(String code) {
        ShortLink link = get(code);
        Instant now = clock.instant();
        if (link.isExpired(now)) {
            throw ApiException.gone("Link '" + code + "' has expired");
        }
        repository.registerClick(link.getId(), now);
        return link.getTargetUrl();
    }

    @Transactional(readOnly = true)
    public ShortLink get(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("No link with code '" + code + "'"));
    }

    @Transactional(readOnly = true)
    public List<ShortLink> recent() {
        return repository.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional
    public void delete(String code) {
        repository.delete(get(code));
    }

    public Instant now() {
        return clock.instant();
    }

    private String validateUrl(String raw) {
        try {
            URI uri = new URI(raw);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https") || uri.getHost() == null) {
                throw ApiException.badRequest("URL must start with http:// or https:// and include a host");
            }
            return uri.toString();
        } catch (URISyntaxException e) {
            throw ApiException.badRequest("Not a valid URL");
        }
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            String code = sb.toString();
            if (!repository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not generate a unique code");
    }
}
