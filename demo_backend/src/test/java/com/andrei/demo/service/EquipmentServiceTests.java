package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.repository.EquipmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EquipmentServiceTests {

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private EquipmentService equipmentService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testGetAllEquipment() {
        List<Equipment> equipment = List.of(new Equipment(), new Equipment());
        when(equipmentRepository.findAll()).thenReturn(equipment);

        List<Equipment> result = equipmentService.getAllEquipment();

        assertEquals(2, result.size());
        verify(equipmentRepository, times(1)).findAll();
    }

    @Test
    void testGetEquipmentById_Found() {
        UUID uuid = UUID.randomUUID();
        Equipment equipment = new Equipment();
        equipment.setId(uuid);

        when(equipmentRepository.findById(uuid)).thenReturn(Optional.of(equipment));

        Equipment result = equipmentService.getEquipmentById(uuid);

        assertEquals(uuid, result.getId());
    }

    @Test
    void testGetEquipmentById_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(equipmentRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> equipmentService.getEquipmentById(uuid));
    }

    @Test
    void testAddEquipment() {
        Equipment equipment = new Equipment();
        equipment.setName("Laptop");
        equipment.setDescription("Dev laptop");
        equipment.setStockCount(5);

        Equipment saved = new Equipment();
        saved.setId(UUID.randomUUID());
        saved.setName("Laptop");
        saved.setDescription("Dev laptop");
        saved.setStockCount(5);

        when(equipmentRepository.save(equipment)).thenReturn(saved);

        Equipment result = equipmentService.addEquipment(equipment);

        assertNotNull(result.getId());
        assertEquals("Laptop", result.getName());
        verify(equipmentRepository, times(1)).save(equipment);
    }

    @Test
    void testUpdateEquipment_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Equipment existing = new Equipment();
        existing.setId(uuid);
        existing.setName("Old Name");
        existing.setDescription("Old Desc");
        existing.setStockCount(3);

        Equipment updates = new Equipment();
        updates.setName("New Name");
        updates.setDescription("New Desc");
        updates.setStockCount(10);

        when(equipmentRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(existing)).thenReturn(existing);

        Equipment result = equipmentService.updateEquipment(uuid, updates);

        assertEquals("New Name", existing.getName());
        assertEquals("New Desc", existing.getDescription());
        assertEquals(10, existing.getStockCount());
        verify(equipmentRepository).save(existing);
    }

    @Test
    void testUpdateEquipment_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(equipmentRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> equipmentService.updateEquipment(uuid, new Equipment()));
    }

    @Test
    void testPatchEquipment_PartialUpdate() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Equipment existing = new Equipment();
        existing.setId(uuid);
        existing.setName("Original");
        existing.setDescription("Original Desc");
        existing.setStockCount(5);

        Equipment patch = new Equipment();
        patch.setName("Patched");
        // description and stockCount not set

        when(equipmentRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(equipmentRepository.save(existing)).thenReturn(existing);

        equipmentService.patchEquipment(uuid, patch);

        assertEquals("Patched", existing.getName());
        assertEquals("Original Desc", existing.getDescription()); // unchanged
        assertEquals(5, existing.getStockCount());               // unchanged
        verify(equipmentRepository).save(existing);
    }

    @Test
    void testPatchEquipment_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(equipmentRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> equipmentService.patchEquipment(uuid, new Equipment()));
    }

    @Test
    void testDeleteEquipment() {
        UUID uuid = UUID.randomUUID();
        doNothing().when(equipmentRepository).deleteById(uuid);

        equipmentService.deleteEquipment(uuid);

        verify(equipmentRepository, times(1)).deleteById(uuid);
    }
}