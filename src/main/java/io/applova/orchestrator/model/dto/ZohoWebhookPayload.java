package io.applova.orchestrator.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.applova.orchestrator.model.enums.TicketTag;
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
public class ZohoWebhookPayload {
    @NotBlank(message = "Record ID is required")
    @JsonProperty("record_id")
    private String recordId;

    @NotBlank(message = "Subject is required")
    @JsonProperty("subject")
    private String subject;

    @JsonProperty("description")
    private String description;

    @JsonProperty("tag")
    private TicketTag tag;

    @JsonProperty("contact_email")
    private String contactEmail;

    @JsonProperty("created_time")
    private Instant createdTime;

    @JsonProperty("module")
    private String module;
}
