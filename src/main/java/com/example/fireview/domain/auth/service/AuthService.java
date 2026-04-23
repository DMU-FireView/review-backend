package com.example.fireview.domain.auth.service;

import com.example.fireview.domain.auth.dto.LoginRequest;
import com.example.fireview.domain.auth.dto.LoginResponse;
import com.example.fireview.domain.auth.dto.PasswordResetRequest;
import com.example.fireview.domain.auth.dto.SignupRequest;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    // In-memory store for password reset tokens (production: use Redis)
    private final Map<String, String> resetTokenStore = new ConcurrentHashMap<>();

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(Role.USER)
                .provider(OAuthProvider.LOCAL)
                .onboardingCompleted(false)
                .build();
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getNickname(), user.getRole(), user.isOnboardingCompleted());
    }

    public String requestPasswordReset(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String token = UUID.randomUUID().toString();
        resetTokenStore.put(token, email);
        // In production: send email with reset link
        return token;
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String email = resetTokenStore.get(request.token());
        if (email == null) {
            throw new CustomException(ErrorCode.INVALID_RESET_TOKEN);
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        resetTokenStore.remove(request.token());
    }

    private String generateToken(User user) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("fireview")
                .issuedAt(now)
                .expiresAt(now.plusMillis(jwtExpirationMs))
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .claim("userId", user.getId())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}