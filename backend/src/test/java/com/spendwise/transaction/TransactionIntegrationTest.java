package com.spendwise.transaction;

import com.spendwise.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIntegrationTest extends IntegrationTest {

    private static final String COFFEE = """
            {"amount":95,"type":"EXPENSE","description":"Coffee","date":"2026-11-03"}
            """;

    @Test
    void crudLifecycle() {
        String token = registerUser();

        long id = id(postJson("/api/transactions", token, COFFEE));

        var updated = putJson("/api/transactions/" + id, token, """
                {"amount":120,"type":"EXPENSE","description":"Coffee and croissant","date":"2026-11-04"}
                """);
        assertThat(updated).hasStatusOk();
        assertThat(updated).bodyJson().extractingPath("$.amount").isEqualTo(120);
        assertThat(updated).bodyJson().extractingPath("$.date").isEqualTo("2026-11-04");

        assertThat(get("/api/transactions?month=2026-11", token))
                .bodyJson().extractingPath("$.length()").isEqualTo(1);

        assertThat(delete("/api/transactions/" + id, token)).hasStatus(204);
        assertThat(get("/api/transactions?month=2026-11", token))
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
    }

    @Test
    void listIsFilteredByMonthAndSortedNewestFirst() {
        String token = registerUser();
        postJson("/api/transactions", token, """
                {"amount":1,"type":"EXPENSE","description":"early","date":"2026-11-01"}""");
        postJson("/api/transactions", token, """
                {"amount":2,"type":"EXPENSE","description":"late","date":"2026-11-30"}""");
        postJson("/api/transactions", token, """
                {"amount":3,"type":"EXPENSE","description":"october","date":"2026-10-31"}""");

        var list = get("/api/transactions?month=2026-11", token);
        assertThat(list).bodyJson().extractingPath("$[*].description").asArray().containsExactly("late", "early");
    }

    @Test
    void usersCannotSeeOrChangeEachOthersTransactions() {
        String alice = registerUser();
        String bob = registerUser();
        long aliceTx = id(postJson("/api/transactions", alice, COFFEE));

        assertThat(get("/api/transactions?month=2026-11", bob))
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(putJson("/api/transactions/" + aliceTx, bob, COFFEE)).hasStatus(404);
        assertThat(delete("/api/transactions/" + aliceTx, bob)).hasStatus(404);

        // Bob also cannot attach Alice's category to his own transaction.
        Long aliceCategory = categoryIdByName(alice, "Groceries");
        assertThat(postJson("/api/transactions", bob, """
                {"amount":1,"type":"EXPENSE","description":"x","date":"2026-11-01","categoryId":%d}
                """.formatted(aliceCategory))).hasStatus(404);
    }

    @Test
    void invalidTransactionIsRejected() {
        String token = registerUser();
        var result = postJson("/api/transactions", token, """
                {"amount":-5,"type":"EXPENSE","description":"","date":null}
                """);
        assertThat(result).hasStatus(400);
        assertThat(result).bodyJson().extractingPath("$.errors.amount").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.description").isNotNull();
        assertThat(result).bodyJson().extractingPath("$.errors.date").isNotNull();
    }
}
