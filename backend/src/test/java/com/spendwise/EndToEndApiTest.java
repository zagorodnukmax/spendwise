package com.spendwise;

import com.spendwise.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Happy path through the whole app: register → bearer token → auto-categorized expense → summary. */
class EndToEndApiTest extends ApiTestBase {

    @Test
    void registerThenUseBearerTokenAndAutoCategorize() {
        String token = registerUser();

        var created = postJson("/api/transactions", token, """
                {"amount":120.50,"type":"EXPENSE","description":"Uber to office","date":"2026-11-02"}
                """);
        assertThat(created).hasStatus(201)
                .bodyJson().extractingPath("$.category.name").isEqualTo("Transport");

        assertThat(get("/api/transactions/summary?month=2026-11", token))
                .hasStatusOk()
                .bodyJson().extractingPath("$.expenses").isEqualTo(120.5);
    }
}
