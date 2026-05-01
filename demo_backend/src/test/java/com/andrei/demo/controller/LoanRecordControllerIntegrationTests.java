package com.andrei.demo.controller;

import com.andrei.demo.model.*;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class LoanRecordControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private LoanRecordRepository loanRecordRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private EquipmentRepository equipmentRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    private UUID personId;
    private UUID equipmentId;
    private UUID loanId;
    private String authToken;
    private String studentAuthToken;

    @BeforeEach
    void setUp() {
        loanRecordRepository.deleteAll();
        personRepository.deleteAll();
        equipmentRepository.deleteAll();

        // Admin
        Person admin = new Person();
        admin.setName("Test Admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordUtil.hashPassword("Password123!"));
        admin.setAge(30);
        admin.setRole(Role.ADMIN);
        Person savedAdmin = personRepository.save(admin);
        personId = savedAdmin.getId();
        authToken = jwtUtil.createToken(savedAdmin);

        // Student
        Person student = new Person();
        student.setName("Test Student");
        student.setEmail("student@test.com");
        student.setPassword(passwordUtil.hashPassword("Password123!"));
        student.setAge(22);
        student.setRole(Role.STUDENT);
        Person savedStudent = personRepository.save(student);
        studentAuthToken = jwtUtil.createToken(savedStudent);

        // Equipment with 10 in stock
        Equipment equipment = new Equipment();
        equipment.setName("Test Equipment");
        equipment.setDescription("Test description");
        equipment.setStockCount(10);
        Equipment savedEquipment = equipmentRepository.save(equipment);
        equipmentId = savedEquipment.getId();

        // Existing admin loan (qty 1)
        LoanRecord loan = new LoanRecord();
        loan.setPerson(savedAdmin);
        loan.setLoanDate(LocalDate.now());
        loan.setStatus("ACTIVE");
        loan.setExpectedReturnDate(LocalDate.now().plusDays(7));

        LoanEquipmentItem loanItem = new LoanEquipmentItem();
        loanItem.setEquipment(savedEquipment);
        loanItem.setQuantity(1);
        loanItem.setLoanRecord(loan);
        loan.getItems().add(loanItem);

        loanId = loanRecordRepository.save(loan).getId();

        // Deduct stock manually since we bypassed the service
        savedEquipment.setStockCount(9);
        equipmentRepository.save(savedEquipment);
    }

    // --- admin CRUD ---

    @Test
    void testGetAllLoanRecords() throws Exception {
        mockMvc.perform(get("/loan")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].items[0].quantity").value(1));
    }

    @Test
    void testGetAllLoanRecords_Unauthorized() throws Exception {
        mockMvc.perform(get("/loan"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetAllLoanRecords_AsStudent_Forbidden() throws Exception {
        mockMvc.perform(get("/loan")
                        .header("Authorization", "Bearer " + studentAuthToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetLoanRecordById() throws Exception {
        mockMvc.perform(get("/loan/" + loanId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loanId.toString()));
    }

    @Test
    void testAddLoanRecord_Valid() throws Exception {
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(3);

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].equipment.name").value("Test Equipment"));

        Equipment eq = equipmentRepository.findById(equipmentId).orElseThrow();
        assertEquals(6, eq.getStockCount()); // 9 - 3
    }

    @Test
    void testAddLoanRecord_InsufficientStock() throws Exception {
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(100); // way more than available

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.business_error").exists());
    }

    @Test
    void testUpdateLoanRecord_RestoresStockOnReturn() throws Exception {
        // Stock is currently 9 (deducted 1 in setUp)
        LoanRecord update = new LoanRecord();
        update.setStatus("RETURNED");
        update.setActualReturnDate(LocalDate.now());

        mockMvc.perform(put("/loan/" + loanId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        // Stock should be restored: 9 + 1 = 10
        Equipment eq = equipmentRepository.findById(equipmentId).orElseThrow();
        assertEquals(10, eq.getStockCount());
    }

    @Test
    void testDeleteLoanRecord() throws Exception {
        mockMvc.perform(delete("/loan/" + loanId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/loan/" + loanId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    // --- student endpoints ---

    @Test
    void testRequestLoan_AsStudent_Valid() throws Exception {
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(2);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan/request")
                        .header("Authorization", "Bearer " + studentAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.person.email").value("student@test.com"));

        Equipment eq = equipmentRepository.findById(equipmentId).orElseThrow();
        assertEquals(7, eq.getStockCount()); // 9 - 2
    }

    @Test
    void testRequestLoan_Unauthorized() throws Exception {
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(1);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetMyLoanRecords_AsStudent() throws Exception {
        // First request a loan
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(1);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan/request")
                        .header("Authorization", "Bearer " + studentAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        // Fetch student's own loans
        mockMvc.perform(get("/loan/my")
                        .header("Authorization", "Bearer " + studentAuthToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PROCESSING"));
    }

    @Test
    void testGetMyLoanRecords_AsAdmin_ReturnsOwnLoans() throws Exception {
        mockMvc.perform(get("/loan/my")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void testAdminApprovesProcessingLoan() throws Exception {
        // Create student request
        EquipmentQuantityDTO eqQty = new EquipmentQuantityDTO();
        eqQty.setEquipmentId(equipmentId);
        eqQty.setQuantity(2);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(eqQty));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        String responseBody = mockMvc.perform(post("/loan/request")
                        .header("Authorization", "Bearer " + studentAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String newLoanId = objectMapper.readTree(responseBody).get("id").asText();

        // Admin approves (changes PROCESSING → ACTIVE)
        LoanRecord update = new LoanRecord();
        update.setStatus("ACTIVE");

        mockMvc.perform(patch("/loan/" + newLoanId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}