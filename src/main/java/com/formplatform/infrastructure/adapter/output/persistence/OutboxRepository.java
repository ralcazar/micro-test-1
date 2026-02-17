package com.formplatform.infrastructure.adapter.output.persistence;

import java.util.List;

/**
 * Contract for persisting and reading outbox events (resilience when queue is unavailable).
 * Used only by infrastructure: OutboxEventPublisher and OutboxProcessor.
 */
public interface OutboxRepository {

    /**
     * Persist an event to be published later. Must be called in the same transaction as the business operation.
     */
    void save(String channel, String payload);

    /**
     * Find pending events to be sent, ordered by creation date, limited by limit.
     */
    List<PendingOutboxEvent> findPending(int limit);

    void markSent(Long id);

    void markFailed(Long id);

    void incrementRetry(Long id);

    record PendingOutboxEvent(Long id, String channel, String payload, int retryCount) {}
}
