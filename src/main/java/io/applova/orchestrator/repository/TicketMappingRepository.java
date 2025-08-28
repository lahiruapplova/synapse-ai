package io.applova.orchestrator.repository;

import io.applova.orchestrator.model.TicketMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketMappingRepository extends JpaRepository<TicketMapping, Long> {
    /**
     * Find a ticket mapping by its Jira key.
     * 
     * @param jiraKey the unique Jira ticket key
     * @return Optional containing the TicketMapping if found
     */
    Optional<TicketMapping> findByJiraKey(String jiraKey);
}
