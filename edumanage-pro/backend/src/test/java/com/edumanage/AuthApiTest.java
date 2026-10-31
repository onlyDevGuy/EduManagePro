package com.edumanage;

import com.edumanage.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends IntegrationTest {

    @Test
    void registeringASchoolSignsInItsAdmin() throws Exception {
        School school = registerSchool();
        call(get("/api/v1/students"), school.adminToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void schoolSlugsAreUnique() throws Exception {
        School school = registerSchool();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(write(Map.of(
                        "schoolName", "Copycat", "slug", school.slug(), "adminEmail", "x@copy.test",
                        "adminPassword", PASSWORD, "adminFullName", "Copy Cat"))))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationValidatesInput() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(write(Map.of(
                        "schoolName", "Bad", "slug", "Not A Slug!", "adminEmail", "not-an-email",
                        "adminPassword", "short", "adminFullName", "Bad"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void unknownSchoolUnknownUserAndWrongPasswordAllLookTheSame() throws Exception {
        School school = registerSchool();
        String admin = "admin@" + school.slug() + ".test";

        String unknownSchool = loginBody("no-such-school", admin, PASSWORD);
        String unknownUser = loginBody(school.slug(), "nobody@" + school.slug() + ".test", PASSWORD);
        String wrongPassword = loginBody(school.slug(), admin, "wrong-password-1");

        assertThat(unknownSchool).contains("Invalid credentials");
        assertThat(unknownUser).isEqualTo(unknownSchool).isEqualTo(wrongPassword);
    }

    @Test
    void fiveWrongPasswordsLockTheAccountUntilTheLockoutPasses() throws Exception {
        School school = registerSchool();
        String admin = "admin@" + school.slug() + ".test";

        for (int i = 0; i < 5; i++) {
            attempt(school.slug(), admin, "wrong-password-" + i).andExpect(status().isUnauthorized());
        }
        // Locked: even the right password is refused, without revealing that it was right.
        attempt(school.slug(), admin, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Try again in 15 minutes")));

        clock.advance(Duration.ofMinutes(16));
        attempt(school.slug(), admin, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void lockingOneAccountDoesNotAffectAnother() throws Exception {
        School first = registerSchool();
        School second = registerSchool();
        for (int i = 0; i < 5; i++) {
            attempt(first.slug(), "admin@" + first.slug() + ".test", "nope-nope-" + i);
        }
        attempt(second.slug(), "admin@" + second.slug() + ".test", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void refreshTokenIssuesNewTokensButCannotCallTheApi() throws Exception {
        School school = registerSchool();

        call(get("/api/v1/students"), school.refreshToken()).andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("refreshToken", school.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void anAccessTokenIsNotAcceptedAsARefreshToken() throws Exception {
        School school = registerSchool();
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(write(Map.of("refreshToken", school.adminToken()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedAndMissingTokensAreRejected() throws Exception {
        School school = registerSchool();
        String token = school.adminToken();
        String tampered = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        call(get("/api/v1/students"), tampered).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/students")).andExpect(status().isUnauthorized());
    }

    @Test
    void errorsNeverIncludeInternalDetails() throws Exception {
        School school = registerSchool();
        call(post("/api/v1/students"), school.adminToken(), "{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The request is malformed or missing a required value."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private ResultActions attempt(String slug, String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(write(Map.of("tenantSlug", slug, "email", email, "password", password))));
    }

    private String loginBody(String slug, String email, String password) throws Exception {
        return attempt(slug, email, password).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
    }
}
