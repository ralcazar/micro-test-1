package com.formpresentationreceiver.domain.port.output;

import com.formpresentationreceiver.domain.model.PresentationId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Output port for inbox repository operations
 */
public interface InboxRepository {

    /**
     * Save a presentation ID to the inbox
     * Returns the saved presentation ID with its internal database ID
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
    void markAsProcessed(PresentationId id);

    /**
     * Try to atomically mark a presentation as processing.
     * Returns the number of rows updated (1 if successful, 0 if already processed by another instance)
     */
    int tryMarkAsProcessing(PresentationId id);

    /**
     * Mark a presentation ID as unprocessed (for error recovery)
     */
    void markAsUnprocessed(PresentationId id);

    /**
     * Check if a presentation ID already exists in the inbox
     */
    boolean existsByPresentationId(PresentationId presentationId);
}
