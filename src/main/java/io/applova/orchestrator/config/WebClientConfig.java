package io.applova.orchestrator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;

@Configuration
public class WebClientConfig {

    @Value("${api.jira.base-url}")
    private String jiraBaseUrl;

    @Value("${api.jira.username}")
    private String jiraUsername;

    @Value("${api.jira.api-token}")
    private String jiraApiToken;

    @Value("${api.gpt.base-url}")
    private String gptBaseUrl;

    @Value("${api.gpt.api-key}")
    private String gptApiKey;

    @Bean
    public WebClient jiraWebClient() {
        // Create Basic Auth header
        String credentials = jiraUsername + ":" + jiraApiToken;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        return WebClient.builder()
                .baseUrl(jiraBaseUrl)
                .defaultHeader("Authorization", "Basic " + encodedCredentials)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public WebClient gptWebClient() {
        return WebClient.builder()
                .baseUrl(gptBaseUrl)
                .defaultHeader("Authorization", "Bearer " + gptApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
