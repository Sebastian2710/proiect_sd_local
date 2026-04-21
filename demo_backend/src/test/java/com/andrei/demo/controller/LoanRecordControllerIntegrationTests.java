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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class LoanRecordControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRecordRepository loanRecordRepository;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordUtil passwordUtil;

    private UUID personId;
    private UUID equipmentId;
    private UUID loanId;
    private String authToken;

    @BeforeEach
    void setUp() {
        loanRecordRepository.deleteAll();
        personRepository.deleteAll();
        equipmentRepository.deleteAll();

        Person person = new Person();
        person.setName("Test User");
        person.setEmail("test@example.com");
        person.setPassword(passwordUtil.hashPassword("Password123!"));
        person.setAge(20);
        person.setRole(Role.ADMIN);
        Person savedPerson = personRepository.save(person);
        personId = savedPerson.getId();
        authToken = jwtUtil.createToken(savedPerson);

        Equipment equipment = new Equipment();
        equipment.setName("Test Equipment");
        equipment.setStockCount(10);
        equipment.setDescription("Test description");
        equipmentId = equipmentRepository.save(equipment).getId();

        LoanRecord loan = new LoanRecord();
        loan.setPerson(savedPerson);
        loan.setEquipmentList(List.of(equipment));
        loan.setLoanDate(LocalDate.now());
        loan.setStatus("ACTIVE");
        loan.setExpectedReturnDate(LocalDate.now().plusDays(7));
        loanId = loanRecordRepository.save(loan).getId();
    }

    @Test
    void testGetAllLoanRecords() throws Exception {
        mockMvc.perform(get("/loan")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void testGetAllLoanRecords_Unauthorized() throws Exception {
        mockMvc.perform(get("/loan"))
                .andExpect(status().isUnauthorized());
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
        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentIds(List.of(equipmentId));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/loan")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void testUpdateLoanRecord() throws Exception {
        LoanRecord update = new LoanRecord();
        update.setStatus("RETURNED");
        update.setActualReturnDate(LocalDate.now());

        mockMvc.perform(put("/loan/" + loanId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
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
}