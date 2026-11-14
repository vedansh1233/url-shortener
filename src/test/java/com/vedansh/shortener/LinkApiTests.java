package com.vedansh.shortener;

import com.vedansh.shortener.link.ShortLink;
import com.vedansh.shortener.link.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LinkApiTests {

    @Autowired MockMvc mvc;
    @Autowired ShortLinkRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private void create(String json) throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void shortenRedirectAndCountClicks() throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/some/long/path?q=1\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value(matchesPattern("^[A-Za-z0-9]{7}$")))
                .andExpect(jsonPath("$.clicks").value(0));

        String code = repository.findAll().getFirst().getCode();
        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/some/long/path?q=1"));
        mvc.perform(get("/" + code)).andExpect(status().isFound());

        mvc.perform(get("/api/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(2))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/" + code))
                .andExpect(jsonPath("$.lastAccessedAt").exists());
    }

    @Test
    void customAliasMustBeUnique() throws Exception {
        create("{\"url\":\"https://github.com\",\"alias\":\"my-gh\"}");
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://gitlab.com\",\"alias\":\"my-gh\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Alias 'my-gh' is already taken"));
    }

    @Test
    void rejectsInvalidInput() throws Exception {
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://files.example.com\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\",\"alias\":\"a b\",\"expiresInDays\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.url").exists())
                .andExpect(jsonPath("$.fieldErrors.alias").exists())
                .andExpect(jsonPath("$.fieldErrors.expiresInDays").exists());
        mvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"alias\":\"api\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAndExpiredLinks() throws Exception {
        mvc.perform(get("/nope123")).andExpect(status().isNotFound());

        ShortLink link = new ShortLink("old-link", "https://example.com", Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-02-01T00:00:00Z"));
        repository.save(link);
        mvc.perform(get("/old-link")).andExpect(status().isGone());
        mvc.perform(get("/api/links/old-link")).andExpect(jsonPath("$.expired").value(true));
    }

    @Test
    void listAndDelete() throws Exception {
        create("{\"url\":\"https://a.example.com\",\"alias\":\"first\"}");
        create("{\"url\":\"https://b.example.com\",\"expiresInDays\":7}");
        mvc.perform(get("/api/links")).andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(delete("/api/links/first")).andExpect(status().isNoContent());
        mvc.perform(get("/api/links")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(delete("/api/links/first")).andExpect(status().isNotFound());
    }

    @Test
    void servesHomePageAndApiDocs() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
