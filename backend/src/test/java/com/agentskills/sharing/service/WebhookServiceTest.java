package com.agentskills.sharing.service;

import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    private static final String WEBHOOK_SECRET = "test-webhook-secret";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private RepositoryScannerService repositoryScannerService;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(
                WEBHOOK_SECRET, repositoryRepository, repositoryScannerService, OBJECT_MAPPER);
    }

    // --- Signature verification tests ---

    @Test
    void verifySignature_withValidSignature_shouldReturnTrue() throws Exception {
        byte[] payload = "{\"test\": true}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmac(payload, WEBHOOK_SECRET);

        assertThat(webhookService.verifySignature(payload, signature)).isTrue();
    }

    @Test
    void verifySignature_withInvalidSignature_shouldReturnFalse() {
        byte[] payload = "{\"test\": true}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=0000000000000000000000000000000000000000000000000000000000000000";

        assertThat(webhookService.verifySignature(payload, signature)).isFalse();
    }

    @Test
    void verifySignature_withNullSignature_shouldReturnFalse() {
        byte[] payload = "{\"test\": true}".getBytes(StandardCharsets.UTF_8);

        assertThat(webhookService.verifySignature(payload, null)).isFalse();
    }

    @Test
    void verifySignature_withMissingPrefix_shouldReturnFalse() {
        byte[] payload = "{\"test\": true}".getBytes(StandardCharsets.UTF_8);

        assertThat(webhookService.verifySignature(payload, "invalid-signature")).isFalse();
    }

    @Test
    void verifySignature_withEmptyPayload_shouldVerifyCorrectly() throws Exception {
        byte[] payload = "".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmac(payload, WEBHOOK_SECRET);

        assertThat(webhookService.verifySignature(payload, signature)).isTrue();
    }

    @Test
    void verifySignature_withTamperedPayload_shouldReturnFalse() throws Exception {
        byte[] originalPayload = "{\"test\": true}".getBytes(StandardCharsets.UTF_8);
        byte[] tamperedPayload = "{\"test\": false}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + computeHmac(originalPayload, WEBHOOK_SECRET);

        assertThat(webhookService.verifySignature(tamperedPayload, signature)).isFalse();
    }

    // --- containsSkillsChanges tests ---

    @Test
    void containsSkillsChanges_withAddedSkillFile_shouldReturnTrue() throws Exception {
        String json = """
                {
                  "commits": [{
                    "added": ["skills/my-skill/README.md"],
                    "modified": [],
                    "removed": []
                  }]
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isTrue();
    }

    @Test
    void containsSkillsChanges_withModifiedSkillFile_shouldReturnTrue() throws Exception {
        String json = """
                {
                  "commits": [{
                    "added": [],
                    "modified": ["skills/agent-tool/manifest.json"],
                    "removed": []
                  }]
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isTrue();
    }

    @Test
    void containsSkillsChanges_withRemovedSkillFile_shouldReturnTrue() throws Exception {
        String json = """
                {
                  "commits": [{
                    "added": [],
                    "modified": [],
                    "removed": ["skills/old-skill/README.md"]
                  }]
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isTrue();
    }

    @Test
    void containsSkillsChanges_withNoSkillChanges_shouldReturnFalse() throws Exception {
        String json = """
                {
                  "commits": [{
                    "added": ["src/main.java"],
                    "modified": ["README.md"],
                    "removed": ["docs/old.md"]
                  }]
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isFalse();
    }

    @Test
    void containsSkillsChanges_withEmptyCommits_shouldReturnFalse() throws Exception {
        String json = """
                {
                  "commits": []
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isFalse();
    }

    @Test
    void containsSkillsChanges_withNoCommitsField_shouldReturnFalse() throws Exception {
        String json = "{}";
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isFalse();
    }

    @Test
    void containsSkillsChanges_withSkillsInLaterCommit_shouldReturnTrue() throws Exception {
        String json = """
                {
                  "commits": [
                    {
                      "added": ["src/main.java"],
                      "modified": [],
                      "removed": []
                    },
                    {
                      "added": ["skills/new-skill/README.md"],
                      "modified": [],
                      "removed": []
                    }
                  ]
                }
                """;
        JsonNode root = OBJECT_MAPPER.readTree(json);

        assertThat(webhookService.containsSkillsChanges(root)).isTrue();
    }

    // --- processPushEvent tests ---

    @Test
    void processPushEvent_withSkillsChanges_shouldTriggerRescan() throws Exception {
        User user = new User();
        user.setId("user-1");

        Repository repo = new Repository();
        repo.setId("repo-1");
        repo.setUrl("https://github.com/owner/my-repo");
        repo.setUser(user);

        String payload = """
                {
                  "repository": {
                    "html_url": "https://github.com/owner/my-repo",
                    "name": "my-repo",
                    "owner": { "login": "owner" }
                  },
                  "commits": [{
                    "added": ["skills/new-skill/README.md"],
                    "modified": [],
                    "removed": []
                  }]
                }
                """;

        when(repositoryRepository.findByGithubOwnerAndGithubRepo("owner", "my-repo"))
                .thenReturn(List.of(repo));

        webhookService.processPushEvent(payload.getBytes(StandardCharsets.UTF_8));

        verify(repositoryScannerService).importRepository("https://github.com/owner/my-repo", "user-1");
    }

    @Test
    void processPushEvent_withNoSkillsChanges_shouldNotTriggerRescan() throws Exception {
        String payload = """
                {
                  "repository": {
                    "html_url": "https://github.com/owner/my-repo",
                    "name": "my-repo",
                    "owner": { "login": "owner" }
                  },
                  "commits": [{
                    "added": ["src/main.java"],
                    "modified": [],
                    "removed": []
                  }]
                }
                """;

        webhookService.processPushEvent(payload.getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(repositoryScannerService);
    }

    @Test
    void processPushEvent_withNoMatchingRepo_shouldNotTriggerRescan() throws Exception {
        String payload = """
                {
                  "repository": {
                    "html_url": "https://github.com/owner/unknown-repo",
                    "name": "unknown-repo",
                    "owner": { "login": "owner" }
                  },
                  "commits": [{
                    "added": ["skills/new-skill/README.md"],
                    "modified": [],
                    "removed": []
                  }]
                }
                """;

        when(repositoryRepository.findByGithubOwnerAndGithubRepo("owner", "unknown-repo"))
                .thenReturn(List.of());

        webhookService.processPushEvent(payload.getBytes(StandardCharsets.UTF_8));

        verifyNoInteractions(repositoryScannerService);
    }

    @Test
    void processPushEvent_withRescanFailure_shouldNotThrow() throws Exception {
        User user = new User();
        user.setId("user-1");

        Repository repo = new Repository();
        repo.setId("repo-1");
        repo.setUrl("https://github.com/owner/my-repo");
        repo.setUser(user);

        String payload = """
                {
                  "repository": {
                    "html_url": "https://github.com/owner/my-repo",
                    "name": "my-repo",
                    "owner": { "login": "owner" }
                  },
                  "commits": [{
                    "added": ["skills/new-skill/README.md"],
                    "modified": [],
                    "removed": []
                  }]
                }
                """;

        when(repositoryRepository.findByGithubOwnerAndGithubRepo("owner", "my-repo"))
                .thenReturn(List.of(repo));
        doThrow(new RuntimeException("scan failed"))
                .when(repositoryScannerService).importRepository(anyString(), anyString());

        // Should not throw
        webhookService.processPushEvent(payload.getBytes(StandardCharsets.UTF_8));

        verify(repositoryScannerService).importRepository("https://github.com/owner/my-repo", "user-1");
    }

    // --- Helper ---

    private String computeHmac(byte[] payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal(payload);
        return HexFormat.of().formatHex(hash);
    }
}
