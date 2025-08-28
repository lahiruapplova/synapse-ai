package io.applova.orchestrator.service;

import reactor.core.publisher.Mono;

/**
 * Service for interacting with a knowledge base or GPT API to generate responses.
 */
public interface KnowledgeBaseService {
    /**
     * Query the GPT API to generate a response based on the subject and description.
     *
     * @param subject     The subject of the query
     * @param description Detailed description or context for the query
     * @return A Mono containing the generated response from the GPT API
     */
    Mono<String> queryGpt(String subject, String description);
}

