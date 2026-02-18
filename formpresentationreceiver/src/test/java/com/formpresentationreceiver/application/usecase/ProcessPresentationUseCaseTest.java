package com.formpresentationreceiver.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessPresentationUseCaseTest {

    private ProcessPresentationUseCase processPresentationUseCase;

    @BeforeEach
    void setUp() {
        processPresentationUseCase = new ProcessPresentationUseCase();
    }

    @Test
    void shouldProcessPresentationSuccessfully() {
        UUID presentationId = UUID.randomUUID();

        // Should not throw any exception
        assertDoesNotThrow(() -> processPresentationUseCase.execute(presentationId));
    }

    @Test
    void shouldProcessMultiplePresentationsIndependently() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Should not throw any exception
        assertDoesNotThrow(() -> {
            processPresentationUseCase.execute(id1);
            processPresentationUseCase.execute(id2);
        });
    }

    @Test
    void shouldAcceptNullIdWithoutException() {
        // The use case should handle the ID as provided
        // State management is external, so this just tests the business logic
        assertDoesNotThrow(() -> processPresentationUseCase.execute(null));
    }
}
