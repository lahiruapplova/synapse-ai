package io.applova.orchestrator.controller;

import io.applova.orchestrator.model.dto.ZohoWebhookPayload;
import io.applova.orchestrator.model.enums.TicketTag;
import io.applova.orchestrator.service.JiraService;
import io.applova.orchestrator.service.KnowledgeBaseService;
import io.applova.orchestrator.service.TicketService;
import io.applova.orchestrator.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@Lazy
public class ZohoWebhookController {

    private final JiraService jiraService;
    private final EmailService emailService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final TicketService ticketService;

    @Value("${zoho.webhook.secret}")
    private String zohoWebhookSecret;

    @PostMapping("/api/zoho/webhook")
    public Mono<ResponseEntity<String>> handleZohoWebhook(
            @RequestHeader("X-Zoho-Secret") String receivedSecret,
            @Valid @RequestBody ZohoWebhookPayload payload
    ) {
        // Validate webhook secret
        if (!zohoWebhookSecret.equals(receivedSecret)) {
            return Mono.error(new SecurityException("Invalid Zoho webhook secret"));
        }

        // Process webhook based on tag
        return processWebhookByTag(payload)
            .thenReturn(ResponseEntity.ok("Webhook processed successfully"))
            .onErrorResume(ex -> {
                log.error("Zoho webhook processing error", ex);
                return Mono.just(ResponseEntity.badRequest().body("Webhook processing failed"));
            });
    }

    private Mono<Void> processWebhookByTag(ZohoWebhookPayload payload) {
        // Determine ticket type based on issue type and severity
        String issueType = payload.getIssueType();
        String issueSeverity = payload.getIssueSeverity();

        // Determine ticket tag based on issue type and severity
        TicketTag tag = determineTicketTag(issueType, issueSeverity);

        switch (tag) {
            case FEATURE:
            case CLARIFICATION:
                return processIntelligentTicket(payload);
            case SUPPORT:
            case BUG:
                return processStandardTicket(payload);
            default:
                log.warn("Received webhook with unknown tag: {}", tag);
                return Mono.empty();
        }
    }

    private TicketTag determineTicketTag(String issueType, String issueSeverity) {
        if (issueType == null || issueSeverity == null) {
            return TicketTag.UNKNOWN;
        }

        switch (issueType.toLowerCase()) {
            case "feature":
                return TicketTag.FEATURE;
            case "bug":
                return issueSeverity.equalsIgnoreCase("high") ? TicketTag.BUG : TicketTag.SUPPORT;
            case "clarification":
                return TicketTag.CLARIFICATION;
            default:
                return TicketTag.UNKNOWN;
        }
    }

    private Mono<Void> processIntelligentTicket(ZohoWebhookPayload payload) {
        return knowledgeBaseService.queryGpt(payload.getIssueTitle(), payload.getIssueDescription())
            .flatMap(gptResponse -> 
                emailService.sendAutoReply(payload, gptResponse)
                    .flatMap(emailMessageId -> 
                        jiraService.createTicket(payload)
                            .flatMap(jiraKey -> 
                                ticketService.saveMapping(
                                    payload.getZohoTicketNumber(), 
                                    jiraKey, 
                                    emailMessageId, 
                                    payload.getIssueTitle()
                                )
                            )
                    )
            )
            .then();
    }

    private Mono<Void> processStandardTicket(ZohoWebhookPayload payload) {
        return jiraService.createTicket(payload)
            .flatMap(jiraKey -> 
                ticketService.saveMapping(
                    payload.getZohoTicketNumber(), 
                    jiraKey, 
                    null, 
                    payload.getIssueTitle()
                )
            )
            .then();
    }

    /**
     * Endpoint for testing Jira ticket creation directly without Zoho webhook validation
     * 
     * @param payload The test payload for creating a Jira ticket
     * @return ResponseEntity with the result of ticket creation
     */
    @PostMapping("/api/test/jira-ticket")
    public Mono<ResponseEntity<String>> createTestJiraTicket(
            @Valid @RequestBody ZohoWebhookPayload payload
    ) {
        log.info("Received test Jira ticket creation request");
        
        return jiraService.createTicket(payload)
            .map(jiraTicketKey -> ResponseEntity.ok("Jira ticket created successfully. Ticket Key: " + jiraTicketKey))
            .onErrorResume(ex -> {
                log.error("Error creating test Jira ticket", ex);
                return Mono.just(ResponseEntity.badRequest().body("Failed to create Jira ticket: " + ex.getMessage()));
            });
    }
}
