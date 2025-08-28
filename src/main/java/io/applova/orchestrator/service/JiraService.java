package io.applova.orchestrator.service;

import io.applova.orchestrator.model.dto.ZohoWebhookPayload;
import reactor.core.publisher.Mono;

/**
 * Service for interacting with Jira API to create and manage tickets.
 */
public interface JiraService {
    /**
     * Create a Jira ticket based on the Zoho webhook payload.
     *
     * @param payload The Zoho webhook payload containing ticket details
     * @return A Mono containing the created Jira ticket key
     */
    Mono<String> createTicket(ZohoWebhookPayload payload);

    /**
     * Update an existing Jira ticket's status.
     *
     * @param ticketKey The unique key of the Jira ticket
     * @param newStatus The new status to set for the ticket
     * @return A Mono indicating the completion of the update
     */
    Mono<Void> updateTicketStatus(String ticketKey, String newStatus);
}

