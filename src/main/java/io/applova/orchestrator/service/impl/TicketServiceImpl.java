package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.model.TicketMapping;
import io.applova.orchestrator.repository.TicketMappingRepository;
import io.applova.orchestrator.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMappingRepository ticketMappingRepository;

    @Override
    public Mono<TicketMapping> saveMapping(String zohoRecordId, String jiraKey, String emailMessageId, String subject) {
        // Create a new TicketMapping entity
        TicketMapping ticketMapping = TicketMapping.builder()
                .zohoRecordId(zohoRecordId)
                .jiraKey(jiraKey)
                .emailMessageId(emailMessageId)
                .initialSubject(subject)
                .status("OPEN")
                .createdAt(Instant.now())
                .build();

        // Save the mapping asynchronously
        return Mono.fromCallable(() -> ticketMappingRepository.save(ticketMapping))
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(savedMapping -> log.info("Saved ticket mapping for Jira key: {}", jiraKey))
                .doOnError(ex -> log.error("Error saving ticket mapping: {}", ex.getMessage()));
    }

    @Override
    public Mono<TicketMapping> findByJiraKey(String jiraKey) {
        // Find ticket mapping by Jira key
        return Mono.fromCallable(() -> ticketMappingRepository.findByJiraKey(jiraKey)
                .orElseThrow(() -> new RuntimeException("No ticket mapping found for Jira key: " + jiraKey)))
                .publishOn(Schedulers.boundedElastic())
                .doOnSuccess(mapping -> log.info("Found ticket mapping for Jira key: {}", jiraKey))
                .doOnError(ex -> log.error("Error finding ticket mapping: {}", ex.getMessage()));
    }

    @Override
    public Mono<TicketMapping> updateTicketStatus(String jiraKey, String newStatus) {
        // Find and update ticket mapping status
        return Mono.fromCallable(() -> {
            log.info("Attempting to update ticket status for Jira key: {} to new status: {}", jiraKey, newStatus);
            
            TicketMapping ticketMapping = ticketMappingRepository.findByJiraKey(jiraKey)
                    .orElseThrow(() -> {
                        log.error("No ticket mapping found for Jira key: {}", jiraKey);
                        return new RuntimeException("No ticket mapping found for Jira key: " + jiraKey);
                    });
            
            log.info("Current ticket mapping details before update: {}", ticketMapping);
            
            ticketMapping.setStatus(newStatus);
            TicketMapping updatedMapping = ticketMappingRepository.save(ticketMapping);
            
            log.info("Updated ticket mapping details: {}", updatedMapping);
            return updatedMapping;
        })
        .publishOn(Schedulers.boundedElastic())
        .doOnSuccess(updatedMapping -> log.info("Successfully updated ticket mapping status for Jira key: {} to {}", jiraKey, newStatus))
        .doOnError(ex -> log.error("Error updating ticket mapping status: {}", ex.getMessage(), ex));
    }
}
