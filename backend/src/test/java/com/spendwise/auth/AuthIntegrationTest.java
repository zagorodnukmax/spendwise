package com.spendwise.auth;

import com.spendwise.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registerReturnsBearerTokenAndUser() {
        var result = postJson("/api/auth/register", null, """
                {"email":"Olena@Example.com","password":"secret123","displayName":"  Olena  "}
                """);

        assertThat(result).hasStatus(201);
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.user.email").isEqualTo("olena@example.com");
        assertThat(result).bodyJson().extractingPath("$.user.displayName").isEqualTo("Olena");
        assertThat((String) read(result, "$.accessToken")).isNotBlank();
    }

    @Test
    void newUserGetsDefaultCategories() {
        String token = registerUser();
        assertThat(get("/api/categories", token)).hasStatusOk()
                .bodyJson().extractingPath("$.length()").isEqualTo(6);
    }

    @Test
    void duplicateEmailIsRejectedCaseInsensitively() {
        postJson("/api/auth/register", null, """
                {"email":"dup@example.com","password":"secret123","displayName":"A"}
                """);
        var second = postJson("/api/auth/register", null, """
                {"email":"DUP@example.com","password":"secret123","displayName":"B"}
                """);
        assertThat(second).hasStatus(409);
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() {
        var result = postJson("/api/auth/register", null, """
                {"email":"not-an-email","password":"short","displayName":""}
                """);
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errors.email").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.password").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.displayName").isNotNull();
    }

    @Test
    void loginWithCorrectPasswordReturnsWorkingToken() {
        postJson("/api/auth/register", null, """
                {"email":"login@example.com","password":"secret123","displayName":"Login"}
                """);
        var login = postJson("/api/auth/login", null, """
                {"email":"login@example.com","password":"secret123"}
                """);
        assertThat(login).hasStatusOk();

        String token = read(login, "$.accessToken");
        assertThat(get("/api/me", token)).hasStatusOk()
                .bodyJson().extractingPath("$.email").isEqualTo("login@example.com");
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailIs401() {
        postJson("/api/auth/register", null, """
                {"email":"wrongpass@example.com","password":"secret123","displayName":"W"}
                """);
        assertThat(postJson("/api/auth/login", null, """
                {"email":"wrongpass@example.com","password":"bad-password"}
                """)).hasStatus(401);
        assertThat(postJson("/api/auth/login", null, """
                {"email":"nobody@example.com","password":"secret123"}
                """)).hasStatus(401);
    }

    @Test
    void protectedEndpointsRequireValidBearerToken() {
        assertThat(get("/api/transactions", null)).hasStatus(401);
        assertThat(get("/api/me", "not-a-jwt")).hasStatus(401);

        // Valid structure, wrong signature: flip the last character of a real token.
        String token = registerUser();
        String forged = token.substring(0, token.length() - 2)
                + (token.endsWith("AA") ? "BB" : "AA");
        assertThat(get("/api/me", forged)).hasStatus(401);
    }
}
