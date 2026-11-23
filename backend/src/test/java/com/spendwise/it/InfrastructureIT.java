package com.spendwise.it;

import com.spendwise.support.RealServerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/** Cross-cutting behaviour that only shows up with a real server: security, CORS, error format, H2 console. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:it-infra;DB_CLOSE_DELAY=-1",
        "app.seed-demo-data=false"
})
class InfrastructureIT extends RealServerSupport {

    @Test
    void protectedEndpointsNeedAValidBearerToken() {
        assertThat(get("/api/transactions", null).status()).isEqualTo(401);
        assertThat(get("/api/transactions", null).header("WWW-Authenticate")).startsWith("Bearer");
        assertThat(get("/api/me", "not.a.jwt").status()).isEqualTo(401);

        String token = registerUser();
        String forged = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(get("/api/me", forged).status()).isEqualTo(401);
        assertThat(get("/api/me", token).status()).isEqualTo(200);
    }

    @Test
    void corsAllowsTheAngularDevServerOnly() {
        var allowed = send(HttpRequest.newBuilder(uri("/api/transactions"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"));
        assertThat(allowed.status()).isEqualTo(200);
        assertThat(allowed.header("Access-Control-Allow-Origin")).isEqualTo("http://localhost:4200");

        var denied = send(HttpRequest.newBuilder(uri("/api/transactions"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "GET"));
        assertThat(denied.status()).isEqualTo(403);
        assertThat(denied.header("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void errorsAreProblemDetailJson() {
        var r = json("POST", "/api/auth/register", null, "{\"email\":\"bad\",\"password\":\"1\",\"displayName\":\"\"}");
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.header("Content-Type")).startsWith("application/problem+json");
        assertThat(r.body()).contains("\"errors\"", "\"email\"", "\"password\"");
    }

    @Test
    void h2ConsoleIsReachableWithoutToken() {
        var r = get("/h2-console/", null);
        assertThat(r.status()).isEqualTo(200);
        assertThat(r.body()).containsIgnoringCase("h2");
    }
}
