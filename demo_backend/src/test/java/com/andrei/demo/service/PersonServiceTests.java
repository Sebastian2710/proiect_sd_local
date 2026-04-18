package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.PersonRepository;
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

class PersonServiceTests {

    @Mock
    private PersonRepository personRepository;

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

        Person saved = new Person();
        saved.setId(UUID.randomUUID());
        saved.setName("John");
        saved.setPassword("Password1!");
        saved.setAge(25);
        saved.setEmail("john@example.com");
        saved.setRole(Role.STUDENT);

        when(personRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertNotNull(result.getId());
        assertEquals(Role.STUDENT, result.getRole());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testAddPerson_EmailAlreadyExists() {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setEmail("john@example.com");

        when(personRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class, () -> personService.addPerson(dto));
        verify(personRepository, never()).save(any());
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
        existing.setPassword("oldpass");
        existing.setRole(Role.STUDENT);

        Person update = new Person();
        update.setName("Jane");
        update.setEmail("john@example.com"); // same email, no conflict check
        update.setAge(30);
        update.setPassword("newpass");
        update.setRole(Role.ADMIN);

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenReturn(existing);

        Person result = personService.updatePerson(uuid, update);

        assertEquals("Jane", existing.getName());
        assertEquals(30, existing.getAge());
        assertEquals(Role.ADMIN, existing.getRole());
        verify(personRepository).save(existing);
    }

    @Test
    void testUpdatePerson_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> personService.updatePerson(uuid, new Person()));
        verify(personRepository, never()).save(any());
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
        when(personRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class,
                () -> personService.updatePerson(uuid, update));
    }

    // --- updatePerson2 ---

    @Test
    void testUpdatePerson2_Success() throws ValidationException {
        UUID uuid = UUID.randomUUID();

        Person existing = new Person();
        existing.setId(uuid);
        existing.setName("John");

        Person update = new Person();
        update.setName("Jane");
        update.setEmail("jane@example.com");
        update.setAge(22);
        update.setPassword("pass");

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenReturn(existing);

        personService.updatePerson2(uuid, update);

        verify(personRepository).save(existing);
    }

    @Test
    void testUpdatePerson2_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> personService.updatePerson2(uuid, new Person()));
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

        when(personRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(person));

        Person result = personService.getPersonByEmail("john@example.com");

        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void testGetPersonByEmail_NotFound() {
        when(personRepository.findByEmail("notfound@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> personService.getPersonByEmail("notfound@example.com"));
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

        assertThrows(IllegalStateException.class,
                () -> personService.getPersonById(uuid));
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

        Person patch = new Person();
        patch.setName("Johnny");
        patch.setAge(26);
        // email and password not set — should remain unchanged

        when(personRepository.findById(uuid)).thenReturn(Optional.of(existing));
        when(personRepository.save(any(Person.class))).thenReturn(existing);

        personService.patchPerson(uuid, patch);

        assertEquals("Johnny", existing.getName());
        assertEquals(26, existing.getAge());
        assertEquals("john@example.com", existing.getEmail()); // unchanged
        verify(personRepository).save(existing);
    }

    @Test
    void testPatchPerson_NotFound() {
        UUID uuid = UUID.randomUUID();
        when(personRepository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class,
                () -> personService.patchPerson(uuid, new Person()));
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
        when(personRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(new Person()));

        assertThrows(ValidationException.class,
                () -> personService.patchPerson(uuid, patch));
    }
    @Test
    void testAddPerson_AsAdmin_SetsAdminRole() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Admin User");
        dto.setPassword("Password1!");
        dto.setAge(30);
        dto.setEmail("admin@example.com");
        dto.setRole(Role.ADMIN); // explicitly set

        Person saved = new Person();
        saved.setId(UUID.randomUUID());
        saved.setName("Admin User");
        saved.setPassword("Password1!");
        saved.setAge(30);
        saved.setEmail("admin@example.com");
        saved.setRole(Role.ADMIN);

        when(personRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertEquals(Role.ADMIN, result.getRole());
        verify(personRepository).save(any(Person.class));
    }

    @Test
    void testAddPerson_NoRoleProvided_DefaultsToStudent() throws ValidationException {
        PersonCreateDTO dto = new PersonCreateDTO();
        dto.setName("Student User");
        dto.setPassword("Password1!");
        dto.setAge(20);
        dto.setEmail("student@example.com");
        // role intentionally not set

        Person saved = new Person();
        saved.setId(UUID.randomUUID());
        saved.setRole(Role.STUDENT);

        when(personRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(personRepository.save(any(Person.class))).thenReturn(saved);

        Person result = personService.addPerson(dto);

        assertEquals(Role.STUDENT, result.getRole());
    }
}