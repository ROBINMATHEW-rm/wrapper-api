package com.enterprise_wrapper_api.wrapper_api.security.oauth2;

import com.enterprise_wrapper_api.wrapper_api.security.repository.UserRepository;
import com.enterprise_wrapper_api.wrapper_api.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Called after a successful OAuth2 login (Google/GitHub).
 * Issues a JWT token and redirects to /api/oauth2/token?token=<jwt>
 * so the frontend/client can extract and store the token.
 *
 * This keeps OAuth2 and JWT working together seamlessly.
 * Existing JWT auth flow is completely unchanged.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public OAuth2AuthenticationSuccessHandler(JwtService jwtService,
                                               UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract email from OAuth2 attributes (works for both Google and GitHub)
        String email = (String) oAuth2User.getAttributes().get("email");

        // Load user from DB to get their role
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("OAuth2 user not found in DB: " + email));

        // Build UserDetails manually to generate JWT (reuses existing JwtService)
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .build();

        String token = jwtService.generateToken(userDetails);

        System.out.println("OAuth2 login success for: " + email + " | JWT issued");

        // Redirect to token endpoint — client reads the token from URL param
        // Note: context-path /api is already prepended by servlet, so use relative path only
        getRedirectStrategy().sendRedirect(request, response,
                "/oauth2/token?token=" + token);
    }
}
