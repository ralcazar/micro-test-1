package com.formpresentationreceiver.infrastructure.adapter.output.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Panache repository for InboxEntity
 */
@ApplicationScoped
public class InboxEntityRepository implements PanacheRepositoryBase<InboxEntity, UUID> {

    public List<InboxEntity> findUnprocessed(int limit) {
        return find("status = 'PENDING' ORDER BY receivedAt ASC")
                .page(0, limit)
                .list();
    }

    public List<InboxEntity> findUnprocessedSince(LocalDateTime since) {
        return find("status = 'PENDING' AND receivedAt >= ?1 ORDER BY receivedAt ASC", since)
                .list();
    }

    public boolean existsByPresentationId(UUID presentationId) {
        return count("formId", presentationId) > 0;
    }

    public void markAsProcessed(UUID presentationId) {
        update("status = 'DONE', processedAt = ?1 WHERE formId = ?2", LocalDateTime.now(), presentationId);
    }

    public int tryMarkAsProcessing(UUID presentationId) {
        return update("status = 'DOING' WHERE formId = ?1 AND status = 'PENDING'", presentationId);
    }

    public void markAsUnprocessed(UUID presentationId) {
        update("status = 'PENDING', processedAt = null WHERE formId = ?1", presentationId);
    }
}
