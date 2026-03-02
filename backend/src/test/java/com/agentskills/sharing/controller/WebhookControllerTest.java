package com.agentskills.sharing.controller;

import com.agentskills.sharing.service.WebhookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookService webhookService;

    @InjectMocks
    private WebhookController webhookController;

    @Test
    void handleGitHubWebhook_withValidSignatureAndPushEvent_shouldProcessAndReturn200() {
        byte[] payload = "{\"commits\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=abc123";

        when(webhookService.verifySignature(payload, signature)).thenReturn(true);

        var result = webhookController.handleGitHubWebhook(payload, signature, "push");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService).processPushEvent(payload);
    }

    @Test
    void handleGitHubWebhook_withInvalidSignature_shouldReturn200WithoutProcessing() {
        byte[] payload = "{\"commits\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=invalid";

        when(webhookService.verifySignature(payload, signature)).thenReturn(false);

        var result = webhookController.handleGitHubWebhook(payload, signature, "push");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, never()).processPushEvent(any());
    }

    @Test
    void handleGitHubWebhook_withNonPushEvent_shouldReturn200WithoutProcessing() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=abc123";

        when(webhookService.verifySignature(payload, signature)).thenReturn(true);

        var result = webhookController.handleGitHubWebhook(payload, signature, "issues");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, never()).processPushEvent(any());
    }

    @Test
    void handleGitHubWebhook_withNullSignature_shouldReturn200WithoutProcessing() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        when(webhookService.verifySignature(payload, null)).thenReturn(false);

        var result = webhookController.handleGitHubWebhook(payload, null, "push");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, never()).processPushEvent(any());
    }

    @Test
    void handleGitHubWebhook_withNullEvent_shouldReturn200WithoutProcessing() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=abc123";

        when(webhookService.verifySignature(payload, signature)).thenReturn(true);

        var result = webhookController.handleGitHubWebhook(payload, signature, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(webhookService, never()).processPushEvent(any());
    }

    @Test
    void handleGitHubWebhook_alwaysReturns200_evenOnProcessingError() {
        byte[] payload = "{\"commits\":[]}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=abc123";

        when(webhookService.verifySignature(payload, signature)).thenReturn(true);
        doThrow(new RuntimeException("processing error"))
                .when(webhookService).processPushEvent(payload);

        var result = webhookController.handleGitHubWebhook(payload, signature, "push");

        // Controller should still return 200 even if processing throws
        // (In practice, WebhookService catches exceptions internally,
        //  but the controller should be resilient too)
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
