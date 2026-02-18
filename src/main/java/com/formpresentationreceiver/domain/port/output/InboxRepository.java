package com.formpresentationreceiver.domain.port.output;

import com.formpresentationreceiver.domain.model.PresentationId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Output port for inbox repository operations
 */
public interface InboxRepository {
    
    /**
     * Save a presentation ID to the inbox
     */
    PresentationId save(PresentationId presentationId);
    
    /**
     * Find unprocessed presentation IDs
     */
    List<PresentationId> findUnprocessed(int limit);
    
    /**
     * Find unprocessed presentation IDs from a specific date range
     */
    List<PresentationId> findUnprocessedSince(LocalDateTime since);
    
    /**
     * Mark a presentation ID as processed
     */
    void markAsProcessed(UUID id);
    
    /**
     * Check if a form ID already exists in the inbox
     */
    boolean existsByFormId(UUID formId);
}
