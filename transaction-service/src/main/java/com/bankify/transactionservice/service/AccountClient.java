package com.bankify.transactionservice.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

/** Calls account-service (found through Eureka) to check ownership and move money. */
@Component
public class AccountClient {

    private final RestTemplate http;

    public AccountClient(RestTemplate http) {
        this.http = http;
    }

    /** 404 unless the user owns the account; 503 if account-service cannot be reached. */
    public void requireOwner(Long userId, String accountNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));
        try {
            http.exchange("http://ACCOUNTSERVICE/api/accounts/number/{n}", HttpMethod.GET,
                    new HttpEntity<>(headers), Void.class, accountNumber);
        } catch (HttpClientErrorException e) {
            throw rejected(e);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account service is unavailable", e);
        }
    }

    /**
     * Positive amount credits, negative debits. A ResponseStatusException means account-service rejected
     * the change and no money moved; any other exception means the outcome is unknown.
     */
    public void adjust(String accountNumber, BigDecimal amount) {
        try {
            http.put("http://ACCOUNTSERVICE/internal/accounts/number/{n}/balance?amount={a}", null, accountNumber, amount);
        } catch (HttpClientErrorException e) {
            throw rejected(e);
        }
    }

    /** Passes 404 (no such account) and 409 (insufficient funds) straight back to the caller. */
    private static ResponseStatusException rejected(HttpClientErrorException e) {
        Map<?, ?> body = e.getResponseBodyAs(Map.class);
        Object message = body == null ? null : body.get("message");
        return new ResponseStatusException(e.getStatusCode(), message != null ? message.toString() : e.getStatusText());
    }
}
