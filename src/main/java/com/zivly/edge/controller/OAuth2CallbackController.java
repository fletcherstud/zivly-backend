package com.zivly.edge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zivly.edge.model.AuthProvider;
import com.zivly.edge.model.AuthResponse;
import com.zivly.edge.model.UserInfo;
import com.zivly.edge.model.entity.User;
import com.zivly.edge.model.response.UserCreateResponse;
import com.zivly.edge.model.response.UserResponse;
import com.zivly.edge.repository.UserRepository;
import com.zivly.edge.security.JwtUtil;
import com.zivly.edge.security.OAuth2AuthenticationSuccessHandler;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class OAuth2CallbackController {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private RestClientAuthorizationCodeTokenResponseClient tokenResponseClient;

    @Autowired
    private OAuth2AuthenticationSuccessHandler successHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper; // For JSON deserialization

    private JwtDecoder jwtDecoder;

    @Autowired
    public void setClientRegistrationRepository(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        ClientRegistration appleRegistration = clientRegistrationRepository.findByRegistrationId("apple");
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri(appleRegistration.getProviderDetails().getJwkSetUri()).build();
    }

    @PostMapping(value = "/auth/oauth2/code/apple", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handleAppleFormPost(
            @RequestParam("code") String code,
            @RequestParam("id_token") String idToken,
            @RequestParam("state") String state,
            @RequestParam(value = "user", required = false) String user,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        log.info("Received Apple form_post - Code: {}, State: {}, Token: {}, User: {}", code, state, idToken, user);
        ClientRegistration appleRegistration = clientRegistrationRepository.findByRegistrationId("apple");
        log.info("Client ID: {}", appleRegistration.getClientId());

        OAuth2AuthorizationRequest authRequest = OAuth2AuthorizationRequest.authorizationCode()
                .clientId(appleRegistration.getClientId())
                .redirectUri(appleRegistration.getRedirectUri())
                .state(state)
                .scope(appleRegistration.getScopes().toArray(new String[0]))
                .authorizationUri(appleRegistration.getProviderDetails().getAuthorizationUri())
                .build();

        OAuth2AuthorizationResponse authResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(appleRegistration.getRedirectUri())
                .state(state)
                .build();

        OAuth2AuthorizationExchange exchange = new OAuth2AuthorizationExchange(authRequest, authResponse);

        OAuth2AuthorizationCodeGrantRequest tokenRequest = new OAuth2AuthorizationCodeGrantRequest(appleRegistration, exchange);

        try {
            var tokenResponse = tokenResponseClient.getTokenResponse(tokenRequest);

            // Extract and verify id_token using Spring Security's JwtDecoder
            String idTokenFromResponse = (String) tokenResponse.getAdditionalParameters().get("id_token");
            Jwt jwt = jwtDecoder.decode(idTokenFromResponse);
            String sub = jwt.getSubject();

            // Parse user data into UserInfo model
            UserInfo userInfo = parseUserInfo(user);
            String email = userInfo != null ? userInfo.getEmail() : null;
            String firstName = userInfo != null ? userInfo.getFirstName() : null;
            String lastName = userInfo != null ? userInfo.getLastName() : null;

            // Find or create user in repository
            User appUser = userRepository.findByProviderId(sub)
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .id(UUID.randomUUID())
                                .providerId(sub)
                                .authProvider(AuthProvider.APPLE)
                                .email(email)
                                .firstName(firstName)
                                .lastName(lastName)
                                .build();
                        log.info("Creating new user with sub: {}, email: {}, firstName: {}, lastName: {}",
                                sub, email, firstName, lastName);
                        return userRepository.save(newUser);
                    });

            // Update user if new data is provided
            boolean updated = false;
            if (email != null && !email.equals(appUser.getEmail())) {
                if (userRepository.findByEmail(email).isEmpty() || appUser.getEmail().equals(email)) {
                    appUser.setEmail(email);
                    updated = true;
                } else {
                    log.warn("Email {} already in use by another user", email);
                }
            }
            if (firstName != null && !firstName.equals(appUser.getFirstName())) {
                appUser.setFirstName(firstName);
                updated = true;
            }
            if (lastName != null && !lastName.equals(appUser.getLastName())) {
                appUser.setLastName(lastName);
                updated = true;
            }
            if (updated) {
                userRepository.save(appUser);
            }

            // Generate your own tokens
            String accessToken = jwtUtil.generateAccessToken(appUser);
            String refreshToken = jwtUtil.generateRefreshToken(appUser);

            // Handle scopes and authorities
            Collection<GrantedAuthority> authorities = tokenResponse.getAccessToken().getScopes()
                    .stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", sub);
            if (appUser.getEmail() != null) attributes.put("email", appUser.getEmail());

            Authentication authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                    new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                            authorities, attributes, "sub"),
                    authorities,
                    appleRegistration.getRegistrationId()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Return your tokens
            return ResponseEntity.ok(
                    UserCreateResponse.builder()
                                    .userResponse(objectMapper.convertValue(appUser, UserResponse.class))
                                            .tokenResponse(AuthResponse.builder()
                                                    .accessToken(accessToken)
                                                    .refreshToken(refreshToken)
                                                    .message("Authenticated")
                                                    .build())
                    .build());
        } catch (OAuth2AuthorizationException e) {
            log.error("OAuth2 error: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized", "message", e.getMessage()));
        } catch (JwtException e) {
            log.error("JWT verification error: {}", e.getMessage(), e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid id_token", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal Server Error", "message", e.getMessage()));
        }
    }

    private UserInfo parseUserInfo(String userJson) {
        if (userJson == null || userJson.isEmpty() || userJson.equals("[{\"name\":\"null null\"}]")) {
            log.debug("User JSON is null, empty, or placeholder: {}", userJson);
            return null;
        }
        try {
            return objectMapper.readValue(userJson, UserInfo.class);
        } catch (Exception e) {
            log.warn("Failed to parse user JSON: {}", userJson, e);
            return null;
        }
    }
}