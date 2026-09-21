package com.gateway.platform.service;

import com.gateway.platform.dto.request.LoginRequest;
import com.gateway.platform.dto.request.RegisterRequest;
import com.gateway.platform.dto.response.AuthResponse;
import com.gateway.platform.entity.Role;
import com.gateway.platform.entity.SubscriptionPlan;
import com.gateway.platform.entity.User;
import com.gateway.platform.exception.ApiException;
import com.gateway.platform.repository.SubscriptionPlanRepository;
import com.gateway.platform.repository.UserRepository;
import com.gateway.platform.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private SubscriptionService subscriptionService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, planRepository, passwordEncoder, jwtService, subscriptionService);
    }

    @Test
    void register_withNewEmail_createsUserAndSubscribesToFreePlan() {
        RegisterRequest request = new RegisterRequest("new@user.com", "password123", "New User", "Acme");
        when(userRepository.existsByEmail("new@user.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("user-1");
            return u;
        });
        SubscriptionPlan freePlan = SubscriptionPlan.builder().id("plan-free").name("FREE")
                .monthlyPrice(BigDecimal.ZERO).build();
        when(planRepository.findByName("FREE")).thenReturn(Optional.of(freePlan));
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("jwt-token", response.token());
        assertEquals("new@user.com", response.email());
        assertEquals(Role.CONSUMER.name(), response.role());
        verify(subscriptionService).createSubscription(any(User.class), eq(freePlan));
    }

    @Test
    void register_withExistingEmail_throwsConflict() {
        RegisterRequest request = new RegisterRequest("taken@user.com", "password123", "User", "Acme");
        when(userRepository.existsByEmail("taken@user.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(request));
        assertEquals("EMAIL_TAKEN", ex.getErrorCode());
    }

    @Test
    void login_withCorrectPassword_returnsToken() {
        User user = User.builder().id("user-1").email("a@b.com").passwordHash("hashed")
                .role(Role.CONSUMER).active(true).fullName("A B").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain", "hashed")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest("a@b.com", "plain"));

        assertEquals("jwt-token", response.token());
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        User user = User.builder().id("user-1").email("a@b.com").passwordHash("hashed")
                .role(Role.CONSUMER).active(true).fullName("A B").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(new LoginRequest("a@b.com", "wrong")));
        assertEquals("INVALID_CREDENTIALS", ex.getErrorCode());
    }

    @Test
    void login_withDisabledAccount_throwsForbidden() {
        User user = User.builder().id("user-1").email("a@b.com").passwordHash("hashed")
                .role(Role.CONSUMER).active(false).fullName("A B").build();
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class,
                () -> authService.login(new LoginRequest("a@b.com", "plain")));
        assertEquals("ACCOUNT_DISABLED", ex.getErrorCode());
    }
}
