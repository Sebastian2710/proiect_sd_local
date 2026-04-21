package com.andrei.demo.service;

import com.andrei.demo.model.LoginResponse;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.JwtUtil;
import com.andrei.demo.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityServiceTests {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private SecurityService securityService;

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
    void testLoginSuccess_AdminRole() {
        String email = "admin@example.com";
        String password = "password";
        String token = "jwt-token-123";

        Person person = new Person();
        person.setEmail(email);
        person.setPassword("hashed-password");
        person.setRole(Role.ADMIN);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(password, "hashed-password")).thenReturn(true);
        when(jwtUtil.createToken(person)).thenReturn(token);

        LoginResponse result = securityService.login(email, password);

        assertTrue(result.success());
        assertEquals("ADMIN", result.role());
        assertEquals(token, result.token());
        assertNull(result.errorMessage());
        verify(personRepository, times(1)).findByEmail(email);
        verify(passwordUtil, times(1)).checkPassword(password, "hashed-password");
        verify(jwtUtil, times(1)).createToken(person);
    }

    @Test
    void testLoginSuccess_StudentRole() {
        String email = "student@example.com";
        String password = "password";
        String token = "jwt-token-456";

        Person person = new Person();
        person.setEmail(email);
        person.setPassword("hashed-password");
        person.setRole(Role.STUDENT);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(password, "hashed-password")).thenReturn(true);
        when(jwtUtil.createToken(person)).thenReturn(token);

        LoginResponse result = securityService.login(email, password);

        assertTrue(result.success());
        assertEquals("STUDENT", result.role());
        assertEquals(token, result.token());
    }

    @Test
    void testLoginIncorrectPassword() {
        String email = "john@example.com";
        String password = "wrong-password";

        Person person = new Person();
        person.setEmail(email);
        person.setPassword("stored-hash");
        person.setRole(Role.STUDENT);

        when(personRepository.findByEmail(email)).thenReturn(Optional.of(person));
        when(passwordUtil.checkPassword(password, "stored-hash")).thenReturn(false);

        LoginResponse result = securityService.login(email, password);

        assertFalse(result.success());
        assertNull(result.role());
        assertNull(result.token());
        assertEquals("Incorrect password", result.errorMessage());
        verify(personRepository, times(1)).findByEmail(email);
        verify(passwordUtil, times(1)).checkPassword(password, "stored-hash");
        verify(jwtUtil, never()).createToken(any(Person.class));
    }

    @Test
    void testLoginEmailNotFound() {
        String email = "notfound@example.com";
        when(personRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginResponse result = securityService.login(email, "anypassword");

        assertFalse(result.success());
        assertNull(result.role());
        assertNull(result.token());
        assertEquals("Person with email " + email + " not found", result.errorMessage());
        verify(personRepository, times(1)).findByEmail(email);
        verifyNoInteractions(passwordUtil, jwtUtil);
    }
}