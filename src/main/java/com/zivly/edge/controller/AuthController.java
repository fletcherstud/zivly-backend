package com.zivly.edge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zivly.edge.model.AuthProvider;
import com.zivly.edge.model.AuthResponse;
import com.zivly.edge.model.entity.User;
import com.zivly.edge.model.request.LoginRequest;
import com.zivly.edge.model.request.UserRequest;
import com.zivly.edge.model.response.UserCreateResponse;
import com.zivly.edge.model.response.UserResponse;
import com.zivly.edge.repository.UserRepository;
import com.zivly.edge.security.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @PostMapping("/register")
    public ResponseEntity<UserCreateResponse> register(@RequestBody UserRequest request) {
        log.info("Attempting user registration {}", request);
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthdate(request.getBirthDate())
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);
        UserCreateResponse createResponse = UserCreateResponse.builder()
                .userResponse(objectMapper.convertValue(user, UserResponse.class))
                .tokenResponse(createAuthResponse(user))
                .build();
        return ResponseEntity.ok(createResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<UserCreateResponse> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        UserCreateResponse createResponse = UserCreateResponse.builder()
                .userResponse(objectMapper.convertValue(user, UserResponse.class))
                .tokenResponse(createAuthResponse(user))
                .build();
        return ResponseEntity.ok(createResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        if (!jwtUtil.validateToken(request.getRefreshToken(), true)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Invalid refresh token")
                            .build());
        }

        UUID id = jwtUtil.getIdFromToken(request.getRefreshToken(), true);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return ResponseEntity.ok(createAuthResponse(user));
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, "Authentication successful");
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
