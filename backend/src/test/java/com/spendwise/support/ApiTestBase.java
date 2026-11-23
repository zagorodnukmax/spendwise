package com.spendwise.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for HTTP-level tests: full Spring context, in-memory H2, no demo data.
 * Every test registers its own fresh user, so tests don't depend on each other.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=false"
})
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    @Autowired
    protected MockMvcTester mvc;

    /** Registers a new user with a random email and returns its bearer token. */
    protected String registerUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        var result = postJson("/api/auth/register", null, """
                {"email":"%s","password":"secret123","displayName":"Test User"}
                """.formatted(email));
        assertThat(result).hasStatus(201);
        return read(result, "$.accessToken");
    }

    protected MvcTestResult get(String uri, String token) {
        var req = mvc.get().uri(uri);
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return req.exchange();
    }

    protected MvcTestResult postJson(String uri, String token, String json) {
        var req = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return req.exchange();
    }

    protected MvcTestResult putJson(String uri, String token, String json) {
        var req = mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return req.exchange();
    }

    protected MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    protected static <T> T read(MvcTestResult result, String jsonPath) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected static long id(MvcTestResult result) {
        return ((Number) read(result, "$.id")).longValue();
    }

    protected Long categoryIdByName(String token, String name) {
        var list = get("/api/categories", token);
        java.util.List<Integer> ids = read(list, "$[?(@.name == '" + name + "')].id");
        return ids.getFirst().longValue();
    }
}
