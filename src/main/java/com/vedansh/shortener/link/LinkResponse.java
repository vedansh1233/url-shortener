package com.vedansh.shortener.link;

import java.time.Instant;

public record LinkResponse(
        String code,
        String shortUrl,
        String targetUrl,
        long clicks,
        Instant createdAt,
        Instant expiresAt,
        Instant lastAccessedAt,
        boolean expired
) {
    public static LinkResponse from(ShortLink link, String baseUrl, Instant now) {
        return new LinkResponse(link.getCode(), baseUrl + "/" + link.getCode(), link.getTargetUrl(),
                link.getClicks(), link.getCreatedAt(), link.getExpiresAt(), link.getLastAccessedAt(),
                link.isExpired(now));
    }
}
