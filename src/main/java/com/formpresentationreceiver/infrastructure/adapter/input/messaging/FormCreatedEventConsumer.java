package com.formpresentationreceiver.infrastructure.adapter.input.messaging;

import com.formpresentationreceiver.domain.port.input.ReceiveFormCreatedCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * RabbitMQ consumer for form-created events
 * Input adapter that receives messages and delegates to the use case
 */
@ApplicationScoped
public class FormCreatedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(FormCreatedEventConsumer.class);

    private final ReceiveFormCreatedCommand receiveFormCreatedCommand;
    private final ObjectMapper objectMapper;

    public FormCreatedEventConsumer(ReceiveFormCreatedCommand receiveFormCreatedCommand) {
        this.receiveFormCreatedCommand = receiveFormCreatedCommand;
        this.objectMapper = new ObjectMapper();
    }

    @Incoming("form-created-in")
    @Blocking
    public void consume(String message) {
        try {
            log.info("Received form-created event: {}", message);
            
            // Parse the message to extract formId
            JsonNode jsonNode = objectMapper.readTree(message);
            String formIdStr = jsonNode.get("formId").asText();
            UUID formId = UUID.fromString(formIdStr);
            
            // Delegate to use case
            receiveFormCreatedCommand.execute(formId);
            
        } catch (Exception e) {
            log.error("Error processing form-created event: {}", message, e);
            // In a production system, you might want to send to a dead letter queue
            throw new RuntimeException("Failed to process form-created event", e);
        }
    }
}
