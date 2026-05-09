package com.andrei.demo.assistant.controller;

import com.andrei.demo.assistant.client.LlmCallResult;
import com.andrei.demo.assistant.client.LlmClient;
import com.andrei.demo.assistant.client.LlmException;
import com.andrei.demo.assistant.model.AssistantAuditLog;
import com.andrei.demo.assistant.model.RecommendationRequest;
import com.andrei.demo.assistant.model.RecommendationSession;
import com.andrei.demo.assistant.model.RecommendationSessionStatus;
import com.andrei.demo.assistant.model.RecommendationSubmitRequest;
import com.andrei.demo.assistant.repository.AssistantAuditLogRepository;
import com.andrei.demo.assistant.repository.RecommendationSessionRepository;
import com.andrei.demo.assistant.service.RateLimiterService;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProjectAssistantControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RecommendationSessionRepository sessionRepository;
    @Autowired private AssistantAuditLogRepository auditLogRepository;
    @Autowired private RateLimiterService rateLimiterService;
    @Autowired private PersonRepository personRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private LoanRecordRepository loanRecordRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    @MockitoBean
    private LlmClient llmClient;

    private String studentToken;
    private String otherStudentToken;
    private UUID equipmentId;

    private static final String CANNED_LLM_RESPONSE = """
            {
              "projectPlan": "Use the ESP32 dev board for the WiFi connectivity. Power it from USB.",
              "items": [
                {"name": "ESP32 dev board", "quantity": 1, "reason": "needed for WiFi connectivity"}
              ]
            }
            """;

    private static LlmCallResult cannedCallResult() {
        return new LlmCallResult(CANNED_LLM_RESPONSE, 100L, 50, 30);
    }

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        sessionRepository.deleteAll();
        loanRecordRepository.deleteAll();
        personRepository.deleteAll();
        equipmentRepository.deleteAll();
        rateLimiterService.clearAll();

        Person student = new Person();
        student.setName("Test Student");
        student.setEmail("student@test.com");
        student.setPassword(passwordUtil.hashPassword("Password123!"));
        student.setAge(22);
        student.setRole(Role.STUDENT);
        Person savedStudent = personRepository.save(student);
        studentToken = jwtUtil.createToken(savedStudent);

        Person other = new Person();
        other.setName("Other Student");
        other.setEmail("other@test.com");
        other.setPassword(passwordUtil.hashPassword("Password123!"));
        other.setAge(22);
        other.setRole(Role.STUDENT);
        Person savedOther = personRepository.save(other);
        otherStudentToken = jwtUtil.createToken(savedOther);

        Equipment esp32 = new Equipment();
        esp32.setName("ESP32 dev board");
        esp32.setDescription("WiFi+BLE microcontroller, 5V via USB-C");
        esp32.setStockCount(5);
        equipmentId = equipmentRepository.save(esp32).getId();
    }

    // --- POST /assistant/recommend -----------------------------------------

    @Test
    void recommend_asStudent_createsSessionWithItems() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());

        RecommendationRequest req = new RecommendationRequest(
                "Build a WiFi-enabled IoT device that sends data to the cloud.",
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.projectPlan", Matchers.containsString("ESP32")))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].equipment.name").value("ESP32 dev board"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andExpect(jsonPath("$.items[0].availabilityStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.person.email").value("student@test.com"));
    }

    @Test
    void recommend_unauthorized_whenNoToken() throws Exception {
        RecommendationRequest req = new RecommendationRequest(
                "Build a thing for school", LocalDate.now().plusDays(7));
        mockMvc.perform(post("/assistant/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recommend_invalid_descriptionTooShort() throws Exception {
        RecommendationRequest req = new RecommendationRequest(
                "short", LocalDate.now().plusWeeks(2));
        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").exists());
    }

    @Test
    void recommend_invalid_returnDateInPast() throws Exception {
        RecommendationRequest req = new RecommendationRequest(
                "A perfectly valid project description here.",
                LocalDate.now().minusDays(1));
        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expectedReturnDate").exists());
    }

    // --- GET /assistant/recommend/{sessionId} ------------------------------

    @Test
    void getSession_asOwner_returnsSession() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();

        mockMvc.perform(get("/assistant/recommend/" + sessionId)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getSession_byOtherStudent_returns404() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();

        mockMvc.perform(get("/assistant/recommend/" + sessionId)
                        .header("Authorization", "Bearer " + otherStudentToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSession_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/assistant/recommend/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isNotFound());
    }

    // --- POST /assistant/recommend/{sessionId}/submit ----------------------

    @Test
    void submit_asOwner_createsLoanRecordAndDeductsStock() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();
        RecommendationSession created = sessionRepository.findById(sessionId).orElseThrow();
        UUID itemId = created.getItems().get(0).getId();

        RecommendationSubmitRequest submit = new RecommendationSubmitRequest(
                List.of(new RecommendationSubmitRequest.SubmitItem(itemId, 2)),
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend/" + sessionId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.loanRecord.id").exists())
                .andExpect(jsonPath("$.loanRecord.status").value("PROCESSING"));

        Equipment esp32 = equipmentRepository.findById(equipmentId).orElseThrow();
        assertEquals(3, esp32.getStockCount(), "stock should be 5 - 2 = 3");

        RecommendationSession reloaded = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(RecommendationSessionStatus.SUBMITTED, reloaded.getStatus());
        assertNotNull(reloaded.getLoanRecord());
    }

    @Test
    void submit_alreadySubmittedSession_isRejected() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();

        RecommendationSession session = sessionRepository.findById(sessionId).orElseThrow();
        session.setStatus(RecommendationSessionStatus.SUBMITTED);
        sessionRepository.save(session);
        UUID itemId = session.getItems().get(0).getId();

        RecommendationSubmitRequest submit = new RecommendationSubmitRequest(
                List.of(new RecommendationSubmitRequest.SubmitItem(itemId, 1)),
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend/" + sessionId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.business_error").exists());
    }

    @Test
    void submit_byOtherStudent_returns404() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();
        UUID itemId = sessionRepository.findById(sessionId).orElseThrow()
                .getItems().get(0).getId();

        RecommendationSubmitRequest submit = new RecommendationSubmitRequest(
                List.of(new RecommendationSubmitRequest.SubmitItem(itemId, 1)),
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend/" + sessionId + "/submit")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isNotFound());
    }

    @Test
    void submit_unknownItemId_isRejected() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();

        RecommendationSubmitRequest submit = new RecommendationSubmitRequest(
                List.of(new RecommendationSubmitRequest.SubmitItem(UUID.randomUUID(), 1)),
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend/" + sessionId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.business_error").exists());
    }

    @Test
    void submit_emptyItems_isRejected() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());
        UUID sessionId = createSessionForCurrentStudent();

        RecommendationSubmitRequest submit = new RecommendationSubmitRequest(
                List.of(),
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend/" + sessionId + "/submit")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.items").exists());
    }

    // --- Phase 6: rate limiting --------------------------------------------

    @Test
    void recommend_rateLimitExceeded_after5RequestsReturns429() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());

        RecommendationRequest req = new RecommendationRequest(
                "Build a WiFi-enabled IoT device that sends data to the cloud.",
                LocalDate.now().plusWeeks(2)
        );

        // Capacity is 5; first 5 must succeed.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/assistant/recommend")
                            .header("Authorization", "Bearer " + studentToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        // 6th must be rate-limited
        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests());

        // ...but a different user is unaffected
        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // --- Phase 6: audit logging --------------------------------------------

    @Test
    void recommend_success_writesAuditRow() throws Exception {
        when(llmClient.completeRich(anyString(), anyString())).thenReturn(cannedCallResult());

        RecommendationRequest req = new RecommendationRequest(
                "Build a WiFi-enabled IoT device that sends data to the cloud.",
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AssistantAuditLog> audits = auditLogRepository.findAll();
        assertEquals(1, audits.size());
        AssistantAuditLog audit = audits.get(0);
        assertEquals("student@test.com", audit.getPersonEmail());
        assertTrue(audit.isSuccess());
        assertNotNull(audit.getSessionId());
        assertNotNull(audit.getRawLlmResponse());
        assertTrue(audit.getRawLlmResponse().contains("ESP32"));
        assertNotNull(audit.getParsedItemsJson());
        assertTrue(audit.getParsedItemsJson().contains("ESP32"));
        assertEquals(50, audit.getPromptTokens());
        assertEquals(30, audit.getCompletionTokens());
        assertEquals(100L, audit.getLatencyMs());
        assertEquals("v1.0", audit.getPromptVersion());
    }

    @Test
    void recommend_llmFailure_writesFailureAuditRow() throws Exception {
        when(llmClient.completeRich(anyString(), anyString()))
                .thenThrow(new LlmException("simulated provider failure"));

        RecommendationRequest req = new RecommendationRequest(
                "Build a WiFi-enabled IoT device that sends data to the cloud.",
                LocalDate.now().plusWeeks(2)
        );

        mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());

        List<AssistantAuditLog> audits = auditLogRepository.findAll();
        assertEquals(1, audits.size(), "failure should still produce exactly one audit row");
        AssistantAuditLog audit = audits.get(0);
        assertEquals("student@test.com", audit.getPersonEmail());
        assertFalse(audit.isSuccess());
        assertNotNull(audit.getErrorMessage());
        assertTrue(audit.getErrorMessage().contains("simulated provider failure"));
        // No session was created on failure
        org.junit.jupiter.api.Assertions.assertNull(audit.getSessionId());
    }

    // --- helpers -----------------------------------------------------------

    private UUID createSessionForCurrentStudent() throws Exception {
        RecommendationRequest req = new RecommendationRequest(
                "Build a WiFi-enabled IoT device that sends data to the cloud.",
                LocalDate.now().plusWeeks(2)
        );
        String response = mockMvc.perform(post("/assistant/recommend")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}