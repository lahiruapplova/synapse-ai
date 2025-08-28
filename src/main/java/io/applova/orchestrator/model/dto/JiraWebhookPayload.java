package io.applova.orchestrator.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraWebhookPayload {
    @NotBlank(message = "Webhook event type is required")
    @JsonProperty("webhookEvent")
    private String webhookEvent;

    @NotBlank(message = "Issue key is required")
    @JsonProperty("issue_key")
    private String issueKey;

    @JsonProperty("issue_id")
    private String issueId;

    @JsonProperty("status")
    private IssueStatus status;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueStatus {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("category")
        private String category;
    }
}
