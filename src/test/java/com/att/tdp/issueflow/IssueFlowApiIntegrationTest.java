package com.att.tdp.issueflow;

import com.att.tdp.issueflow.projects.Project;
import com.att.tdp.issueflow.projects.ProjectRepository;
import com.att.tdp.issueflow.tickets.Ticket;
import com.att.tdp.issueflow.tickets.TicketPriority;
import com.att.tdp.issueflow.tickets.TicketRepository;
import com.att.tdp.issueflow.tickets.TicketService;
import com.att.tdp.issueflow.tickets.TicketStatus;
import com.att.tdp.issueflow.tickets.TicketType;
import com.att.tdp.issueflow.users.Role;
import com.att.tdp.issueflow.users.User;
import com.att.tdp.issueflow.users.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IssueFlowApiIntegrationTest {
    private static final String DEFAULT_PASSWORD = "issueflow123";

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    TicketService ticketService;

    @Test
    void jwtLoginMeLogoutAndProtectedEndpointBehavior() throws Exception {
        mvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));

        String adminToken = login("admin");

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/logout").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminOnlyDeletedAndRestoreEndpointsRejectDevelopers() throws Exception {
        String adminToken = login("admin");
        String developerToken = login("dev");
        Long projectId = createProject(adminToken, admin().getId()).get("id").asLong();
        Long ticketId = createTicket(adminToken, projectId, "Admin-only test").get("id").asLong();

        mvc.perform(get("/tickets/deleted").param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(developerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/tickets/{ticketId}/restore", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(developerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/projects/deleted").header(HttpHeaders.AUTHORIZATION, bearer(developerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/projects/{projectId}/restore", projectId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(developerToken)))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/tickets/{ticketId}", ticketId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/tickets/deleted").param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ticketId));
    }

    @Test
    void softDeletedProjectsHideNestedTicketsCommentsAttachmentsDependenciesAndCsv() throws Exception {
        String adminToken = login("admin");
        Long adminId = admin().getId();
        Long projectId = createProject(adminToken, adminId).get("id").asLong();
        Long ticketId = createTicket(adminToken, projectId, "Hidden ticket").get("id").asLong();
        Long blockerId = createTicket(adminToken, projectId, "Hidden blocker").get("id").asLong();

        mvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("authorId", adminId, "content", "Visible before delete"))))
                .andExpect(status().isOk());
        mvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("blockedBy", blockerId))))
                .andExpect(status().isOk());
        mvc.perform(delete("/projects/{projectId}", projectId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());

        mvc.perform(get("/projects/{projectId}", projectId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets/{ticketId}", ticketId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets").param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets/{ticketId}/comments", ticketId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets/{ticketId}/dependencies", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets/export").param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        MockMultipartFile file = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void auditLogsPersistManualAndSystemActions() throws Exception {
        String adminToken = login("admin");
        Long adminId = admin().getId();
        Long projectId = createProject(adminToken, adminId).get("id").asLong();
        Long ticketId = createTicket(adminToken, projectId, "Audit ticket").get("id").asLong();

        mvc.perform(get("/audit-logs")
                        .param("entityType", "TICKET")
                        .param("entityId", ticketId.toString())
                        .param("action", "CREATE")
                        .param("actor", "USER")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].performedBy").value(adminId));

        mvc.perform(get("/audit-logs")
                        .param("entityType", "TICKET")
                        .param("entityId", ticketId.toString())
                        .param("action", "AUTO_ASSIGN")
                        .param("actor", "SYSTEM")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].performedBy").doesNotExist());
    }

    @Test
    void csvImportHandlesQuotesAndReportsRowFailuresThenExportQuotesFields() throws Exception {
        String adminToken = login("admin");
        Long projectId = createProject(adminToken, admin().getId()).get("id").asLong();
        Long devId = developer().getId();
        String csv = "id,title,description,status,priority,type,assigneeId\n"
                + ",\"Title, with comma\",\"Desc \"\"quoted\"\"\",TODO,HIGH,BUG," + devId + "\n"
                + ",,missing title,TODO,HIGH,BUG,\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/tickets/import").file(file)
                        .param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors[0]").exists());

        String exported = mvc.perform(get("/tickets/export").param("projectId", projectId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(exported).contains("id,title,description,status,priority,type,assigneeId");
        assertThat(exported).contains("\"Title, with comma\"");
        assertThat(exported).contains("\"Desc \"\"quoted\"\"\"");
    }

    @Test
    void attachmentUploadValidatesContentTypeAndSize() throws Exception {
        String adminToken = login("admin");
        Long projectId = createProject(adminToken, admin().getId()).get("id").asLong();
        Long ticketId = createTicket(adminToken, projectId, "Attachment ticket").get("id").asLong();
        MockMultipartFile valid = new MockMultipartFile(
                "file", "note.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile unsupported = new MockMultipartFile(
                "file", "script.sh", "application/x-sh", "bad".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile tooLarge = new MockMultipartFile(
                "file", "large.txt", "text/plain", new byte[(10 * 1024 * 1024) + 1]);

        mvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(valid)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("note.txt"))
                .andExpect(jsonPath("$.contentType").value("text/plain"));
        mvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(unsupported)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(tooLarge)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mentionsArePersistedAndRecalculatedOnCommentUpdate() throws Exception {
        String adminToken = login("admin");
        Long adminId = admin().getId();
        Long devId = developer().getId();
        Long janeId = createUser(adminToken, "jane", "jane@example.com", "Jane Dev", Role.DEVELOPER).get("id").asLong();
        Long projectId = createProject(adminToken, adminId).get("id").asLong();
        Long ticketId = createTicket(adminToken, projectId, "Mention ticket").get("id").asLong();

        String commentContent = mvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("authorId", adminId, "content", "Hi @dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentionedUsers[0].username").value("dev"))
                .andReturn().getResponse().getContentAsString();
        Long commentId = objectMapper.readTree(commentContent).get("id").asLong();

        mvc.perform(patch("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Hi @JANE"))))
                .andExpect(status().isOk());

        mvc.perform(get("/users/{userId}/mentions", devId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/users/{userId}/mentions", janeId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].mentionedUsers[0].username").value("jane"));
    }

    @Test
    void autoEscalationPersistsSystemAudit() throws Exception {
        String adminToken = login("admin");
        User admin = admin();
        Project project = new Project();
        project.setName("Escalation Project");
        project.setOwner(admin);
        project = projectRepository.save(project);
        Ticket ticket = new Ticket();
        ticket.setTitle("Overdue");
        ticket.setDescription("Overdue description");
        ticket.setProject(project);
        ticket.setStatus(TicketStatus.TODO);
        ticket.setPriority(TicketPriority.LOW);
        ticket.setType(TicketType.BUG);
        ticket.setDueDate(Instant.parse("2026-01-01T00:00:00Z"));
        ticket = ticketRepository.save(ticket);

        ticketService.escalateOverdueTickets();

        mvc.perform(get("/audit-logs")
                        .param("entityType", "TICKET")
                        .param("entityId", ticket.getId().toString())
                        .param("action", "AUTO_ESCALATE")
                        .param("actor", "SYSTEM")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(ticket.getId()));
    }

    private String login(String username) throws Exception {
        String body = json(Map.of("username", username, "password", DEFAULT_PASSWORD));
        String content = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).get("accessToken").asText();
    }

    private JsonNode createProject(String token, Long ownerId) throws Exception {
        String content = mvc.perform(post("/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Project " + System.nanoTime(),
                                "description", "Project description",
                                "ownerId", ownerId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode createTicket(String token, Long projectId, String title) throws Exception {
        String content = mvc.perform(post("/tickets")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", title,
                                "description", title + " description",
                                "status", "TODO",
                                "priority", "HIGH",
                                "type", "BUG",
                                "projectId", projectId))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode createUser(String token, String username, String email, String fullName, Role role) throws Exception {
        String content = mvc.perform(post("/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", email,
                                "fullName", fullName,
                                "role", role.name(),
                                "password", DEFAULT_PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content);
    }

    private User admin() {
        return userRepository.findByUsernameIgnoreCase("admin").orElseThrow();
    }

    private User developer() {
        return userRepository.findByUsernameIgnoreCase("dev").orElseThrow();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

}
