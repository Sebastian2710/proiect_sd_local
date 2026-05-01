package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.*;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

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

    // --- helpers ---

    private EquipmentQuantityDTO makeQtyDTO(UUID equipmentId, int qty) {
        EquipmentQuantityDTO dto = new EquipmentQuantityDTO();
        dto.setEquipmentId(equipmentId);
        dto.setQuantity(qty);
        return dto;
    }

    private Equipment makeEquipment(UUID id, String name, int stock) {
        Equipment eq = new Equipment();
        eq.setId(id);
        eq.setName(name);
        eq.setStockCount(stock);
        return eq;
    }

    // --- getAllLoanRecords ---

    @Test
    void testGetAllLoanRecords() {
        when(loanRecordRepository.findAll()).thenReturn(List.of(new LoanRecord(), new LoanRecord()));
        assertEquals(2, loanRecordService.getAllLoanRecords().size());
        verify(loanRecordRepository, times(1)).findAll();
    }

    // --- getLoanRecordById ---

    @Test
    void testGetLoanRecordById_Found() {
        UUID uuid = UUID.randomUUID();
        LoanRecord record = new LoanRecord();
        record.setId(uuid);
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(record));
        assertEquals(uuid, loanRecordService.getLoanRecordById(uuid).getId());
    }

    @Test
    void testGetLoanRecordById_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> loanRecordService.getLoanRecordById(uuid));
    }

    // --- addLoanRecord ---

    @Test
    void testAddLoanRecord_Success() throws ValidationException {
        UUID personId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        Equipment equipment = makeEquipment(equipmentId, "Laptop", 5);
        Person person = new Person();
        person.setId(personId);

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(makeQtyDTO(equipmentId, 2)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        LoanRecord saved = new LoanRecord();
        saved.setId(UUID.randomUUID());
        saved.setStatus("ACTIVE");

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.saveAll(anyList())).thenReturn(List.of(equipment));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenReturn(saved);

        LoanRecord result = loanRecordService.addLoanRecord(dto);

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(3, equipment.getStockCount()); // 5 - 2
        verify(equipmentRepository).saveAll(anyList());
        verify(loanRecordRepository).save(any(LoanRecord.class));
    }

    @Test
    void testAddLoanRecord_PersonNotFound() {
        UUID personId = UUID.randomUUID();
        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(makeQtyDTO(UUID.randomUUID(), 1)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findById(personId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> loanRecordService.addLoanRecord(dto));
        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    void testAddLoanRecord_EquipmentNotFound() throws Exception {
        UUID personId = UUID.randomUUID();
        UUID eq1Id = UUID.randomUUID();
        UUID eq2Id = UUID.randomUUID();

        Person person = new Person();
        person.setId(personId);

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(makeQtyDTO(eq1Id, 1), makeQtyDTO(eq2Id, 1)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findById(eq1Id)).thenReturn(Optional.of(makeEquipment(eq1Id, "Laptop", 5)));
        when(equipmentRepository.findById(eq2Id)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> loanRecordService.addLoanRecord(dto));
        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    void testAddLoanRecord_InsufficientStock() {
        UUID personId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        Person person = new Person();
        person.setId(personId);
        Equipment equipment = makeEquipment(equipmentId, "Laptop", 2);

        LoanRecordCreateDTO dto = new LoanRecordCreateDTO();
        dto.setPersonId(personId);
        dto.setEquipmentQuantities(List.of(makeQtyDTO(equipmentId, 5))); // requesting 5, only 2 available
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));

        ValidationException ex = assertThrows(ValidationException.class,
                () -> loanRecordService.addLoanRecord(dto));
        assertTrue(ex.getMessage().contains("stock"));
        verify(loanRecordRepository, never()).save(any());
    }

    // --- requestLoan ---

    @Test
    void testRequestLoan_Success() throws ValidationException {
        String email = "student@example.com";
        UUID equipmentId = UUID.randomUUID();

        Person person = new Person();
        person.setEmail(email);
        Equipment equipment = makeEquipment(equipmentId, "Camera", 3);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(makeQtyDTO(equipmentId, 2)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        LoanRecord saved = new LoanRecord();
        saved.setId(UUID.randomUUID());
        saved.setStatus("PROCESSING");

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));
        when(equipmentRepository.saveAll(anyList())).thenReturn(List.of(equipment));
        when(loanRecordRepository.save(any(LoanRecord.class))).thenReturn(saved);

        LoanRecord result = loanRecordService.requestLoan(dto, email);

        assertNotNull(result);
        assertEquals("PROCESSING", result.getStatus());
        assertEquals(1, equipment.getStockCount()); // 3 - 2
    }

    @Test
    void testRequestLoan_PersonNotFound() {
        String email = "ghost@example.com";
        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(makeQtyDTO(UUID.randomUUID(), 1)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> loanRecordService.requestLoan(dto, email));
        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    void testRequestLoan_OutOfStock() {
        String email = "student@example.com";
        UUID equipmentId = UUID.randomUUID();

        Person person = new Person();
        person.setEmail(email);
        Equipment equipment = makeEquipment(equipmentId, "Tablet", 0);

        StudentLoanRequestDTO dto = new StudentLoanRequestDTO();
        dto.setEquipmentQuantities(List.of(makeQtyDTO(equipmentId, 1)));
        dto.setExpectedReturnDate(LocalDate.now().plusDays(7));

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(equipmentRepository.findById(equipmentId)).thenReturn(Optional.of(equipment));

        assertThrows(ValidationException.class, () -> loanRecordService.requestLoan(dto, email));
        verify(loanRecordRepository, never()).save(any());
    }

    // --- getLoanRecordsByPersonEmail ---

    @Test
    void testGetLoanRecordsByPersonEmail() {
        String email = "student@example.com";
        when(loanRecordRepository.findByPerson_Email(email))
                .thenReturn(List.of(new LoanRecord(), new LoanRecord()));
        assertEquals(2, loanRecordService.getLoanRecordsByPersonEmail(email).size());
    }

    @Test
    void testGetLoanRecordsByPersonEmail_NoRecords() {
        String email = "nobody@example.com";
        when(loanRecordRepository.findByPerson_Email(email)).thenReturn(List.of());
        assertTrue(loanRecordService.getLoanRecordsByPersonEmail(email).isEmpty());
    }

    // --- updateLoanRecord / patchLoanRecord with stock restoration ---

    @Test
    void testUpdateLoanRecord_RestoresStockOnReturn() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        Equipment eq = makeEquipment(equipmentId, "Laptop", 4); // current stock after loan
        LoanEquipmentItem item = new LoanEquipmentItem();
        item.setEquipment(eq);
        item.setQuantity(1);

        LoanRecord existing = new LoanRecord();
        existing.setId(uuid);
        existing.setStatus("ACTIVE");
        existing.getItems().add(item);
        item.setLoanRecord(existing);

        LoanRecord updates = new LoanRecord();
        updates.setStatus("RETURNED");
        updates.setActualReturnDate(LocalDate.now());

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(loanRecordRepository.save(existing)).thenReturn(existing);
        when(equipmentRepository.saveAll(anyList())).thenReturn(List.of(eq));

        loanRecordService.updateLoanRecord(uuid, updates);

        assertEquals("RETURNED", existing.getStatus());
        assertEquals(5, eq.getStockCount()); // 4 + 1 restored
        verify(equipmentRepository).saveAll(anyList());
    }

    @Test
    void testUpdateLoanRecord_DoesNotRestoreStockIfAlreadyReturned() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        Equipment eq = makeEquipment(equipmentId, "Laptop", 5);
        LoanEquipmentItem item = new LoanEquipmentItem();
        item.setEquipment(eq);
        item.setQuantity(1);

        LoanRecord existing = new LoanRecord();
        existing.setId(uuid);
        existing.setStatus("RETURNED"); // already returned
        existing.getItems().add(item);
        item.setLoanRecord(existing);

        LoanRecord updates = new LoanRecord();
        updates.setStatus("RETURNED");

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(loanRecordRepository.save(existing)).thenReturn(existing);

        loanRecordService.updateLoanRecord(uuid, updates);

        assertEquals(5, eq.getStockCount()); // unchanged
        verify(equipmentRepository, never()).saveAll(anyList());
    }

    @Test
    void testPatchLoanRecord_RestoresStockOnReturn() throws ValidationException {
        UUID uuid = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();

        Equipment eq = makeEquipment(equipmentId, "Monitor", 8);
        LoanEquipmentItem item = new LoanEquipmentItem();
        item.setEquipment(eq);
        item.setQuantity(2);

        LoanRecord existing = new LoanRecord();
        existing.setId(uuid);
        existing.setStatus("PROCESSING");
        existing.getItems().add(item);
        item.setLoanRecord(existing);

        LoanRecord patch = new LoanRecord();
        patch.setStatus("RETURNED");

        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(loanRecordRepository.save(existing)).thenReturn(existing);
        when(equipmentRepository.saveAll(anyList())).thenReturn(List.of(eq));

        loanRecordService.patchLoanRecord(uuid, patch);

        assertEquals("RETURNED", existing.getStatus());
        assertEquals(10, eq.getStockCount()); // 8 + 2 restored
    }

    @Test
    void testUpdateLoanRecord_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(loanRecordRepository.findById(uuid)).thenReturn(Optional.empty());
        assertThrows(ValidationException.class,
                () -> loanRecordService.updateLoanRecord(uuid, new LoanRecord()));
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