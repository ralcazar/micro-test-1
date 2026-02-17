package com.formplatform.infrastructure.adapter.output.persistence;

import com.formplatform.domain.port.output.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Persists and reads outbox events for resilient event publishing
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
    @SuppressWarnings("unchecked")
    public List<PendingOutboxEvent> findPending(int limit) {
        List<OutboxEventEntity> entities = entityManager
                .createQuery("SELECT e FROM OutboxEventEntity e WHERE e.status = :status ORDER BY e.createdAt ASC", OutboxEventEntity.class)
                .setParameter("status", OutboxEventEntity.Status.PENDING)
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

    @Override
    @Transactional
    public void incrementRetry(Long id) {
        OutboxEventEntity e = entityManager.find(OutboxEventEntity.class, id);
        if (e != null) {
            e.setRetryCount(e.getRetryCount() + 1);
        }
    }
}
