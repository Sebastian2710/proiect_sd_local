package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.model.LoanRecord;
import com.andrei.demo.model.LoanRecordCreateDTO;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoanRecordServiceTests {

    @Mock
    private LoanRecordRepository loanRecordRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private LoanRecordService loanRecordService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // --- getAllLoanRecords ---

    @Test
    void testGetAllLoanRecords() {
        List<LoanRecord> records = List.of(new LoanRecord(), new LoanRecord());
        when(loanRecordRepository.findAll()).thenReturn(records);

        List<LoanRecord> result = loanRecordService.getAllLoanRecords();

        assertEquals(2, result.size());
        verify(loanRecordRepository, times(1)).findAll();
    }

    // --- getLoanRecordById ---

    @Test
    void testGetLoanRecordById_Found() {
        UUID uuid = UUID.randomUUID();
        LoanRecord record = new LoanRecord();
        record.setId(uuid);

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(record));

        LoanRecord result = loanRecordService.getLoanRecordById(uuid);

        assertEquals(uuid, result.getId());
    }

    @Test
    void testGetLoanRecordById_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> loanRecordService.getLoanRecordById(uuid));
    }

    // --- addLoanRecord ---

    @Test
    void testAddLoanRecord_Success() throws ValidationException {
        UUID personId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentIds(List.of(equipmentId));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        Person person = new Person();
        person.setId(personId);

        Equipment equipment = new Equipment();
        equipment.setId(equipmentId);
        equipment.setName("Laptop");
        equipment.setStockCount(5);

        LoanRecord saved = new LoanRecord();
        saved.setId(UUID.randomUUID());
        saved.setStatus("ACTIVE");

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findAllById(dto.getEquipmentIds())).thenReturn(List.of(equipment));
        when(equipmentRepository.saveAll(anyList())).thenReturn(List.of(equipment));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenReturn(saved);

        LoanRecord result = loanRecordService.addLoanRecord(dto);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(4, equipment.getStockCount()); // decremented
        verify(loanRecordRepository).save(any(LoanRecord.class));
        verify(equipmentRepository).saveAll(anyList());
    }

    @Test
    void testAddLoanRecord_PersonNotFound() {
        UUID personId = UUID.randomUUID();

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentIds(List.of(UUID.randomUUID()));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> loanRecordService.addLoanRecord(dto));
        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    void testAddLoanRecord_EquipmentNotFound() {
        UUID personId = UUID.randomUUID();
        UUID equipmentId1 = UUID.randomUUID();
        UUID equipmentId2 = UUID.randomUUID();

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentIds(List.of(equipmentId1, equipmentId2));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        Person person = new Person();
        person.setId(personId);

        // Only returns 1 item when 2 were requested
        Equipment equipment = new Equipment();
        equipment.setId(equipmentId1);
        equipment.setStockCount(5);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findAllById(dto.getEquipmentIds())).thenReturn(List.of(equipment));

        assertThrows(ValidationException.class, () -> loanRecordService.addLoanRecord(dto));
        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    void testAddLoanRecord_EquipmentOutOfStock() {
        UUID personId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentIds(List.of(equipmentId));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        Person person = new Person();
        person.setId(personId);

        Equipment equipment = new Equipment();
        equipment.setId(equipmentId);
        equipment.setName("Laptop");
        equipment.setStockCount(0); // out of stock

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findAllById(dto.getEquipmentIds())).thenReturn(List.of(equipment));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> loanRecordService.addLoanRecord(dto));
        assertTrue(ex.getMessage().contains("out of stock"));
        verify(loanRecordRepository, never()).save(any());
    }

    // --- updateLoanRecord ---

    @Test
    void testUpdateLoanRecord_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        LoanRecord existing = new LoanRecord();
        existing.setId(uuid);
        existing.setStatus("ACTIVE");
        existing.setExpectedReturnDate(LocalDate.now().plusDays(7));

        LoanRecord updates = new LoanRecord();
        updates.setExpectedReturnDate(LocalDate.now().plusDays(14));
        updates.setActualReturnDate(LocalDate.now());
        updates.setStatus("RETURNED");

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(loanRecordRepository.save(existing)).thenReturn(existing);

        LoanRecord result = loanRecordService.updateLoanRecord(uuid, updates);

        assertEquals("RETURNED", existing.getStatus());
        assertEquals(LocalDate.now(), existing.getActualReturnDate());
        verify(loanRecordRepository).save(existing);
    }

    @Test
    void testUpdateLoanRecord_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> loanRecordService.updateLoanRecord(uuid, new LoanRecord()));
    }

    // --- patchLoanRecord ---

    @Test
    void testPatchLoanRecord_PartialUpdate() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        LoanRecord existing = new LoanRecord();
        existing.setId(uuid);
        existing.setStatus("ACTIVE");
        existing.setExpectedReturnDate(LocalDate.now().plusDays(7));

        LoanRecord patch = new LoanRecord();
        patch.setStatus("RETURNED");
        // actualReturnDate and expectedReturnDate not set

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(loanRecordRepository.save(existing)).thenReturn(existing);

        loanRecordService.patchLoanRecord(uuid, patch);

        assertEquals("RETURNED", existing.getStatus());
        assertNull(existing.getActualReturnDate()); // unchanged
        verify(loanRecordRepository).save(existing);
    }

    @Test
    void testPatchLoanRecord_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> loanRecordService.patchLoanRecord(uuid, new LoanRecord()));
    }

    // --- deleteLoanRecord ---

    @Test
    void testDeleteLoanRecord() {
        UUID uuid = UUID.randomUUID();
        doNothing().when(loanRecordRepository).deleteById(uuid);

        loanRecordService.deleteLoanRecord(uuid);

        verify(loanRecordRepository, times(1)).deleteById(uuid);
    }
}