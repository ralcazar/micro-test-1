package com.formplatform.infrastructure.adapter.output.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.formplatform.domain.model.Form;
import com.formplatform.domain.port.output.FormRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * H2 Database output adapter implementing FormRepository port
 */
@ApplicationScoped
public class H2FormRepository implements FormRepository {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    EntityManager entityManager;

    @Override
    @Transactional
    public Form save(Form form) {
        try {
            String jsonData = objectMapper.writeValueAsString(form.getData());
            FormEntity entity = new FormEntity(jsonData, form.getCreatedAt());
            entityManager.persist(entity);
            entityManager.flush();

            form.setId(entity.getId());
            return form;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing form data", e);
        }
    }

    @Override
    public Form findById(UUID id) {
        FormEntity entity = entityManager.find(FormEntity.class, id);

        if (entity == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.readValue(entity.getFormData(), Map.class);
            return new Form(entity.getId(), data, entity.getCreatedAt());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing form data", e);
        }
    }
}
