# Snip: URL Shortener

A URL shortener with click analytics, built with **Java 21** and **Spring Boot 3.5**. Paste a long link, get a short one, share it, and see how many times it was clicked.

## Features

- Shorten any `http`/`https` URL to a random 7-character code, or pick a custom alias (`/my-github`)
- Following a short link gives a **302 redirect** and counts the click
- Click counts are updated with an **atomic SQL update**, so concurrent clicks are never lost
- Optional expiry (1–365 days); expired links return **410 Gone**
- Codes avoid look-alike characters (`0/O`, `1/l/I`) and are generated with `SecureRandom`
- Clean JSON errors with per-field validation messages
- Built-in web page (no build step) and Swagger UI docs
- 6 integration tests (MockMvc + H2)

## Tech stack

Java 21 · Spring Boot 3.5 · Spring Web · Spring Data JPA (Hibernate) · Bean Validation · H2 / PostgreSQL · springdoc OpenAPI · JUnit 5 + MockMvc

## Run it

Requires Java 21+. No database setup needed: an embedded H2 database is stored in `./data`.

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

- App: http://localhost:8081
- API docs: http://localhost:8081/swagger-ui.html

Run the tests with `./mvnw test`.

To use PostgreSQL instead, set `DB_URL=jdbc:postgresql://localhost:5432/shortener`, `DB_USERNAME` and `DB_PASSWORD`.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/links` | Shorten a URL: `{ "url": "...", "alias": "optional", "expiresInDays": 30 }` → 201 |
| GET | `/api/links` | 100 most recent links with click counts |
| GET | `/api/links/{code}` | One link and its statistics |
| DELETE | `/api/links/{code}` | Delete a link → 204 |
| GET | `/{code}` | Redirect to the original URL (302), counting a click |

Errors: 400 validation, 404 unknown code, 409 alias taken, 410 expired.

```bash
curl -X POST localhost:8081/api/links -H "Content-Type: application/json" \
     -d '{"url":"https://github.com/vedansh1233","alias":"vedansh"}'
curl -i localhost:8081/vedansh     # HTTP/1.1 302, Location: https://github.com/vedansh1233
```

## Project structure

```
src/main/java/com/vedansh/shortener
├── link/     ShortLink entity, repository, service, REST + redirect controllers, DTOs
├── error/    ApiException and the global JSON error handler
└── config/   Clock and OpenAPI beans
src/main/resources/static/index.html   the web UI
```

---
Built by [Vedansh Trivedi](https://vedansh1233.github.io)
