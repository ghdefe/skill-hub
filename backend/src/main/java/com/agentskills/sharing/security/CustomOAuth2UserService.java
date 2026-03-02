package com.agentskills.sharing.security;

import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Custom OAuth2 user service that creates or updates User records
 * on GitHub OAuth callback, encrypting the access token before storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Object githubIdObj = oAuth2User.getAttribute("id");
        String githubId = String.valueOf(githubIdObj);
        String username = oAuth2User.getAttribute("login");
        String displayName = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        String encryptedToken = encryptionUtil.encrypt(accessToken);

        Optional<User> existingUser = userRepository.findByGithubId(githubId);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(encryptedToken);
            userRepository.save(user);
            log.info("Updated existing user: {}", username);
        } else {
            User user = new User();
            user.setGithubId(githubId);
            user.setUsername(username);
            user.setDisplayName(displayName);
            user.setAvatarUrl(avatarUrl);
            user.setAccessToken(encryptedToken);
            userRepository.save(user);
            log.info("Created new user: {}", username);
        }

        return oAuth2User;
    }
}
