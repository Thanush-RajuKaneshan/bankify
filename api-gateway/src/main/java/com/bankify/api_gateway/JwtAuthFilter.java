package com.bankify.api_gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Every request except sign-up and login must carry a valid "Authorization: Bearer <jwt>" header.
 * The verified user id is forwarded to the services as X-User-Id (any client-supplied value is dropped).
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";

    private final SecretKey key;

    public JwtAuthFilter(@Value("${bankify.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.equals("/api/auth/register") || path.equals("/api/auth/login")) {
            return chain.filter(exchange.mutate()
                    .request(request -> request.headers(headers -> headers.remove(USER_ID_HEADER))).build());
        }
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange);
        }
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange);
        }
        ServerWebExchange authenticated = exchange.mutate()
                .request(request -> request.headers(headers -> headers.set(USER_ID_HEADER, claims.getSubject())))
                .build();
        return chain.filter(authenticated);
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
