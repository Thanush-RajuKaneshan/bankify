package com.bankify.userservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loginId;

    private LocalDateTime loginTimestamp;
    private LocalDateTime logoutTimestamp;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public LoginHistory() {
    }

    public LoginHistory(LocalDateTime loginTimestamp, LocalDateTime logoutTimestamp, User user) {
        this.loginTimestamp = loginTimestamp;
        this.logoutTimestamp = logoutTimestamp;
        this.user = user;
    }

    public Long getLoginId() {
        return loginId;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public LocalDateTime getLoginTimestamp() {
        return loginTimestamp;
    }

    public void setLoginTimestamp(LocalDateTime loginTimestamp) {
        this.loginTimestamp = loginTimestamp;
    }

    public LocalDateTime getLogoutTimestamp() {
        return logoutTimestamp;
    }

    public void setLogoutTimestamp(LocalDateTime logoutTimestamp) {
        this.logoutTimestamp = logoutTimestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
