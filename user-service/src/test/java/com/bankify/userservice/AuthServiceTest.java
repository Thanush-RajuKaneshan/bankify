package com.bankify.userservice;

import com.bankify.userservice.auth.AuthRequests;
import com.bankify.userservice.auth.AuthService;
import com.bankify.userservice.auth.UserEventPublisher;
import com.bankify.userservice.entity.User;
import com.bankify.userservice.repository.LoginHistoryRepository;
import com.bankify.userservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
class AuthServiceTest {

    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired LoginHistoryRepository logins;
    @Autowired ObjectMapper json;
    @MockitoBean UserEventPublisher events;
    @Value("${bankify.jwt.secret}") String secret;

    @Test
    void registerStoresAHashAndAnnouncesTheUser() throws Exception {
        User user = auth.register(new AuthRequests.Register("asha", "asha@example.com", "correct-horse"));

        assertThat(users.findById(user.getUserId()).orElseThrow().getPassword())
                .startsWith("$2").doesNotContain("correct-horse");
        assertThat(json.writeValueAsString(user)).doesNotContain("password");
        verify(events).userRegistered(any(User.class));
    }

    @Test
    void duplicateUsernameIsRejected() {
        auth.register(new AuthRequests.Register("ravi", "ravi@example.com", "password1"));
        assertThatThrownBy(() -> auth.register(new AuthRequests.Register("ravi", "other@example.com", "password1")))
                .hasMessageContaining("already registered");
    }

    @Test
    void loginIssuesAVerifiableTokenAndRecordsHistory() {
        User user = auth.register(new AuthRequests.Register("meera", "meera@example.com", "s3cret-pass"));

        AuthRequests.Token token = auth.login(new AuthRequests.Login("meera", "s3cret-pass"));

        String subject = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).build()
                .parseSignedClaims(token.token()).getPayload().getSubject();
        assertThat(subject).isEqualTo(String.valueOf(user.getUserId()));
        assertThat(logins.count()).isPositive();
        assertThatThrownBy(() -> auth.login(new AuthRequests.Login("meera", "wrong-pass")))
                .hasMessageContaining("Invalid username or password");
    }
}
