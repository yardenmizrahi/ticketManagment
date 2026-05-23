package com.att.tdp.issueflow.users;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserManagementIntegrationTest {
    private static final String DEFAULT_PASSWORD = "issueflow123";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void createListGetUpdateAndDeleteUserWithoutExposingPasswordHash() throws Exception {
        String token = login("admin");

        JsonNode created = createUser(token, "reviewer", "reviewer@example.com", "Reviewer User", "DEVELOPER");
        Long userId = created.get("id").asLong();

        mvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("reviewer"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$", not(hasKey("password"))));

        mvc.perform(get("/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='reviewer')]").exists());

        mvc.perform(post("/users/update/{userId}", userId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Reviewer Updated", "role", "ADMIN"))))
                .andExpect(status().isOk());

        mvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Reviewer Updated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(delete("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/users/{userId}", userId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateUsernameDuplicateEmailInvalidEmailAndMissingRole() throws Exception {
        String token = login("admin");
        createUser(token, "reviewer", "reviewer@example.com", "Reviewer User", "DEVELOPER");

        mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "REVIEWER",
                                "email", "other@example.com",
                                "fullName", "Duplicate Username",
                                "role", "DEVELOPER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username already exists"));

        mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "other",
                                "email", "REVIEWER@example.com",
                                "fullName", "Duplicate Email",
                                "role", "DEVELOPER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));

        mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "bademail",
                                "email", "not-an-email",
                                "fullName", "Bad Email",
                                "role", "DEVELOPER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "norole",
                                "email", "norole@example.com",
                                "fullName", "No Role"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private String login(String username) throws Exception {
        String content = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", DEFAULT_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).get("accessToken").asText();
    }

    private JsonNode createUser(String token, String username, String email, String fullName, String role) throws Exception {
        String content = mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", email,
                                "fullName", fullName,
                                "role", role,
                                "password", DEFAULT_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
