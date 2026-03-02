package com.agentskills.sharing.security;

import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles successful OAuth2 authentication by generating a JWT
 * and redirecting to the frontend callback URL with the token.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Object githubIdObj = oAuth2User.getAttribute("id");
        String githubId = String.valueOf(githubIdObj);

        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth callback"));

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getAvatarUrl());

        String redirectUrl = frontendUrl + "/auth/callback?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        log.info("OAuth2 login successful for user: {}, redirecting to frontend", user.getUsername());
        response.sendRedirect(redirectUrl);
    }
}
