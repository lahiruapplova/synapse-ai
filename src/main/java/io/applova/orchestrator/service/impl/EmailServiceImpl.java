package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.model.dto.ZohoWebhookPayload;
import io.applova.orchestrator.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public Mono<String> sendAutoReply(ZohoWebhookPayload payload, String kbResponse) {
        return Mono.fromCallable(() -> {
            try {
                // Generate a unique message ID
                String messageId = UUID.randomUUID().toString();

                // Create a MIME message
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                // Set email details
                helper.setFrom(senderEmail);
                helper.setTo(payload.getContactEmail());
                helper.setSubject("Re: " + payload.getIssueTitle());

                // Construct email body
                String emailBody = buildEmailBody(payload, kbResponse);
                helper.setText(emailBody, true); // true indicates HTML content

                // Send the email
                mailSender.send(message);

                log.info("Sent auto-reply email to {}", payload.getContactEmail());
                return messageId;
            } catch (Exception e) {
                log.error("Error sending auto-reply email: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to send email", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> sendStatusUpdate(String emailMessageId, String jiraKey, String newStatus) {
        return Mono.fromCallable(() -> {
            try {
                // Create a MIME message for status update
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                // Set email details
                helper.setFrom(senderEmail);
                helper.setTo(senderEmail); // Sending to internal email for status tracking
                helper.setSubject("Ticket Status Update: " + jiraKey);

                // Construct status update email body
                String emailBody = buildStatusUpdateBody(emailMessageId, jiraKey, newStatus);
                helper.setText(emailBody, true);

                // Send the email
                mailSender.send(message);

                log.info("Sent status update email for ticket {}", jiraKey);
                return null;
            } catch (Exception e) {
                log.error("Error sending status update email: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to send status update email", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String buildEmailBody(ZohoWebhookPayload payload, String kbResponse) {
        return String.format(
            "<html><body>" +
            "<h2>Auto-Reply for: %s</h2>" +
            "<p><strong>Original Issue:</strong> %s</p>" +
            "<p><strong>Response:</strong> %s</p>" +
            "<p><strong>Contact Email:</strong> %s</p>" +
            "</body></html>",
            payload.getIssueTitle(),
            payload.getIssueDescription(),
            kbResponse,
            payload.getContactEmail()
        );
    }

    private String buildStatusUpdateBody(String emailMessageId, String jiraKey, String newStatus) {
        return String.format(
            "<html><body>" +
            "<h2>Ticket Status Update</h2>" +
            "<p><strong>Ticket Key:</strong> %s</p>" +
            "<p><strong>New Status:</strong> %s</p>" +
            "<p><strong>Original Email Message ID:</strong> %s</p>" +
            "</body></html>",
            jiraKey,
            newStatus,
            emailMessageId
        );
    }
}

