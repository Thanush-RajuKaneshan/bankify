package com.bankify.userservice.auth;

import com.bankify.userservice.entity.LoginHistory;
import com.bankify.userservice.entity.User;
import com.bankify.userservice.repository.LoginHistoryRepository;
import com.bankify.userservice.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository users;
    private final LoginHistoryRepository logins;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwt;
    private final UserEventPublisher events;

    public AuthService(UserRepository users, LoginHistoryRepository logins, PasswordEncoder passwordEncoder,
                       JwtService jwt, UserEventPublisher events) {
        this.users = users;
        this.logins = logins;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.events = events;
    }

    public User register(AuthRequests.Register request) {
        if (users.existsByUsernameOrEmail(request.username(), request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered");
        }
        User user;
        try {
            user = users.save(new User(request.username(), request.email(),
                    passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {
            // Two sign-ups raced past the check above; the unique constraint let only one through.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email is already registered");
        }
        events.userRegistered(user);
        return user;
    }

    public AuthRequests.Token login(AuthRequests.Login request) {
        User user = users.findByUsername(request.username())
                .filter(found -> passwordEncoder.matches(request.password(), found.getPassword()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        // Re-announced so a sign-up event lost while Kafka was down is repaired; consumers treat repeats as no-ops.
        events.userRegistered(user);
        logins.save(new LoginHistory(LocalDateTime.now(), null, user));
        return new AuthRequests.Token(jwt.issue(user.getUserId(), user.getUsername()), user.getUserId(), jwt.ttlSeconds());
    }
}
