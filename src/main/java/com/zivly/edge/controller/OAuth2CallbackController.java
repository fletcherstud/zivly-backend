package com.zivly.edge.controller;

import com.zivly.edge.security.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class OAuth2CallbackController {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private RestClientAuthorizationCodeTokenResponseClient tokenResponseClient;

    @Autowired
    private OAuth2AuthenticationSuccessHandler successHandler;

    @PostMapping("/auth/oauth2/code/apple")
    public void handleAppleFormPost(
            @RequestParam("code") String code,
            @RequestParam("id_token") String idToken,
            @RequestParam("state") String state,
            @RequestParam(value = "user", required = false) String user,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        log.info("Received Apple form_post - Code: {}, State: {}, Token {}, User: {}", code, state, idToken, user);
        ClientRegistration appleRegistration = clientRegistrationRepository.findByRegistrationId("apple");
        log.info("Client ID: {}", appleRegistration.getClientId()); // Should be com.zivly.app

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
            log.info("Token Response: {}", tokenResponse);
            // Convert scopes to GrantedAuthority
            Collection<GrantedAuthority> authorities = tokenResponse.getAccessToken().getScopes()
                    .stream()
                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                    .collect(Collectors.toList());

            Authentication authentication = new org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken(
                    new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                            authorities, // Pass authorities here
                            java.util.Map.of("sub", tokenResponse.getAdditionalParameters().get("sub"), "email", parseEmailFromUser(user)),
                            "sub"),
                    authorities, // Pass authorities here too
                    appleRegistration.getRegistrationId()
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            successHandler.onAuthenticationSuccess(request, response, authentication);
        } catch (OAuth2AuthorizationException e) {
//            log.error("Exception: ", e);
            throw e;
        }


    }

    private String parseEmailFromUser(String userJson) {
        if (userJson == null || userJson.isEmpty()) {
            return null;
        }
        try {
            return userJson.contains("email") ? userJson.split("\"email\":\"")[1].split("\"")[0] : null;
        } catch (Exception e) {
            return null;
        }
    }
}