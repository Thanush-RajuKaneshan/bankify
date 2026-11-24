package com.bankify.api_gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-characters";
    private final JwtAuthFilter filter = new JwtAuthFilter(SECRET);
    private final AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
    private final GatewayFilterChain chain = exchange -> {
        forwarded.set(exchange);
        return Mono.empty();
    };

    private static String token(String secret, long expiresInMs) {
        return Jwts.builder().subject("42").expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private MockServerWebExchange call(String path, String authorization) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get(path).header("X-User-Id", "999");
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        filter.filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void loginAndRegisterNeedNoToken() {
        call("/api/auth/login", null);
        assertThat(forwarded.get().getRequest().getHeaders().get("X-User-Id")).isNull();
    }

    @Test
    void missingBadOrExpiredTokensAreRejected() {
        assertThat(call("/api/auth/../users/me", null).getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call("/api/accounts", null).getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call("/api/accounts", "Bearer not-a-jwt").getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call("/api/accounts", "Bearer " + token("another-secret-that-is-also-32-chars!!", 60_000))
                .getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(call("/api/accounts", "Bearer " + token(SECRET, -60_000)).getResponse().getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forwarded.get()).isNull();
    }

    @Test
    void validTokenIsForwardedWithTheVerifiedUserId() {
        call("/api/accounts", "Bearer " + token(SECRET, 60_000));

        // The client tried to send X-User-Id: 999; only the id from the token survives.
        assertThat(forwarded.get().getRequest().getHeaders().get("X-User-Id")).containsExactly("42");
    }
}
