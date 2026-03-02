package com.agentskills.sharing.controller;

import com.agentskills.sharing.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for receiving GitHub webhook events.
 * The endpoint is public (no JWT auth) but protected by HMAC-SHA256 signature verification.
 * Always returns 200 OK to avoid leaking information about internal processing.
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;

    /**
     * Receive GitHub push events.
     * Validates the X-Hub-Signature-256 header, then processes the payload
     * to check for skills/ directory changes.
     *
     * @param payload   the raw request body
     * @param signature the X-Hub-Signature-256 header
     * @param event     the X-GitHub-Event header
     * @return 200 OK regardless of outcome
     */
    @PostMapping("/github")
    public ResponseEntity<Void> handleGitHubWebhook(
            @RequestBody byte[] payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event) {

        // Validate signature
        if (!webhookService.verifySignature(payload, signature)) {
            log.warn("Invalid webhook signature received");
            return ResponseEntity.ok().build();
        }

        // Only process push events
        if (!"push".equals(event)) {
            log.debug("Ignoring non-push webhook event: {}", event);
            return ResponseEntity.ok().build();
        }

        // Process the push event
        try {
            webhookService.processPushEvent(payload);
        } catch (Exception e) {
            log.error("Error processing webhook push event", e);
        }

        return ResponseEntity.ok().build();
    }
}
