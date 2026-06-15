package com.enterprise_wrapper_api.wrapper_api.security.oauth2;

import com.enterprise_wrapper_api.wrapper_api.security.entity.User;
import com.enterprise_wrapper_api.wrapper_api.security.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Handles OAuth2 login from Google and GitHub.
 * After successful OAuth2 authentication, this service:
 * 1. Extracts the user's email and name from the provider
 * 2. Creates a new user in the DB if they don't exist (auto-registration)
 * 3. Returns the OAuth2User so Spring Security can proceed
 *
 * Existing JWT auth is NOT affected — this only runs for OAuth2 logins.
 */
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user info from the OAuth2 provider (Google/GitHub)
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google" or "github"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(provider, attributes);
        String name  = extractName(provider, attributes);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth2 provider: " + provider);
        }

        // Auto-create user in DB if first time login
        if (!userRepository.existsByEmail(email)) {
            String username = generateUsername(name, email);
            User newUser = User.builder()
                    .username(username)
                    .email(email)
                    .password("OAUTH2_NO_PASSWORD") // OAuth2 users don't use password login
                    .role(User.Role.ROLE_USER)
                    .enabled(true)
                    .build();
            userRepository.save(newUser);
            System.out.println("OAuth2 new user registered: " + email + " via " + provider);
        }

        return oAuth2User;
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> (String) attributes.get("email");
            case "github" -> (String) attributes.get("email");
            default -> null;
        };
    }

    private String extractName(String provider, Map<String, Object> attributes) {
        return switch (provider) {
            case "google" -> (String) attributes.get("name");
            case "github" -> (String) attributes.getOrDefault("name", attributes.get("login"));
            default -> "user";
        };
    }

    /**
     * Generates a unique username from name + email.
     * e.g. "John Doe" + "john@gmail.com" → "johndoe"
     * If taken, appends part of email: "johndoe_john"
     */
    private String generateUsername(String name, String email) {
        String base = name != null
                ? name.toLowerCase().replaceAll("\\s+", "")
                : email.split("@")[0];

        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        // Append email prefix to make unique
        String withEmail = base + "_" + email.split("@")[0];
        if (!userRepository.existsByUsername(withEmail)) {
            return withEmail;
        }
        // Last resort: append timestamp
        return base + "_" + System.currentTimeMillis();
    }
}
