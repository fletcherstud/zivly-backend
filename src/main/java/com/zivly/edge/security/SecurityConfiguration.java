package com.zivly.edge.security;

import com.zivly.edge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity(debug = true)
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final AppleClientSecretGenerator appleClientSecretGenerator;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless API
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/oauth2/code/apple").permitAll()
                        .requestMatchers("/error").permitAll() //TODO: Maybe dont expose endpoint?
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint()) // 401 for unauthenticated
                        .accessDeniedHandler(restAccessDeniedHandler())) // 403 for unauthorized
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .tokenEndpoint(token -> token.accessTokenResponseClient(tokenResponseClient())))
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository),
                        UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable()) // Disable default login page
                .logout(logout -> logout.disable()); // Disable logout redirect

        return http.build();
    }

    @Bean
    public RestClientAuthorizationCodeTokenResponseClient tokenResponseClient() {
        RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
        client.setParametersConverter(request -> {
            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add("client_id", request.getClientRegistration().getClientId());
            parameters.add("code", request.getAuthorizationExchange().getAuthorizationResponse().getCode());
            parameters.add("grant_type", AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
            parameters.add("redirect_uri", request.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
            if ("apple".equals(request.getClientRegistration().getRegistrationId())) {
                String clientSecret = appleClientSecretGenerator.generate();
                parameters.add("client_secret", clientSecret);
                System.out.println("Apple token request - client_id: " + request.getClientRegistration().getClientId() +
                        ", code: " + request.getAuthorizationExchange().getAuthorizationResponse().getCode() +
                        ", redirect_uri: " + request.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri() +
                        ", client_secret: " + clientSecret);
            } else {
                parameters.add("client_secret", request.getClientRegistration().getClientSecret());
            }
            return parameters;
        });
        return client;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
        };
    }
}