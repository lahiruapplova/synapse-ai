package io.applova.orchestrator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.applova.orchestrator.model.dto.JiraWebhookPayload;
import io.applova.orchestrator.service.EmailService;
import io.applova.orchestrator.service.TicketService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Enumeration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JiraWebhookController {

    private final TicketService ticketService;
    private final EmailService emailService;

    @PostMapping("/api/jira-webhook")
    public ResponseEntity<String> handleJiraWebhook(
            @RequestBody String rawPayload,
            HttpServletRequest request
    ) {
        // Log all incoming webhook details
        log.error("===== JIRA WEBHOOK RECEIVED =====");
        log.error("Remote Address: {}", request.getRemoteAddr());
        log.error("Request Method: {}", request.getMethod());
        
        // Log headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.error("Header - {}: {}", headerName, request.getHeader(headerName));
        }
        
        // Log raw payload
        log.error("Raw Payload: {}", rawPayload);

        try {
            // Parse the payload
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadJson = objectMapper.readTree(rawPayload);
            
            // Extract key information
            String webhookEvent = payloadJson.path("webhookEvent").asText();
            String issueKey = payloadJson.path("issue_key").asText();
            JsonNode issueNode = payloadJson.path("issue");
            String status = issueNode.path("fields").path("status").path("name").asText();

            log.error("Webhook Event: {}", webhookEvent);
            log.error("Issue Key: {}", issueKey);
            log.error("Issue Status: {}", status);

            // Validate payload
            if (StringUtils.hasText(issueKey) && StringUtils.hasText(status)) {
                // Process the webhook
                return processWebhookPayload(issueKey, status);
            } else {
                log.error("Invalid webhook payload: Missing issue key or status");
                return ResponseEntity.badRequest().body("Invalid payload");
            }
        } catch (Exception e) {
            log.error("Error processing Jira webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    private ResponseEntity<String> processWebhookPayload(String issueKey, String status) {
        try {
            // Simulate webhook processing
            log.error("Processing webhook for Issue: {}, Status: {}", issueKey, status);
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error in webhook processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Processing error: " + e.getMessage());
        }
    }
}
