package com.zivly.edge.security;


import com.zivly.edge.model.AuthProvider;
import com.zivly.edge.model.entity.User;
import com.zivly.edge.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        String email = oAuth2User.getAttribute("email");
        String firstName;
        String lastName;

        // Handle provider-specific name attributes
        if ("google".equals(registrationId)) {
            firstName = oAuth2User.getAttribute("given_name");
            lastName = oAuth2User.getAttribute("family_name");
        } else if ("apple".equals(registrationId)) {
            // Apple provides name in a nested object on first login
            Map<String, String> nameMap = oAuth2User.getAttribute("name");
            if (nameMap != null) {
                firstName = nameMap.get("firstName");
                lastName = nameMap.get("lastName");
            } else {
                lastName = null;
                firstName = null;
            }
        } else {
            lastName = null;
            firstName = null;
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .authProvider(provider)
                            .providerId(oAuth2User.getAttribute("sub"))
                            .build();
                    return userRepository.save(newUser);
                });

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }
}