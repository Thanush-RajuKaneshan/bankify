package com.bankify.userservice.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record Register(@NotBlank @Size(max = 50) String username, @Email @NotBlank @Size(max = 255) String email,
                           // BCrypt only uses the first 72 bytes
                           @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {
    }

    public record Login(@NotBlank String username, @NotBlank String password) {
    }

    public record Token(String token, Long userId, long expiresInSeconds) {
    }
}
