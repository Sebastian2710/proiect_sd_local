package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PersonServiceTests {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @InjectMocks
    private PersonService personService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    // --- getPeople ---

    @Test
    void testGetPeople() {
        List<Person> people = List.of(new Person(), new Person());
        when(personRepository.findAll()).thenReturn(people);

        List<Person> result = personService.getPeople();

        assertEquals(2, result.size());
        verify(personRepository, times(1)).findAll();
    }

    // --- addPerson ---

    @Test
    void testAddPerson_Success() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("John");
        dto.setPassword("Password1!");
        dto.setAge(25);
        dto.setEmail("john@example.com");

        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordUtil.hashPassword("Password1!")).thenReturn("hashed-password");
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = personService.addPerson(dto);

        assertEquals("John", result.getName());
        assertEquals(25, result.getAge());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("hashed-password", result.getPassword());
        assertEquals(Role.STUDENT, result.getRole());
        verify(passwordUtil, times(1)).hashPassword("Password1!");
        verify(personRepository, times(1)).save(any(Person.class));
    }

    @Test
    void testAddPerson_AsAdmin_SetsAdminRole() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Admin User");
        dto.setPassword("Password1!");
        dto.setAge(30);
        dto.setEmail("admin@example.com");
        dto.setRole(Role.ADMIN);

        when(personRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordUtil.hashPassword("Password1!")).thenReturn("hashed-password");
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = personService.addPerson(dto);

        assertEquals(Role.ADMIN, result.getRole());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testAddPerson_NoRoleProvided_DefaultsToStudent() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Student");
        dto.setPassword("Password1!");
        dto.setAge(20);
        dto.setEmail("student@example.com");

        when(personRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordUtil.hashPassword(any())).thenReturn("hashed");
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = personService.addPerson(dto);

        assertEquals(Role.STUDENT, result.getRole());
    }

    @Test
    void testAddPerson_EmailAlreadyExists() {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setEmail("john@example.com");

        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.addPerson(dto));
        verify(personRepository, never()).save(any());
        verifyNoInteractions(passwordUtil);
    }

    // --- updatePerson ---

    @Test
    void testUpdatePerson_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setName("John");
        existing.setEmail("john@example.com");
        existing.setAge(25);
        existing.setPassword("old-hash");
        existing.setRole(Role.STUDENT);

        Person update = new Person();
        update.setName("Jane");
        update.setEmail("john@example.com"); // same email, no conflict
        update.setAge(30);
        update.setPassword("ignored-should-not-be-updated");
        update.setRole(Role.ADMIN);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Person result = personService.updatePerson(uuid, update);

        assertEquals("Jane", result.getName());
        assertEquals(30, result.getAge());
        assertEquals(Role.ADMIN, result.getRole());
        assertEquals("old-hash", result.getPassword()); // password must NOT be changed
        verify(personRepository, times(1)).findById(uuid);
        verify(personRepository, times(1)).save(any(Person.class));
        verifyNoInteractions(passwordUtil);
    }

    @Test
    void testUpdatePerson_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> personService.updatePerson(uuid, new Person()));
        verify(personRepository, never()).save(any());
        verifyNoInteractions(passwordUtil);
    }

    @Test
    void testUpdatePerson_EmailConflict() {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setEmail("john@example.com");

        Person update = new Person();
        update.setEmail("taken@example.com");

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.updatePerson(uuid, update));
    }

    // --- deletePerson ---

    @Test
    void testDeletePerson() {
        UUID uuid = UUID.randomUUID();
        doNothing().when(personRepository).deleteById(uuid);

        personService.deletePerson(uuid);

        verify(personRepository, times(1)).deleteById(uuid);
    }

    // --- getPersonByEmail ---

    @Test
    void testGetPersonByEmail_Found() {
        Person person = new Person();
        person.setEmail("john@example.com");
        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.of(person));

        Person result = personService.getPersonByEmail("john@example.com");

        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void testGetPersonByEmail_NotFound() {
        when(personRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonByEmail("notfound@example.com"));
    }

    // --- getPersonById ---

    @Test
    void testGetPersonById_Found() {
        UUID uuid = UUID.randomUUID();
        Person person = new Person();
        person.setId(uuid);
        when(personRepository.findById(uuid)).thenReturn(Optional.of(person));

        Person result = personService.getPersonById(uuid);

        assertEquals(uuid, result.getId());
    }

    @Test
    void testGetPersonById_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> personService.getPersonById(uuid));
    }

    // --- patchPerson ---

    @Test
    void testPatchPerson_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setName("John");
        existing.setEmail("john@example.com");
        existing.setAge(25);
        existing.setPassword("stored-hash");

        Person patch = new Person();
        patch.setName("Johnny");
        patch.setAge(26);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personService.patchPerson(uuid, patch);

        assertEquals("Johnny", existing.getName());
        assertEquals(26, existing.getAge());
        assertEquals("john@example.com", existing.getEmail()); // unchanged
        assertEquals("stored-hash", existing.getPassword());   // password NOT patched
        verify(personRepository).save(existing);
        verifyNoInteractions(passwordUtil);
    }

    @Test
    void testPatchPerson_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> personService.patchPerson(uuid, new Person()));
    }

    @Test
    void testPatchPerson_EmailConflict() {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setEmail("john@example.com");

        Person patch = new Person();
        patch.setEmail("taken@example.com");

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.patchPerson(uuid, patch));
    }
}