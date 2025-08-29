package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import org.springframework.stereotype.Component;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final WebClient gptWebClient;
    private final WebClient knowledgeBaseWebClient; // New WebClient for knowledge base

    @Value("${api.gpt.model}")
    private String gptModel;

    @Value("${knowledgebase.url}")
    private String knowledgeBaseUrl;

    // Constructor with @Qualifier for knowledgeBaseWebClient
    public KnowledgeBaseServiceImpl(
        WebClient gptWebClient, 
        @Qualifier("knowledgeBaseWebClient") WebClient knowledgeBaseWebClient
    ) {
        this.gptWebClient = gptWebClient;
        this.knowledgeBaseWebClient = knowledgeBaseWebClient;
    }

    // New method to fetch knowledge base content
    private Mono<String> fetchKnowledgeBaseContent(String subject) {
        return knowledgeBaseWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/search")
                .queryParam("query", subject)
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(ex -> {
                log.error("Error fetching knowledge base content: {}", ex.getMessage());
                return Mono.just("No relevant knowledge base content found.");
            });
    }

    @Override
    public Mono<String> queryGpt(String subject, String description) {
        // First, fetch relevant knowledge base content
        return fetchKnowledgeBaseContent(subject)
            .flatMap(knowledgeBaseContent -> {
                // Construct GPT API request payload
                Map<String, Object> requestPayload = new HashMap<>();
                requestPayload.put("model", gptModel);
                requestPayload.put("messages", List.of(
                    Map.of(
                        "role", "system", 
                        "content", "You are a helpful and knowledgeable chatbot assistant for merchants using the Applova SaaS platform. " +
                        "Your only source of truth is the content available on the official knowledge base.\n\n" +
                        "Available Knowledge Base Content:\n" + knowledgeBaseContent + "\n\n" +
                        "Key Guidelines:\n" +
                        "- Answer merchant questions using ONLY the provided knowledge base information\n" +
                        "- If the answer is not in the knowledge base, clearly state this\n" +
                        "- Encourage users to contact support or request features if information is missing\n" +
                        "- Summarize or link to specific help articles when possible\n" +
                        "- Clarify unclear questions before answering\n" +
                        "- Provide examples or instructions ONLY if explicitly documented\n\n" +
                        "CRITICAL RULES:\n" +
                        "- Never invent or guess features\n" +
                        "- Do not offer advice on undocumented features\n" +
                        "- Do not assume functionality not in documentation\n" +
                        "- If unsure, respond: 'I'm sorry, but I can only provide support for features documented in our help center. Please reach out to our support team for further assistance.'"
                    ),
                    Map.of(
                        "role", "user", 
                        "content", String.format("Subject: %s\nDescription: %s\n\n" +
                        "Please carefully review the provided knowledge base content and provide a precise, documentation-based response " +
                        "addressing the merchant's inquiry. If the information is not available, clearly state that.", subject, description)
                    )
                ));
                requestPayload.put("max_tokens", 4096);
                requestPayload.put("temperature", 0.7);

                // Send POST request to GPT API
                return gptWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestPayload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> {
                        // Extract the generated response
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                            String generatedResponse = (String) message.get("content");
                            log.info("Generated GPT response for subject: {}", subject);
                            return generatedResponse.trim();
                        }
                        log.warn("No response generated for subject: {}", subject);
                        return "Unable to generate a response at this time.";
                    })
                    .doOnError(ex -> log.error("Error querying GPT API: {}", ex.getMessage()));
            });
    }
}

