package com.formplatform.infrastructure.adapter.output.persistence;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists and reads outbox events for resilient event publishing.
 * Implements exponential backoff via next_retry_at: events are only picked up
 * once their next_retry_at timestamp has passed.
 */
@ApplicationScoped
public class OutboxEventRepository implements OutboxRepository {

    private final EntityManager entityManager;

    public OutboxEventRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(String channel, String payload) {
        OutboxEventEntity entity = new OutboxEventEntity(channel, payload);
        entityManager.persist(entity);
    }

    @Override
    public List<PendingOutboxEvent> findPending(int limit) {
        // Only return events whose next_retry_at is null (first attempt) or has already passed
        List<OutboxEventEntity> entities = entityManager
                .createQuery(
                        "SELECT e FROM OutboxEventEntity e " +
                        "WHERE e.status = :status " +
                        "  AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
                        "ORDER BY e.createdAt ASC",
                        OutboxEventEntity.class)
                .setParameter("status", OutboxEventEntity.Status.PENDING)
                .setParameter("now", LocalDateTime.now())
                .setMaxResults(limit)
                .getResultList();
        return entities.stream()
                .map(e -> new PendingOutboxEvent(e.getId(), e.getChannel(), e.getPayload(), e.getRetryCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markSent(Long id) {
        OutboxEventEntity e = entityManager.find(OutboxEventEntity.class, id);
        if (e != null) {
            e.setStatus(OutboxEventEntity.Status.SENT);
        }
    }

    @Override
    @Transactional
    public void markFailed(Long id) {
        OutboxEventEntity e = entityManager.find(OutboxEventEntity.class, id);
        if (e != null) {
            e.setStatus(OutboxEventEntity.Status.FAILED);
        }
    }

    /**
     * Increments the retry counter and sets next_retry_at using exponential backoff.
     * Backoff schedule (seconds): 10, 20, 40, 80, 160, 320, ... capped at 1 hour.
     */
    @Override
    @Transactional
    public void incrementRetry(Long id) {
        OutboxEventEntity e = entityManager.find(OutboxEventEntity.class, id);
        if (e != null) {
            int newRetryCount = e.getRetryCount() + 1;
            e.setRetryCount(newRetryCount);

            long backoffSeconds = Math.min((long) (10 * Math.pow(2, newRetryCount - 1)), 3600L);
            e.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
        }
    }
}
