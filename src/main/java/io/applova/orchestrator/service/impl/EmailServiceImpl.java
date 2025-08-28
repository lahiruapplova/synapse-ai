package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.model.dto.ZohoWebhookPayload;
import io.applova.orchestrator.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final WebClient sendgridWebClient;

    @Value("${api.sendgrid.sender-email}")
    private String senderEmail;

    @Override
    public Mono<String> sendAutoReply(ZohoWebhookPayload payload, String kbResponse) {
        // Construct email payload
        Map<String, Object> emailPayload = new HashMap<>();
        
        // Personalization
        Map<String, Object> personalization = new HashMap<>();
        List<Map<String, String>> to = List.of(
            Map.of("email", payload.getContactEmail())
        );
        personalization.put("to", to);
        personalization.put("subject", "Re: " + payload.getSubject());

        // Dynamic template data
        Map<String, String> templateData = new HashMap<>();
        templateData.put("subject", payload.getSubject());
        templateData.put("gptResponse", kbResponse);
        templateData.put("originalDescription", payload.getDescription());
        personalization.put("dynamic_template_data", templateData);

        emailPayload.put("personalizations", List.of(personalization));
        
        // From and template
        emailPayload.put("from", Map.of("email", senderEmail));
        emailPayload.put("template_id", "YOUR_SENDGRID_DYNAMIC_TEMPLATE_ID"); // Replace with actual template ID

        // Generate a unique message ID
        String messageId = UUID.randomUUID().toString();

        // Send email via SendGrid
        return sendgridWebClient.post()
                .uri("/mail/send")
                .bodyValue(emailPayload)
                .retrieve()
                .bodyToMono(Void.class)
                .thenReturn(messageId)
                .doOnSuccess(id -> log.info("Sent auto-reply email to {}", payload.getContactEmail()))
                .doOnError(ex -> log.error("Error sending auto-reply email: {}", ex.getMessage()));
    }

    @Override
    public Mono<Void> sendStatusUpdate(String emailMessageId, String jiraKey, String newStatus) {
        // This method would send a status update email
        // In a real-world scenario, you'd fetch the original email details and send an update
        return Mono.fromRunnable(() -> 
            log.info("Sending status update email for ticket {} with new status {}", jiraKey, newStatus)
        );
    }
}

