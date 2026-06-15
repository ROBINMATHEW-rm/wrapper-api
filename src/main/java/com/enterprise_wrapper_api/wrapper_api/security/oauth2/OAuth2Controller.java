package com.enterprise_wrapper_api.wrapper_api.security.oauth2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * After OAuth2 login, the success handler redirects here with the JWT token.
 * The client reads the token from this response and uses it for all future requests.
 */
@RestController
@RequestMapping("/oauth2")
@Tag(name = "OAuth2", description = "OAuth2 login endpoints for Google and GitHub")
public class OAuth2Controller {

    @Operation(
        summary = "Get JWT token after OAuth2 login",
        description = "After successful Google/GitHub login, you are redirected here with your JWT token. " +
                      "Copy the token and use it as Bearer token for all RAG API calls."
    )
    @GetMapping("/token")
    public ResponseEntity<Map<String, Object>> getToken(
            @RequestParam("token") String token) {

        return ResponseEntity.ok(Map.of(
                "token", token,
                "type", "Bearer",
                "message", "OAuth2 login successful. Use this token in Authorization: Bearer <token> header.",
                "usage", "Add header: Authorization: Bearer " + token
        ));
    }

    @Operation(
        summary = "OAuth2 login links",
        description = "Use these URLs to initiate OAuth2 login flow in a browser"
    )
    @GetMapping("/links")
    public ResponseEntity<Map<String, Object>> loginLinks() {
        return ResponseEntity.ok(Map.of(
                "google", "http://localhost:8080/api/oauth2/authorization/google",
                "github", "http://localhost:8080/api/oauth2/authorization/github",
                "instructions", "Open one of these URLs in your browser to login with Google or GitHub"
        ));
    }
}
