package com.andrei.demo.controller;

import com.andrei.demo.model.Equipment;
import com.andrei.demo.repository.EquipmentRepository;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class EquipmentControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private static final String FIXTURE_PATH = "src/test/resources/fixtures/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private UUID seededId1;

    @BeforeEach
    void setUp() throws Exception {
        equipmentRepository.deleteAll();
        equipmentRepository.flush();
        seedDatabase();
    }

    private void seedDatabase() throws Exception {
        String seedDataJson = loadFixture("equipment_seed.json");
        List<Equipment> equipmentList =
                objectMapper.readValue(seedDataJson, new TypeReference<>() {});
        List<Equipment> saved = equipmentRepository.saveAll(equipmentList);
        seededId1 = saved.get(0).getId();
    }

    @Test
    void testGetAllEquipment() throws Exception {
        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name",
                        Matchers.containsInAnyOrder("Laptop", "Monitor")));
    }

    @Test
    void testGetEquipmentById_Existing() throws Exception {
        mockMvc.perform(get("/equipment/" + seededId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seededId1.toString()));
    }

    @Test
    void testAddEquipment_ValidPayload() throws Exception {
        String validJson = loadFixture("valid_equipment.json");

        mockMvc.perform(post("/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.stockCount").value(10));
    }

    @Test
    void testAddEquipment_InvalidStockCount() throws Exception {
        String invalidJson = loadFixture("invalid_equipment.json");

        mockMvc.perform(post("/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stockCount")
                        .value("Stock count cannot be a negative value"));
    }

    @Test
    void testUpdateEquipment() throws Exception {
        String updateJson = """
                {
                  "name": "Updated Laptop",
                  "description": "Updated description",
                  "stockCount": 8
                }
                """;

        mockMvc.perform(put("/equipment/" + seededId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.stockCount").value(8));
    }

    @Test
    void testDeleteEquipment() throws Exception {
        mockMvc.perform(delete("/equipment/" + seededId1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
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