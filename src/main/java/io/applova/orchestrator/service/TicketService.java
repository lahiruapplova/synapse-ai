package io.applova.orchestrator.service;

import io.applova.orchestrator.model.TicketMapping;
import reactor.core.publisher.Mono;

/**
 * Service for managing ticket mappings across different systems.
 */
public interface TicketService {
    /**
     * Save a mapping between Zoho record, Jira ticket, and email message.
     *
     * @param zohoRecordId    The Zoho CRM record ID
     * @param jiraKey         The Jira ticket key
     * @param emailMessageId  The email message ID
     * @param subject         The initial ticket subject
     * @return A Mono containing the saved TicketMapping
     */
    Mono<TicketMapping> saveMapping(String zohoRecordId, String jiraKey, String emailMessageId, String subject);

    /**
     * Find a ticket mapping by Jira key.
     *
     * @param jiraKey The Jira ticket key
     * @return A Mono containing the TicketMapping if found
     */
    Mono<TicketMapping> findByJiraKey(String jiraKey);

    /**
     * Update the status of an existing ticket mapping.
     *
     * @param jiraKey   The Jira ticket key
     * @param newStatus The new status to set
     * @return A Mono containing the updated TicketMapping
     */
    Mono<TicketMapping> updateTicketStatus(String jiraKey, String newStatus);
}
