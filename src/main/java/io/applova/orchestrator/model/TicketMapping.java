package io.applova.orchestrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_mappings")
public class TicketMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zoho_record_id", nullable = false)
    private String zohoRecordId;

    @Column(name = "jira_key", nullable = false, unique = true)
    private String jiraKey;

    @Column(name = "email_message_id")
    private String emailMessageId;

    @Column(name = "initial_subject")
    private String initialSubject;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Optional: Add a pre-persist method to set createdAt if not already set
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
