package com.edumanage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the app with the production profile against a real PostgreSQL: Flyway applies
 * V1__init.sql and Hibernate validates every entity against it, so a migration that
 * drifts from the entities fails here instead of on deploy. Skipped when Docker is absent.
 */
@SpringBootTest(properties = {
        "JWT_SECRET=prod-like-test-secret-0123456789abcdefghijklmnop",
        "app.bootstrap.enabled=false",
})
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;

    @Test
    void migratedSchemaMatchesTheEntitiesAndServesRequests() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"schoolName":"Postgres Academy","slug":"postgres-academy","adminEmail":"admin@pg.test",
                 "adminPassword":"correct-horse-battery","adminFullName":"Pat Postgres"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.slug").value("postgres-academy"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"tenantSlug":"postgres-academy","email":"admin@pg.test","password":"correct-horse-battery"}"""))
                .andExpect(status().isOk());
    }
}
