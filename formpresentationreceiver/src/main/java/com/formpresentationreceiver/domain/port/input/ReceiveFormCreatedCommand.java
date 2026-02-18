package com.formpresentationreceiver.domain.port.input;

import java.util.UUID;

/**
 * Input port for receiving form created events
 */
public interface ReceiveFormCreatedCommand {

    /**
     * Handle a form created event
     */
    void execute(UUID formId);
}
