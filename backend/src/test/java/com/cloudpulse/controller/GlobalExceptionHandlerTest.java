package com.cloudpulse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Verifies GlobalExceptionHandler turns a bad request into a consistent 400 ErrorResponse.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "cloudpulse.scheduler.enabled=false")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidServiceRegistration_returns400WithErrorBody() throws Exception {
        // name and healthUrl are blank, so the @NotBlank validation fails.
        String body = "{\"name\":\"\",\"healthUrl\":\"\",\"monitorType\":\"MOCK\"}";

        mockMvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void wrongHttpMethod_returns405() throws Exception {
        // status-events is GET-only; a POST must be 405, not 500.
        mockMvc.perform(post("/api/services/{id}/status-events", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void malformedPathVariable_returns400() throws Exception {
        // a non-UUID id must be 400, not 500.
        mockMvc.perform(get("/api/services/{id}/status-events", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
