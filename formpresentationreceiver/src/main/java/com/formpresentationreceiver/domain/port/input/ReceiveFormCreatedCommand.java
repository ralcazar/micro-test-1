package com.formpresentationreceiver.domain.port.input;

import com.formpresentationreceiver.domain.model.PresentationId;

/**
 * Input port for receiving form created events
 */
public interface ReceiveFormCreatedCommand {

    /**
     * Handle a form created event
     */
    void execute(PresentationId presentationId);
}
