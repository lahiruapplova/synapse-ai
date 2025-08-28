package io.applova.orchestrator.controller;

import io.applova.orchestrator.model.dto.JiraWebhookPayload;
import io.applova.orchestrator.service.EmailService;
import io.applova.orchestrator.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class JiraWebhookController {

    private final TicketService ticketService;
    private final EmailService emailService;

    @PostMapping("/api/jira/webhook")
    public Mono<ResponseEntity<String>> handleJiraWebhook(
            @Valid @RequestBody JiraWebhookPayload payload
    ) {
        // Process the Jira webhook event
        return processJiraWebhookEvent(payload)
            .thenReturn(ResponseEntity.ok("Jira webhook processed successfully"))
            .onErrorResume(ex -> {
                log.error("Jira webhook processing error", ex);
                return Mono.just(ResponseEntity.badRequest().body("Jira webhook processing failed"));
            });
    }

    /**
     * Process the Jira webhook event by finding the associated ticket mapping
     * and sending a status update email.
     *
     * @param payload The Jira webhook payload
     * @return A Mono indicating the completion of processing
     */
    private Mono<Void> processJiraWebhookEvent(JiraWebhookPayload payload) {
        // Find the ticket mapping by Jira key
        return ticketService.findByJiraKey(payload.getIssueKey())
            .flatMap(ticketMapping -> {
                // If email message ID exists, send status update
                if (ticketMapping.getEmailMessageId() != null) {
                    return emailService.sendStatusUpdate(
                            ticketMapping.getEmailMessageId(), 
                            payload.getIssueKey(), 
                            payload.getStatus().getName()
                        )
                        .then(ticketService.updateTicketStatus(
                            payload.getIssueKey(), 
                            payload.getStatus().getName()
                        ));
                }
                
                // If no email message ID, just update ticket status
                return ticketService.updateTicketStatus(
                    payload.getIssueKey(), 
                    payload.getStatus().getName()
                );
            })
            .switchIfEmpty(Mono.fromRunnable(() -> 
                log.warn("No ticket mapping found for Jira key: {}", payload.getIssueKey())
            ))
            .then(); // Convert to Mono<Void>
    }
}
