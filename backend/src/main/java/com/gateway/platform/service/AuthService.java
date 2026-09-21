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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        }

        User user = User.builder()
                .email(request.email().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .company(request.company() == null ? "" : request.company())
                .role(Role.CONSUMER)
                .active(true)
                .build();
        user = userRepository.save(user);

        SubscriptionPlan freePlan = planRepository.findByName("FREE")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NO_DEFAULT_PLAN",
                        "No FREE plan configured. Contact an administrator."));
        subscriptionService.createSubscription(user, freePlan);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        if (!user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account has been disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
