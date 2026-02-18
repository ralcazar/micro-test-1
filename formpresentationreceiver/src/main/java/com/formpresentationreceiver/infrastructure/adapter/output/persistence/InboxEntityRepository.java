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
        return find("processed = false ORDER BY receivedAt ASC")
                .page(0, limit)
                .list();
    }

    public List<InboxEntity> findUnprocessedSince(LocalDateTime since) {
        return find("processed = false AND receivedAt >= ?1 ORDER BY receivedAt ASC", since)
                .list();
    }

    public boolean existsByFormId(UUID formId) {
        return count("formId", formId) > 0;
    }

    public void markAsProcessed(UUID id) {
        update("processed = true, processedAt = ?1 WHERE id = ?2", LocalDateTime.now(), id);
    }
}
