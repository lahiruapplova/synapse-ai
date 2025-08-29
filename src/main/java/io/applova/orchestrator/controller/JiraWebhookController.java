package io.applova.orchestrator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import io.applova.orchestrator.service.TicketService;
import io.applova.orchestrator.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JiraWebhookController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Inject required services as final fields
    private final TicketService ticketService;
    private final EmailService emailService;

    @PostMapping("/api/jira-webhook")
    public ResponseEntity<String> handleJiraWebhook(
            HttpServletRequest request
    ) {
        try {
            // Read raw payload
            String rawPayload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            
            // Log comprehensive request details
            logRequestDetails(request, rawPayload);
            
            // Parse payload
            JsonNode payloadJson = objectMapper.readTree(rawPayload);
            
            // Extract webhook details with multiple fallback strategies
            WebhookDetails details = extractWebhookDetails(payloadJson);
            
            // Log extracted details
            log.error("Extracted Webhook Details: {}", details);
            
            // Validate and process
            if (details.isValid()) {
                return processWebhookPayload(details);
            } else {
                log.error("Invalid webhook payload structure");
                return ResponseEntity.badRequest().body("Invalid payload structure");
            }
        } catch (Exception e) {
            log.error("Comprehensive Webhook Processing Error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Webhook processing failed: " + e.getMessage());
        }
    }

    private void logRequestDetails(HttpServletRequest request, String rawPayload) {
        log.error("===== JIRA WEBHOOK RECEIVED =====");
        log.error("Remote Address: {}", request.getRemoteAddr());
        log.error("Request Method: {}", request.getMethod());
        
        // Log headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.error("Header - {}: {}", headerName, request.getHeader(headerName));
        }
        
        log.error("Raw Payload: {}", rawPayload);
    }

    private WebhookDetails extractWebhookDetails(JsonNode payloadJson) {
        WebhookDetails details = new WebhookDetails();
        
        // Multiple strategies to extract issue key and status
        try {
            // Strategy 1: Direct paths
            details.issueKey = extractStringValue(payloadJson, 
                "issue_key", "issue.key", "issue.fields.key", "key");
            
            details.status = extractStringValue(payloadJson, 
                "issue.fields.status.name", 
                "issue.status.name", 
                "status.name", 
                "fields.status.name");
            
            // Strategy 2: Webhook event type
            details.webhookEvent = extractStringValue(payloadJson, 
                "webhookEvent", "webhook_event", "event");
        } catch (Exception e) {
            log.error("Error extracting webhook details", e);
        }
        
        return details;
    }

    private String extractStringValue(JsonNode node, String... paths) {
        for (String path : paths) {
            String[] pathParts = path.split("\\.");
            JsonNode currentNode = node;
            
            for (String part : pathParts) {
                if (currentNode == null) break;
                currentNode = currentNode.path(part);
            }
            
            if (currentNode != null && !currentNode.isNull() && currentNode.isTextual()) {
                return currentNode.asText();
            }
        }
        return null;
    }

    private ResponseEntity<String> processWebhookPayload(WebhookDetails details) {
        try {
            log.error("Processing Webhook - Issue: {}, Status: {}, Event: {}", 
                details.issueKey, details.status, details.webhookEvent);
            
            // Check if this is a status change event
            if ("jira:issue_updated".equals(details.webhookEvent)) {
                // Find the existing ticket mapping
                return ticketService.findByJiraKey(details.issueKey)
                    .flatMap(ticketMapping -> {
                        // Update ticket status
                        return ticketService.updateTicketStatus(details.issueKey, details.status)
                            .flatMap(updatedMapping -> {
                                // Send status update email if email message ID exists
                                if (updatedMapping.getEmailMessageId() != null) {
                                    return emailService.sendStatusUpdate(
                                        updatedMapping.getEmailMessageId(), 
                                        details.issueKey, 
                                        details.status
                                    ).thenReturn(ResponseEntity.ok("Webhook processed with email notification"));
                                }
                                return Mono.just(ResponseEntity.ok("Webhook processed without email"));
                            });
                    })
                    .onErrorResume(ex -> {
                        log.error("Error processing webhook for issue {}: {}", details.issueKey, ex.getMessage());
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Processing error: " + ex.getMessage()));
                    })
                    .block(); // Convert to blocking for compatibility with ResponseEntity
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error in webhook processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Processing error: " + e.getMessage());
        }
    }

    // Inner class to hold webhook details
    private static class WebhookDetails {
        String issueKey;
        String status;
        String webhookEvent;

        boolean isValid() {
            return issueKey != null && status != null;
        }

        @Override
        public String toString() {
            return "WebhookDetails{" +
                    "issueKey='" + issueKey + '\'' +
                    ", status='" + status + '\'' +
                    ", webhookEvent='" + webhookEvent + '\'' +
                    '}';
        }
    }
}
