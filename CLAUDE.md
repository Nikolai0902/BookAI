# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BookAI** is a Spring Boot 4.0.3 backend application (Java 25, Maven). Currently in early stages with the core package structure laid out but no production code beyond the entry point.

- **Group ID:** `io.book.ai`
- **Main class:** `io.book.ai.BookAiApplication`

## Commands

```bash
# Run the application
mvn spring-boot:run

# Build (produces executable JAR in target/)
mvn clean package

# Run tests
mvn test

# Compile only
mvn clean compile
```

## Architecture

The package layout under `src/main/java/io/book/ai/` follows a layered structure:

- `controller/` — REST endpoints (`@RestController`)
- `handler/` — business logic layer
- `repository/` — data access layer (e.g., Spring Data repositories)
- `repository/entity/` — JPA entities
- `api/` — DTOs and API model classes

**Dependencies in use:**
- `spring-boot-starter-web` — REST API support
- `spring-boot-starter-validation` — Bean validation (`@Valid`, `@NotNull`, etc.)
- `spring-boot-starter-test` — JUnit + Mockito for tests
