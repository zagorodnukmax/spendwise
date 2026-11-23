package com.spendwise.category;

import com.spendwise.support.ApiTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryApiTest extends ApiTestBase {

    @Test
    void createUpdateAndUseCustomCategoryForAutoCategorization() {
        String token = registerUser();

        var created = postJson("/api/categories", token, """
                {"name":"Pets","color":"#f59e0b","keywords":"vet, pet food"}
                """);
        assertThat(created).hasStatus(201);
        long id = id(created);

        var tx = postJson("/api/transactions", token, """
                {"amount":300,"type":"EXPENSE","description":"Royal Canin pet food","date":"2026-11-10"}
                """);
        assertThat(tx).bodyJson().extractingPath("$.category.name").isEqualTo("Pets");

        assertThat(putJson("/api/categories/" + id, token, """
                {"name":"Pets & Vet","color":"#f59e0b","keywords":"vet"}
                """)).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Pets & Vet");
    }

    @Test
    void duplicateNameIsConflict() {
        String token = registerUser();
        assertThat(postJson("/api/categories", token, """
                {"name":"groceries","color":"#000000","keywords":""}
                """)).hasStatus(409);
    }

    @Test
    void invalidColorIsRejected() {
        String token = registerUser();
        var result = postJson("/api/categories", token, """
                {"name":"Bad","color":"red","keywords":""}
                """);
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errors.color").isNotNull();
    }

    @Test
    void deletingCategoryKeepsTransactionsAsUncategorized() {
        String token = registerUser();
        Long transport = categoryIdByName(token, "Transport");
        long txId = id(postJson("/api/transactions", token, """
                {"amount":50,"type":"EXPENSE","description":"Uber","date":"2026-11-10"}
                """));

        assertThat(delete("/api/categories/" + transport, token)).hasStatus(204);

        var list = get("/api/transactions?month=2026-11", token);
        assertThat(list).bodyJson().extractingPath("$[0].id").isEqualTo((int) txId);
        assertThat(list).bodyJson().extractingPath("$[0].category").isNull();
    }

    @Test
    void suggestEndpointReturnsMatchOrNoContent() {
        String token = registerUser();
        assertThat(get("/api/categories/suggest?description=Netflix%20monthly", token))
                .hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Subscriptions");
        assertThat(get("/api/categories/suggest?description=random%20thing", token))
                .hasStatus(204);
    }

    @Test
    void cannotTouchAnotherUsersCategory() {
        String alice = registerUser();
        String bob = registerUser();
        Long aliceCategory = categoryIdByName(alice, "Health");

        assertThat(putJson("/api/categories/" + aliceCategory, bob, """
                {"name":"Hacked","color":"#000000","keywords":""}
                """)).hasStatus(404);
        assertThat(delete("/api/categories/" + aliceCategory, bob)).hasStatus(404);
    }
}
