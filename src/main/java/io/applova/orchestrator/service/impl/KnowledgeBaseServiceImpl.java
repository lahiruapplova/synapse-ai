package io.applova.orchestrator.service.impl;

import io.applova.orchestrator.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final WebClient gptWebClient;

    @Value("${api.gpt.model}")
    private String gptModel;

    @Override
    public Mono<String> queryGpt(String subject, String description) {
        // Construct GPT API request payload
        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", gptModel);
        requestPayload.put("messages", List.of(
            Map.of(
                "role", "system", 
                "content", "You are a helpful assistant that provides concise, professional responses to customer inquiries."
            ),
            Map.of(
                "role", "user", 
                "content", String.format("Subject: %s\nDescription: %s\n\nPlease provide a clear, helpful response addressing the key points.", subject, description)
            )
        ));
        requestPayload.put("max_tokens", 250);
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
    }
}

