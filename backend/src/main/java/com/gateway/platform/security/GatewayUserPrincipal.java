package com.gateway.platform.security;

import com.gateway.platform.entity.User;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Lightweight authenticated-principal holder used by both JWT (dashboard users)
 * and API-key (gateway consumers) authentication paths, so downstream code can
 * treat "who is calling" uniformly.
 */
@Getter
public class GatewayUserPrincipal {

    private final String userId;
    private final String email;
    private final String role;

    public GatewayUserPrincipal(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public static GatewayUserPrincipal fromUser(User user) {
        return new GatewayUserPrincipal(user.getId(), user.getEmail(), user.getRole().name());
    }

    public UsernamePasswordAuthenticationToken toAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                this, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
