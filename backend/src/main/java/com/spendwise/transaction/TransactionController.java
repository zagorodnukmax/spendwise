package com.spendwise.transaction;

import com.spendwise.auth.CurrentUser;
import com.spendwise.transaction.TransactionDtos.MonthlySummary;
import com.spendwise.transaction.TransactionDtos.TransactionRequest;
import com.spendwise.transaction.TransactionDtos.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    /** @param month in the form yyyy-MM; defaults to the current month. */
    @GetMapping
    public List<TransactionResponse> list(@AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) YearMonth month) {
        return service.list(CurrentUser.id(jwt), month == null ? YearMonth.now() : month);
    }

    @GetMapping("/summary")
    public MonthlySummary summary(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(required = false) YearMonth month) {
        return service.summary(CurrentUser.id(jwt), month == null ? YearMonth.now() : month);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransactionRequest request) {
        return service.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                      @Valid @RequestBody TransactionRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}
