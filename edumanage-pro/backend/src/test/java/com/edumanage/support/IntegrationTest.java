package com.edumanage.support;

import com.edumanage.domain.User;
import com.edumanage.repository.UserRepository;
import com.edumanage.tenant.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole API against an in-memory H2 database. Every test class shares one
 * context; each test registers its own school with a random slug, so tests never see
 * each other's data.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:edumanage-test;DB_CLOSE_DELAY=-1",
        "app.bootstrap.enabled=false",
        "app.stripe.secret-key=sk_test_not_a_real_key",
        "app.stripe.webhook-secret=" + IntegrationTest.WEBHOOK_SECRET,
})
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(IntegrationTest.TestClock.class)
public abstract class IntegrationTest {

    public static final String WEBHOOK_SECRET = "whsec_test_secret_for_signature_checks";
    public static final String PASSWORD = "correct-horse-battery";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected MutableClock clock;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @TestConfiguration
    static class TestClock {
        @Bean @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    /** A school registered through the public API, with its admin's tokens. */
    public record School(String slug, String tenantId, String adminToken, String refreshToken) {}

    protected School registerSchool() throws Exception {
        String slug = "school-" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode body = read(mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of(
                                "schoolName", "Test School " + slug,
                                "slug", slug,
                                "adminEmail", "admin@" + slug + ".test",
                                "adminPassword", PASSWORD,
                                "adminFullName", "Admin " + slug))))
                .andExpect(status().isOk()));
        return new School(slug, body.at("/tenant/id").asText(),
                body.get("accessToken").asText(), body.get("refreshToken").asText());
    }

    /** Creates a user directly (registration only makes admins) and signs them in. */
    protected String userToken(School school, User.Role role, String studentId) throws Exception {
        String email = role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6) + "@" + school.slug() + ".test";
        try {
            TenantContext.setTenantId(school.tenantId());
            userRepository.save(User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(PASSWORD))
                    .fullName(role.name() + " User")
                    .role(role)
                    .studentId(studentId)
                    .enabled(true)
                    .build());
        } finally {
            TenantContext.clear();
        }
        return login(school.slug(), email, PASSWORD).get("accessToken").asText();
    }

    protected JsonNode login(String slug, String email, String password) throws Exception {
        return read(mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("tenantSlug", slug, "email", email, "password", password))))
                .andExpect(status().isOk()));
    }

    protected String createStudent(School school, String firstName) throws Exception {
        JsonNode body = read(call(post("/api/v1/students"), school.adminToken(), Map.of(
                        "firstName", firstName, "lastName", "Tester",
                        "email", firstName.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 6) + "@example.test",
                        "guardianPhone", "+27 82 000 0000"))
                .andExpect(status().isCreated()));
        return body.get("id").asText();
    }

    protected String createInvoice(School school, String studentId, String amount) throws Exception {
        JsonNode body = read(call(post("/api/v1/invoices"), school.adminToken(), Map.of(
                        "studentId", studentId, "description", "Term fees", "amount", amount, "currency", "ZAR"))
                .andExpect(status().isCreated()));
        return body.get("id").asText();
    }

    /** Sends a request with a bearer token and an optional JSON body. */
    protected ResultActions call(MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
        request.header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(write(body));
        }
        return mvc.perform(request);
    }

    protected ResultActions call(MockHttpServletRequestBuilder request, String token) throws Exception {
        return call(request, token, null);
    }

    protected String write(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
