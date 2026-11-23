package com.spendwise.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for integration tests (*IT). Subclasses start the whole application on a random port
 * ({@code @SpringBootTest(webEnvironment = RANDOM_PORT)}) and talk to it with a plain JDK HTTP client,
 * exactly like the Angular app does: real Tomcat, real security filter chain, real JSON, real database.
 */
public abstract class RealServerSupport {

    @Value("${local.server.port}")
    protected int port;

    protected final HttpClient http = HttpClient.newHttpClient();

    public record Response(int status, String body, HttpHeaders headers) {
        public <T> T json(String path) {
            return JsonPath.read(body, path);
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }
    }

    protected URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    protected Response send(HttpRequest.Builder request) {
        try {
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(), response.headers());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected HttpRequest.Builder request(String path, String token) {
        var b = HttpRequest.newBuilder(uri(path));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return b;
    }

    protected Response get(String path, String token) {
        return send(request(path, token).GET());
    }

    protected Response delete(String path, String token) {
        return send(request(path, token).DELETE());
    }

    protected Response json(String method, String path, String token, String body) {
        return send(request(path, token).header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body)));
    }

    protected String login(String email, String password) {
        var r = json("POST", "/api/auth/login", null,
                "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
        assertThat(r.status()).as(r.body()).isEqualTo(200);
        return r.json("$.accessToken");
    }

    protected String registerUser() {
        var r = json("POST", "/api/auth/register", null,
                "{\"email\":\"it-%s@example.com\",\"password\":\"secret123\",\"displayName\":\"IT User\"}"
                        .formatted(UUID.randomUUID()));
        assertThat(r.status()).as(r.body()).isEqualTo(201);
        return r.json("$.accessToken");
    }
}
