package io.applova.orchestrator.model.enums;

/**
 * Enum representing different ticket tags for routing and processing logic.
 */
public enum TicketTag {
    /**
     * Represents a new feature request that requires GPT-based clarification.
     */
    FEATURE,

    /**
     * Represents a request for clarification on an existing feature or requirement.
     */
    CLARIFICATION,

    /**
     * Represents a standard support ticket that doesn't require GPT processing.
     */
    SUPPORT,

    /**
     * Represents a bug report that needs immediate attention.
     */
    BUG,

    /**
     * Represents an unclassified or unknown tag.
     */
    UNKNOWN
}
