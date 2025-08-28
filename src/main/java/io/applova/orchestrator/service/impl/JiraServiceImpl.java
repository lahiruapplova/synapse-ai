package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.model.dto.ZohoWebhookPayload;
import io.applova.orchestrator.service.JiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraServiceImpl implements JiraService {

    private final WebClient jiraWebClient;

    @Value("${api.jira.project-key}")
    private String projectKey;

    @Override
    public Mono<String> createTicket(ZohoWebhookPayload payload) {
        // Construct Jira ticket creation payload
        Map<String, Object> issuePayload = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();

        // Set project
        Map<String, String> project = new HashMap<>();
        project.put("key", projectKey);
        fields.put("project", project);

        // Set summary and description
        fields.put("summary", payload.getIssueTitle());
        fields.put("description", buildTicketDescription(payload));

        // Set issue type based on payload
        Map<String, String> issueType = new HashMap<>();
        issueType.put("name", payload.mapIssueTypeToJiraIssueType());
        fields.put("issuetype", issueType);

        // Set priority based on severity
        Map<String, String> priority = new HashMap<>();
        priority.put("name", payload.mapSeverityToJiraPriority());
        fields.put("priority", priority);

        issuePayload.put("fields", fields);

        // Send POST request to Jira API
        return jiraWebClient.post()
                .uri("/issue")
                .bodyValue(issuePayload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    // Extract and return the Jira ticket key
                    String key = (String) response.get("key");
                    log.info("Created Jira ticket: {}", key);
                    return key;
                })
                .doOnError(ex -> log.error("Error creating Jira ticket: {}", ex.getMessage()));
    }

    @Override
    public Mono<Void> updateTicketStatus(String ticketKey, String newStatus) {
        // Construct status update payload
        Map<String, Object> statusPayload = new HashMap<>();
        Map<String, Object> transition = new HashMap<>();
        transition.put("id", newStatus);
        statusPayload.put("transition", transition);

        // Send POST request to Jira API for status transition
        return jiraWebClient.post()
                .uri("/issue/{ticketKey}/transitions", ticketKey)
                .bodyValue(statusPayload)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> log.info("Updated Jira ticket {} status to {}", ticketKey, newStatus))
                .doOnError(ex -> log.error("Error updating Jira ticket status: {}", ex.getMessage()));
    }

    /**
     * Build a detailed description for the Jira ticket from Zoho payload.
     *
     * @param payload Zoho webhook payload
     * @return Formatted description string
     */
    private String buildTicketDescription(ZohoWebhookPayload payload) {
        StringBuilder description = new StringBuilder();
        description.append("*Ticket Details from Zoho CRM*\n\n");
        description.append("*Issue Title:* ").append(payload.getIssueTitle()).append("\n");
        description.append("*Issue Description:* ").append(payload.getIssueDescription()).append("\n");
        description.append("*Business Name:* ").append(payload.getBusinessName()).append("\n");
        description.append("*Business ID:* ").append(payload.getBusinessId()).append("\n");
        description.append("*Business Revenue Class:* ").append(payload.getBusinessRevenueClass()).append("\n");
        description.append("*Business Products:* ").append(String.join(", ", payload.getBusinessProducts())).append("\n");
        description.append("*Issue Products:* ").append(String.join(", ", payload.getIssueProducts())).append("\n");
        description.append("*Submitting User:* ").append(payload.getSubmittingUser()).append("\n");
        description.append("*Estimated Time of Arrival:* ").append(payload.getEstimatedTimeOfArrival()).append("\n");
        description.append("*Zoho Ticket Number:* ").append(payload.getZohoTicketNumber()).append("\n");

        return description.toString();
    }
}

