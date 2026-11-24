package com.bankify.userservice.auth;

import com.bankify.userservice.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody AuthRequests.Register request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthRequests.Token login(@Valid @RequestBody AuthRequests.Login request) {
        return auth.login(request);
    }
}
