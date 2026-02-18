package com.formplatform.application.usecase;

import com.formplatform.application.exception.InvalidFormException;
import com.formplatform.domain.model.Form;
import com.formplatform.domain.port.output.EventPublisher;
import com.formplatform.domain.port.output.FormRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmitFormUseCaseTest {

    @Mock
    private FormRepository formRepository;

    @Mock
    private EventPublisher eventPublisher;

    private SubmitFormUseCase submitFormUseCase;

    @BeforeEach
    void setUp() {
        submitFormUseCase = new SubmitFormUseCase(formRepository, eventPublisher);
    }

    @Test
    void shouldSubmitFormSuccessfully() {
        Map<String, Object> formData = Map.of("name", "John", "email", "john@example.com");
        UUID expectedId = UUID.randomUUID();
        Form savedForm = new Form(expectedId, formData, null);

        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        UUID result = submitFormUseCase.execute(formData);

        assertEquals(expectedId, result);
        verify(formRepository).save(any(Form.class));
        verify(eventPublisher).publishFormCreated(expectedId);
    }

    @Test
    void shouldThrowExceptionWhenFormDataIsNull() {
        InvalidFormException exception = assertThrows(
                InvalidFormException.class,
                () -> submitFormUseCase.execute(null)
        );

        assertEquals("Form data cannot be empty", exception.getMessage());
        verifyNoInteractions(formRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldThrowExceptionWhenFormDataIsEmpty() {
        Map<String, Object> emptyData = new HashMap<>();

        InvalidFormException exception = assertThrows(
                InvalidFormException.class,
                () -> submitFormUseCase.execute(emptyData)
        );

        assertEquals("Form data cannot be empty", exception.getMessage());
        verifyNoInteractions(formRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldCreateFormWithProvidedData() {
        Map<String, Object> formData = Map.of("field1", "value1");
        UUID savedId = UUID.randomUUID();
        Form savedForm = new Form(savedId, formData, null);

        ArgumentCaptor<Form> formCaptor = ArgumentCaptor.forClass(Form.class);
        when(formRepository.save(formCaptor.capture())).thenReturn(savedForm);

        submitFormUseCase.execute(formData);

        Form capturedForm = formCaptor.getValue();
        assertEquals(formData, capturedForm.getData());
        assertNotNull(capturedForm.getCreatedAt());
    }

    @Test
    void shouldPublishEventAfterSaving() {
        Map<String, Object> formData = Map.of("key", "value");
        UUID savedId = UUID.randomUUID();
        Form savedForm = new Form(savedId, formData, null);

        when(formRepository.save(any(Form.class))).thenReturn(savedForm);

        submitFormUseCase.execute(formData);

        var inOrder = inOrder(formRepository, eventPublisher);
        inOrder.verify(formRepository).save(any(Form.class));
        inOrder.verify(eventPublisher).publishFormCreated(savedId);
    }

    @Test
    void shouldNotPublishEventWhenRepositoryFails() {
        Map<String, Object> formData = Map.of("key", "value");

        when(formRepository.save(any(Form.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> submitFormUseCase.execute(formData));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldReturnIdFromSavedForm() {
        Map<String, Object> formData = Map.of("name", "Test");
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        Form savedForm1 = new Form(id1, formData, null);
        Form savedForm2 = new Form(id2, formData, null);

        when(formRepository.save(any(Form.class)))
                .thenReturn(savedForm1)
                .thenReturn(savedForm2);

        UUID result1 = submitFormUseCase.execute(formData);
        UUID result2 = submitFormUseCase.execute(formData);

        assertEquals(id1, result1);
        assertEquals(id2, result2);
        assertNotEquals(result1, result2);
    }
}
