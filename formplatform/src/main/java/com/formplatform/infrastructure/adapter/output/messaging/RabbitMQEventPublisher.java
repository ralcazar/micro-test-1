package com.formplatform.infrastructure.adapter.output.messaging;

import com.formplatform.domain.port.output.EventPublisher;
import io.smallrye.reactive.messaging.annotations.Broadcast;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.UUID;

/**
 * RabbitMQ output adapter - sends messages to the form-created channel.
 * Used by OutboxProcessor to publish persisted events when the queue is available.
 */
@ApplicationScoped
public class RabbitMQEventPublisher implements EventPublisher {

    @Channel("form-created")
    @Broadcast
    Emitter<String> formCreatedEmitter;

    @Override
    public void publishFormCreated(UUID formId) {
        String message = String.format("{\"formId\": \"%s\", \"event\": \"FORM_CREATED\"}", formId.toString());
        sendMessage(message);
    }

    /**
     * Sends raw payload to the channel. Used by OutboxProcessor for resilient delivery.
     */
    public void sendMessage(String payload) {
        formCreatedEmitter.send(payload);
    }
}
