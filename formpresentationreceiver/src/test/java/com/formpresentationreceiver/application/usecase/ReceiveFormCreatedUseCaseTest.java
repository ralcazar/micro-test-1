package com.formpresentationreceiver.application.usecase;

import com.formpresentationreceiver.domain.model.PresentationId;
import com.formpresentationreceiver.domain.port.output.InboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiveFormCreatedUseCaseTest {

    @Mock
    private InboxRepository inboxRepository;

    private ReceiveFormCreatedUseCase receiveFormCreatedUseCase;

    @BeforeEach
    void setUp() {
        receiveFormCreatedUseCase = new ReceiveFormCreatedUseCase(inboxRepository);
    }

    @Test
    void shouldSaveNewFormIdToInbox() {
        UUID formId = UUID.randomUUID();
        when(inboxRepository.existsByFormId(formId)).thenReturn(false);

        receiveFormCreatedUseCase.execute(formId);

        verify(inboxRepository).save(any(PresentationId.class));
    }

    @Test
    void shouldSkipDuplicateFormId() {
        UUID formId = UUID.randomUUID();
        when(inboxRepository.existsByFormId(formId)).thenReturn(true);

        receiveFormCreatedUseCase.execute(formId);

        verify(inboxRepository, never()).save(any(PresentationId.class));
    }

    @Test
    void shouldCheckForExistingFormIdBeforeSaving() {
        UUID formId = UUID.randomUUID();
        when(inboxRepository.existsByFormId(formId)).thenReturn(false);

        receiveFormCreatedUseCase.execute(formId);

        var inOrder = inOrder(inboxRepository);
        inOrder.verify(inboxRepository).existsByFormId(formId);
        inOrder.verify(inboxRepository).save(any(PresentationId.class));
    }

    @Test
    void shouldCreatePresentationIdWithCorrectFormId() {
        UUID formId = UUID.randomUUID();
        when(inboxRepository.existsByFormId(formId)).thenReturn(false);

        ArgumentCaptor<PresentationId> captor = ArgumentCaptor.forClass(PresentationId.class);

        receiveFormCreatedUseCase.execute(formId);

        verify(inboxRepository).save(captor.capture());
        PresentationId saved = captor.getValue();

        assertEquals(formId, saved.getFormId());
        assertNotNull(saved.getReceivedAt());
        assertFalse(saved.isProcessed());
    }

    @Test
    void shouldHandleMultipleNewFormIds() {
        UUID formId1 = UUID.randomUUID();
        UUID formId2 = UUID.randomUUID();

        when(inboxRepository.existsByFormId(formId1)).thenReturn(false);
        when(inboxRepository.existsByFormId(formId2)).thenReturn(false);

        receiveFormCreatedUseCase.execute(formId1);
        receiveFormCreatedUseCase.execute(formId2);

        verify(inboxRepository, times(2)).save(any(PresentationId.class));
    }

    @Test
    void shouldNotSaveWhenFormIdAlreadyExists() {
        UUID existingFormId = UUID.randomUUID();
        when(inboxRepository.existsByFormId(existingFormId)).thenReturn(true);

        receiveFormCreatedUseCase.execute(existingFormId);

        verify(inboxRepository).existsByFormId(existingFormId);
        verify(inboxRepository, never()).save(any());
    }

    @Test
    void shouldHandleMixOfNewAndDuplicateFormIds() {
        UUID newFormId = UUID.randomUUID();
        UUID duplicateFormId = UUID.randomUUID();

        when(inboxRepository.existsByFormId(newFormId)).thenReturn(false);
        when(inboxRepository.existsByFormId(duplicateFormId)).thenReturn(true);

        receiveFormCreatedUseCase.execute(newFormId);
        receiveFormCreatedUseCase.execute(duplicateFormId);

        verify(inboxRepository, times(1)).save(any(PresentationId.class));
    }
}
