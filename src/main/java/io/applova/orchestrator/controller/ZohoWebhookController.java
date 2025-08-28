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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
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
        TicketTag tag = payload.getTag() != null ? payload.getTag() : TicketTag.UNKNOWN;

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

    private Mono<Void> processIntelligentTicket(ZohoWebhookPayload payload) {
        return knowledgeBaseService.queryGpt(payload.getSubject(), payload.getDescription())
            .flatMap(gptResponse -> 
                emailService.sendAutoReply(payload, gptResponse)
                    .flatMap(emailMessageId -> 
                        jiraService.createTicket(payload)
                            .flatMap(jiraKey -> 
                                ticketService.saveMapping(
                                    payload.getRecordId(), 
                                    jiraKey, 
                                    emailMessageId, 
                                    payload.getSubject()
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
                    payload.getRecordId(), 
                    jiraKey, 
                    null, 
                    payload.getSubject()
                )
            )
            .then();
    }
}
