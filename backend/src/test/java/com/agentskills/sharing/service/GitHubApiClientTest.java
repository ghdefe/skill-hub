package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.GitHubContentItem;
import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.exception.GitHubApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubApiClientTest {

    private MockWebServer mockWebServer;
    private GitHubApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        client = new GitHubApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getRepoContents_returnsContentItems() {
        String json = """
                [
                  {"name": "skill-a", "type": "dir", "path": "skills/skill-a", "download_url": null},
                  {"name": "README.md", "type": "file", "path": "skills/README.md", "download_url": "https://raw.githubusercontent.com/owner/repo/main/skills/README.md"}
                ]
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "100"));

        List<GitHubContentItem> items = client.getRepoContents("owner", "repo", "skills", "token123");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).name()).isEqualTo("skill-a");
        assertThat(items.get(0).type()).isEqualTo("dir");
        assertThat(items.get(0).path()).isEqualTo("skills/skill-a");
        assertThat(items.get(1).name()).isEqualTo("README.md");
        assertThat(items.get(1).type()).isEqualTo("file");
    }

    @Test
    void getFileContent_decodesBase64Content() {
        String originalContent = "# My Skill\nThis is a great skill.";
        String encoded = Base64.getEncoder().encodeToString(originalContent.getBytes(StandardCharsets.UTF_8));
        String json = """
                {"encoding": "base64", "content": "%s"}
                """.formatted(encoded);
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "50"));

        String content = client.getFileContent("owner", "repo", "skills/my-skill/README.md", "token123");

        assertThat(content).isEqualTo(originalContent);
    }

    @Test
    void getRepoInfo_returnsStarAndForkCounts() {
        String json = """
                {"stargazers_count": 42, "forks_count": 7}
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "80"));

        GitHubRepoInfo info = client.getRepoInfo("owner", "repo", "token123");

        assertThat(info.stargazersCount()).isEqualTo(42);
        assertThat(info.forksCount()).isEqualTo(7);
    }

    @Test
    void getRepoContents_throws404WhenNotFound() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"message\": \"Not Found\"}")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "90"));

        assertThatThrownBy(() -> client.getRepoContents("owner", "repo", "skills", "token123"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getRepoContents_throws403WhenRateLimited() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("{\"message\": \"API rate limit exceeded\"}")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "0"));

        assertThatThrownBy(() -> client.getRepoContents("owner", "repo", "skills", "token123"))
                .isInstanceOf(GitHubApiException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void getFileContent_handlesNullToken() {
        String originalContent = "Hello";
        String encoded = Base64.getEncoder().encodeToString(originalContent.getBytes(StandardCharsets.UTF_8));
        String json = """
                {"encoding": "base64", "content": "%s"}
                """.formatted(encoded);
        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-RateLimit-Remaining", "99"));

        String content = client.getFileContent("owner", "repo", "README.md", null);

        assertThat(content).isEqualTo(originalContent);
    }
}
