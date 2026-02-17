package com.formplatform.infrastructure.adapter.input.rest;

import com.formplatform.application.exception.InvalidFormException;
import com.formplatform.domain.port.input.SubmitFormCommand;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.UUID;

/**
 * REST input adapter for Form operations
 */
@Path("/api/forms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class FormResource {

    @Inject
    SubmitFormCommand submitFormCommand;

    @POST
    public Response submitForm(@Valid Map<String, Object> formData) {
        try {
            UUID formId = submitFormCommand.execute(formData);
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "id", formId.toString(),
                            "message", "Form submitted successfully"
                    ))
                    .build();
        } catch (InvalidFormException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of("status", "UP")).build();
    }
}
