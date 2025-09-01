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
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // Add a simple cache to track sent initial emails
    private final Set<String> sentInitialEmails = Collections.synchronizedSet(new HashSet<>());

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
                // Normalize the email message ID to standard format if it's not already
                String normalizedMessageId = normalizeMessageId(emailMessageId);
                
                // Generate a new unique message ID for this email
                String newMessageId = generateMessageId();
                
                // Set threading headers to link this email to the original thread
                message.setHeader("Message-ID", newMessageId);
                message.setHeader("In-Reply-To", normalizedMessageId);
                message.setHeader("References", normalizedMessageId);
                
                helper.setSubject("Ticket Status Update: " + jiraKey);

                // Construct status update email body
                String emailBody = buildStatusUpdateBody(normalizedMessageId, jiraKey, newStatus);
                helper.setText(emailBody, true);

                // Send the email
                mailSender.send(message);

                log.info("Sent status update email for ticket {} in thread {}", jiraKey, normalizedMessageId);
                return null;
            } catch (Exception e) {
                log.error("Error sending status update email: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to send status update email", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<String> createInitialTicketEmail(String zohoRecordId, String jiraKey, String status) {
        return Mono.fromCallable(() -> {
            // Check if an initial email for this ticket has already been sent
            String emailKey = jiraKey + "_" + status;
            if (sentInitialEmails.contains(emailKey)) {
                log.info("Initial email for ticket {} with status {} already sent. Skipping.", jiraKey, status);
                return null;
            }

            try {
                // Generate a unique message ID
                String emailMessageId = generateMessageId();
                
                // Prepare the email message
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                
                // Set Message-ID header
                message.setHeader("Message-ID", emailMessageId);
                
                // Set basic email details
                helper.setFrom(senderEmail);
                helper.setTo(senderEmail); // You might want to fetch the actual recipient dynamically
                helper.setSubject("Ticket " + jiraKey + " - Initial Status: " + status);
                
                // Compose email body
                String emailBody = String.format(
                    "Ticket Details:\n" +
                    "Zoho Record ID: %s\n" +
                    "Jira Key: %s\n" +
                    "Current Status: %s\n\n" +
                    "This is an initial email thread created for tracking purposes.",
                    zohoRecordId, jiraKey, status
                );
                
                helper.setText(emailBody);
                
                // Send the email
                mailSender.send(message);
                
                // Mark this email as sent to prevent duplicates
                sentInitialEmails.add(emailKey);
                
                log.info("Created initial email thread for Jira ticket: {}", jiraKey);
                return emailMessageId;
            } catch (Exception e) {
                log.error("Error creating initial ticket email: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to create initial ticket email", e);
            }
        }).publishOn(Schedulers.boundedElastic());
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
        // Determine if the status indicates a functionality problem
        boolean isProblemStatus = isProblemStatus(newStatus);
        
        String problemMessage = isProblemStatus 
            ? "<p style='color: red; font-weight: bold;'>!</p>"
            : "";
        
        return String.format(
            "<html><body>" +
            "<h2>Ticket Status Update</h2>" +
            "%s" + 
            "<p><strong>Ticket Key:</strong> %s</p>" +
            "<p><strong>New Status:</strong> %s</p>" +
            "<p><strong>Original Email Message ID:</strong> %s</p>" +
            "</body></html>",
            problemMessage,
            jiraKey,
            newStatus,
            emailMessageId
        );
    }
    
    /**
     * Determines if the given status indicates a problem with functionality.
     * 
     * @param status The current ticket status
     * @return true if the status suggests functionality issues, false otherwise
     */
    private boolean isProblemStatus(String status) {
        // Expanded list of statuses that might indicate functionality problems
        String[] problemStatuses = {
            "blocked", 
            "on hold", 
            "in progress", 
            "needs investigation", 
            "bug", 
            "unresolved",
            "to be planned",
            "pending",
            "not started"
        };
        
        // Convert status to lowercase for case-insensitive comparison
        String lowercaseStatus = status.toLowerCase().trim();
        
        // Check for exact match or contains
        for (String problemStatus : problemStatuses) {
            if (lowercaseStatus.equals(problemStatus) || 
                lowercaseStatus.contains(problemStatus)) {
                log.info("Detected problem status: {} matches {}", lowercaseStatus, problemStatus);
                return true;
            }
        }
        
        log.info("Status '{}' not considered a problem status", lowercaseStatus);
        return false;
    }

    /**
     * Normalize the message ID to a standard email Message-ID format.
     * 
     * @param messageId The original message ID
     * @return A normalized Message-ID
     */
    private String normalizeMessageId(String messageId) {
        // If it's already in the correct format, return as-is
        if (messageId.startsWith("<") && messageId.contains("@")) {
            return messageId;
        }
        
        // If it's a UUID, convert to standard Message-ID format
        return "<" + messageId + "@applova.io>";
    }

    /**
     * Generate a new unique Message-ID.
     * 
     * @return A new Message-ID in standard format
     */
    private String generateMessageId() {
        return "<" + UUID.randomUUID().toString() + "@applova.io>";
    }
}

