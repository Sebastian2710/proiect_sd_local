package com.andrei.demo.controller;

import com.andrei.demo.model.Person;
import com.andrei.demo.model.Role;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class PersonControllerIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private PersonRepository personRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordUtil passwordUtil;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        personRepository.deleteAll();
        personRepository.flush();
        seedDatabase();
        initializeAuthToken();
    }

    private void seedDatabase() throws Exception {
        String seedDataJson = loadFixture("person_seed.json");
        List<Person> people = objectMapper.readValue(seedDataJson, new TypeReference<>() {});
        people.forEach(p -> p.setPassword(passwordUtil.hashPassword(p.getPassword())));
        personRepository.saveAll(people);
    }

    private void initializeAuthToken() {
        Person authPerson = personRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No seeded person available"));
        authPerson.setRole(Role.ADMIN);
        personRepository.save(authPerson);
        authToken = jwtUtil.createToken(authPerson);
    }

    // --- person CRUD ---

    @Test
    void testGetPeople() throws Exception {
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name",
                        Matchers.containsInAnyOrder("John Doe", "Jane Doe")));
    }

    @Test
    void testGetPeople_Unauthorized() throws Exception {
        mockMvc.perform(get("/person"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAddPerson_ValidPayload() throws Exception {
        String validPersonJson = loadFixture("valid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPersonJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.password", Matchers.startsWith("$2")))
                .andExpect(jsonPath("$.age").value(28))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));
    }

    @Test
    void testAddPerson_InvalidPayload() throws Exception {
        String invalidPersonJson = loadFixture("invalid_person.json");

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPersonJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name")
                        .value("Name should be between 2 and 100 characters"))
                .andExpect(jsonPath("$.password")
                        .value("Password must contain at least 8 characters, including uppercase, lowercase, digit, and special character"))
                .andExpect(jsonPath("$.age")
                        .value("Age is required"))
                .andExpect(jsonPath("$.email")
                        .value("Email is required"));
    }

    @Test
    void testAddPerson_DuplicateEmail() throws Exception {
        String validPersonJson = """
                {
                  "name": "John Doe",
                  "password": "Password123!@#",
                  "age": 30,
                  "email": "john.doe@example.com"
                }
                """;

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPersonJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.business_error").exists());
    }

    @Test
    void testAddPerson_WithAdminRole() throws Exception {
        String adminJson = """
                {
                  "name": "Admin Person",
                  "password": "Securepass123!@#",
                  "age": 35,
                  "email": "admin.person@example.com",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/person")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // --- register (public endpoint) ---

    @Test
    void testRegister_ValidPayload_NoAuthRequired() throws Exception {
        String json = """
                {
                  "name": "New Student",
                  "password": "Securepass123!@#",
                  "age": 22,
                  "email": "new.student@example.com"
                }
                """;

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.email").value("new.student@example.com"))
                .andExpect(jsonPath("$.password", Matchers.startsWith("$2")));
    }

    @Test
    void testRegister_AlwaysStudentRole() throws Exception {
        // Even if ADMIN role is specified, it should be ignored
        String json = """
                {
                  "name": "Fake Admin",
                  "password": "Securepass123!@#",
                  "age": 25,
                  "email": "fake.admin@example.com"
                }
                """;

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void testRegister_DuplicateEmail() throws Exception {
        String json = """
                {
                  "name": "John Duplicate",
                  "password": "Securepass123!@#",
                  "age": 30,
                  "email": "john.doe@example.com"
                }
                """;

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.business_error").exists());
    }

    @Test
    void testRegister_InvalidPayload() throws Exception {
        String json = """
                {
                  "name": "A",
                  "password": "weak",
                  "age": null,
                  "email": ""
                }
                """;

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    private String loadFixture(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + fileName)) {
            if (is == null) throw new FileNotFoundException("Fixture not found: " + fileName);
            return new String(is.readAllBytes());
        }
    }
}