package com.formpresentationreceiver.infrastructure.adapter.output.persistence;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * H2 implementation of InboxRepository
 */
@ApplicationScoped
public class H2InboxRepository implements InboxRepository {

    private final InboxEntityRepository inboxEntityRepository;

    public H2InboxRepository(InboxEntityRepository inboxEntityRepository) {
        this.inboxEntityRepository = inboxEntityRepository;
    }

    @Override
    public PresentationId save(PresentationId presentationId) {
        InboxEntity entity = new InboxEntity(
                presentationId.getFormId(),
                presentationId.getReceivedAt()
        );
        inboxEntityRepository.persist(entity);
        presentationId.setId(entity.getId());
        return presentationId;
    }

    @Override
    public List<PresentationId> findUnprocessed(int limit) {
        return inboxEntityRepository.findUnprocessed(limit).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<PresentationId> findUnprocessedSince(LocalDateTime since) {
        return inboxEntityRepository.findUnprocessedSince(since).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsProcessed(UUID id) {
        inboxEntityRepository.markAsProcessed(id);
    }

    @Override
    public boolean existsByFormId(UUID formId) {
        return inboxEntityRepository.existsByFormId(formId);
    }

    private PresentationId toDomain(InboxEntity entity) {
        return new PresentationId(
                entity.getId(),
                entity.getFormId(),
                entity.getReceivedAt(),
                entity.isProcessed()
        );
    }
}
