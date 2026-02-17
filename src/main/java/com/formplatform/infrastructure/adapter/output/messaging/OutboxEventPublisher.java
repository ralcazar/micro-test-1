package com.formplatform.infrastructure.adapter.output.messaging;

import com.formplatform.domain.port.output.EventPublisher;
import com.formplatform.infrastructure.adapter.output.persistence.OutboxRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import java.util.UUID;

/**
 * EventPublisher that persists events in the outbox (same transaction as form save).
 * Events are later sent to RabbitMQ by OutboxProcessor when the queue is available.
 */
@ApplicationScoped
@Named("outbox")
public class OutboxEventPublisher implements EventPublisher {

    private static final String CHANNEL_FORM_CREATED = "form-created";

    private final OutboxRepository outboxRepository;

    public OutboxEventPublisher(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void publishFormCreated(UUID formId) {
        String payload = String.format("{\"formId\": \"%s\", \"event\": \"FORM_CREATED\"}", formId.toString());
        outboxRepository.save(CHANNEL_FORM_CREATED, payload);
    }
}
