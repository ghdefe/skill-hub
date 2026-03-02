package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.AuthUserResponse;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Authentication REST API controller.
 *
 * <ul>
 *   <li>GET /api/auth/github - Redirects to GitHub OAuth authorization</li>
 *   <li>GET /api/auth/github/callback - Handled by Spring Security OAuth2 + OAuth2SuccessHandler</li>
 *   <li>POST /api/auth/logout - Returns success (frontend clears JWT from localStorage)</li>
 *   <li>GET /api/auth/me - Returns current authenticated user info</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    /**
     * Initiates GitHub OAuth login by redirecting to Spring Security's OAuth2 authorization endpoint.
     */
    @GetMapping("/github")
    public void githubLogin(HttpServletResponse response) throws IOException {
        // Must use absolute URL so the browser hits the backend directly,
        // not the frontend dev server.
        response.sendRedirect("http://localhost:18123/oauth2/authorization/github");
    }

    /**
     * Logout endpoint. The actual logout is handled by the frontend clearing
     * the JWT from localStorage. This endpoint confirms the action.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "已成功退出登录"));
    }

    /**
     * Returns the current authenticated user's profile information.
     * The userId is extracted from the JWT by JwtAuthenticationFilter and set as the principal.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        String userId = (String) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("用户不存在"));

        AuthUserResponse userResponse = new AuthUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl()
        );

        return ResponseEntity.ok(userResponse);
    }
}
