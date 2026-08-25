package com.main.service;

import com.main.dto.AuthResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${app.jwt.ttl}") Duration tokenTtl,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience,
            Clock clock) {
        if (tokenTtl.isZero() || tokenTtl.isNegative() || tokenTtl.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalStateException("JWT_TTL must be greater than zero and no longer than 24 hours");
        }
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.issuer = issuer.trim();
        this.audience = audience.trim();
        this.clock = clock;
    }

    public AuthResponse issue(Authentication authentication) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenTtl);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        if (roles.isEmpty()) {
            throw new IllegalStateException("Authenticated user has no application role");
        }

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .audience(List.of(audience))
                .claim("roles", roles)
                .build();
        JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue();

        return new AuthResponse(
                token,
                "Bearer",
                expiresAt,
                authentication.getName(),
                roles.get(0));
    }
}
