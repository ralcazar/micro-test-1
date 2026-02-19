package com.formpresentationreceiver.infrastructure.adapter.output.persistence;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDateTime;
import java.util.List;
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
                presentationId.value(),
                LocalDateTime.now()
        );
        inboxEntityRepository.persist(entity);
        return presentationId;
    }

    @Override
    public List<PresentationId> findUnprocessed(int limit) {
        return inboxEntityRepository.findUnprocessed(limit).stream()
                .map(entity -> PresentationId.of(entity.getFormId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<PresentationId> findUnprocessedSince(LocalDateTime since) {
        return inboxEntityRepository.findUnprocessedSince(since).stream()
                .map(entity -> PresentationId.of(entity.getFormId()))
                .collect(Collectors.toList());
    }

    @Override
    public void markAsProcessed(PresentationId presentationId) {
        inboxEntityRepository.markAsProcessed(presentationId.value());
    }

    @Override
    public int tryMarkAsProcessing(PresentationId presentationId) {
        return inboxEntityRepository.tryMarkAsProcessing(presentationId.value());
    }

    @Override
    public void markAsUnprocessed(PresentationId presentationId) {
        inboxEntityRepository.markAsUnprocessed(presentationId.value());
    }

    @Override
    public boolean existsByPresentationId(PresentationId presentationId) {
        return inboxEntityRepository.existsByPresentationId(presentationId.value());
    }
}
