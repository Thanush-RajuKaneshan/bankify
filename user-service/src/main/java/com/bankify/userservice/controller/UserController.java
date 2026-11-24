package com.bankify.userservice.controller;

import com.bankify.userservice.entity.User;
import com.bankify.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only user endpoints. New users sign up through /api/auth/register. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the logged-in user")
    public User getMe(@RequestHeader("X-User-Id") Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping("/me/accounts")
    @Operation(summary = "List the logged-in user's accounts (fetched from account-service)")
    public List<?> getMyAccounts(@RequestHeader("X-User-Id") Long userId) {
        return userService.getUserAccounts(userId);
    }
}
