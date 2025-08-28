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
        fields.put("summary", payload.getSubject());
        fields.put("description", buildTicketDescription(payload));

        // Set issue type (assuming "Task" type)
        Map<String, String> issueType = new HashMap<>();
        issueType.put("name", "Task");
        fields.put("issuetype", issueType);

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
        description.append("*Subject:* ").append(payload.getSubject()).append("\n");
        description.append("*Description:* ").append(payload.getDescription()).append("\n");
        description.append("*Contact Email:* ").append(payload.getContactEmail()).append("\n");
        description.append("*Zoho Record ID:* ").append(payload.getRecordId()).append("\n");
        description.append("*Tag:* ").append(payload.getTag()).append("\n");
        description.append("*Created Time:* ").append(payload.getCreatedTime()).append("\n");

        return description.toString();
    }
}

