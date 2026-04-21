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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordUtil passwordUtil;

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
        // Hash the passwords before saving so tests that need login work correctly
        people.forEach(p -> p.setPassword(passwordUtil.hashPassword(p.getPassword())));
        personRepository.saveAll(people);
    }

    private void initializeAuthToken() {
        Person authPerson = personRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No seeded person available for auth token"));
        // Ensure the auth person has ADMIN role for accessing protected routes
        authPerson.setRole(Role.ADMIN);
        personRepository.save(authPerson);
        authToken = jwtUtil.createToken(authPerson);
    }

    @Test
    void testGetPeople() throws Exception {
        mockMvc.perform(get("/person")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name",
                        Matchers.containsInAnyOrder("John Doe", "Jane Doe")))
                .andExpect(jsonPath("$[*].age",
                        Matchers.containsInAnyOrder(30, 25)))
                .andExpect(jsonPath("$[*].email",
                        Matchers.containsInAnyOrder(
                                "john.doe@example.com", "jane.doe@example.com")));
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

    private String loadFixture(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/" + fileName)) {
            if (is == null) {
                throw new FileNotFoundException("Fixture not found: " + fileName);
            }
            return new String(is.readAllBytes());
        }
    }
}