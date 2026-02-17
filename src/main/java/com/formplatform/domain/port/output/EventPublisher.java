package com.formplatform.domain.port.output;

import java.util.UUID;

/**
 * Output port for publishing events
 */
public interface EventPublisher {
    void publishFormCreated(UUID formId);
}
