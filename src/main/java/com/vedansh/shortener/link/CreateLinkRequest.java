package com.vedansh.shortener.link;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLinkRequest(
        @Schema(example = "https://github.com/vedansh1233")
        @NotBlank @Size(max = 2048) String url,

        @Schema(description = "Optional custom code (3-16 letters, digits, - or _)", example = "my-github")
        @Pattern(regexp = "^[A-Za-z0-9_-]{3,16}$", message = "must be 3-16 letters, digits, - or _")
        String alias,

        @Schema(description = "Optional lifetime in days (1-365)", example = "30")
        @Min(1) @Max(365) Integer expiresInDays
) {
    public CreateLinkRequest {
        url = url == null ? null : url.trim();
        alias = alias == null || alias.isBlank() ? null : alias.trim();
    }
}
