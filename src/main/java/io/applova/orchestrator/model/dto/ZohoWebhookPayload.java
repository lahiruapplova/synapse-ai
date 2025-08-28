package io.applova.orchestrator.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.applova.orchestrator.model.enums.TicketTag;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZohoWebhookPayload {
    @JsonProperty("iTitle")
    @NotBlank(message = "Issue Title is required")
    private String issueTitle;

    @JsonProperty("iDesc")
    private String issueDescription;

    @JsonProperty("eta")
    private String estimatedTimeOfArrival;

    @JsonProperty("bizProds")
    private List<String> businessProducts;

    @JsonProperty("bizName")
    private String businessName;

    @JsonProperty("bizRevClass")
    private String businessRevenueClass;

    @JsonProperty("bizId")
    private String businessId;

    @JsonProperty("iProds")
    private List<String> issueProducts;

    @JsonProperty("iSev")
    private String issueSeverity;

    @JsonProperty("subUser")
    private String submittingUser;

    @JsonProperty("iType")
    private String issueType;

    @JsonProperty("zohoTic")
    private String zohoTicketNumber;

    // Mapping method to convert severity to Jira priority
    public String mapSeverityToJiraPriority() {
        switch (this.issueSeverity) {
            case "High":
                return "High";
            case "Medium":
                return "Medium";
            case "Low":
                return "Low";
            default:
                return "Medium";
        }
    }

    // Mapping method to convert issue type to Jira issue type
    public String mapIssueTypeToJiraIssueType() {
        switch (this.issueType) {
            case "Bug":
                return "Bug";
            case "Feature":
                return "New Feature";
            default:
                return "Task";
        }
    }
}
