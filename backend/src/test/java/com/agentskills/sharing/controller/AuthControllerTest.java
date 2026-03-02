package com.agentskills.sharing.controller;

import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setGithubId("gh-456");
        testUser.setUsername("testuser");
        testUser.setDisplayName("Test User");
        testUser.setAvatarUrl("https://avatars.githubusercontent.com/u/123");
        testUser.setAccessToken("encrypted-token");
    }

    @Test
    void githubLogin_shouldRedirectToOAuth2Authorization() throws Exception {
        var response = new MockHttpServletResponse();
        authController.githubLogin(response);

        assertThat(response.getRedirectedUrl()).isEqualTo("/oauth2/authorization/github");
    }

    @Test
    void logout_shouldReturnOkWithMessage() {
        var result = authController.logout();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).containsEntry("message", "已成功退出登录");
    }

    @Test
    void getCurrentUser_withValidAuthentication_shouldReturnUserInfo() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user-123", null, Collections.emptyList());
        when(userRepository.findById("user-123")).thenReturn(Optional.of(testUser));

        var result = authController.getCurrentUser(auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo("user-123");
        assertThat(result.getBody().username()).isEqualTo("testuser");
        assertThat(result.getBody().displayName()).isEqualTo("Test User");
        assertThat(result.getBody().avatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/123");
    }

    @Test
    void getCurrentUser_withNullAuthentication_shouldReturn401() {
        var result = authController.getCurrentUser(null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getCurrentUser_withNonExistentUser_shouldThrowException() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "non-existent-id", null, Collections.emptyList());
        when(userRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authController.getCurrentUser(auth))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("用户不存在");
    }
}
