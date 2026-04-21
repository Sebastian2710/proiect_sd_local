package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.PersonRepository;
import com.andrei.demo.util.PasswordUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PasswordUtil passwordUtil;

    public List<Person> getPeople() {
        return personRepository.findAll();
    }

    public Person addPerson(PersonCreateDTO personDTO) throws ValidationException {
        if (personRepository.findByEmail(personDTO.getEmail()).isPresent()) {
            throw new ValidationException("A user with the email " + personDTO.getEmail() + " already exists.");
        }

        Person person = new Person();
        person.setName(personDTO.getName());
        person.setAge(personDTO.getAge());
        person.setEmail(personDTO.getEmail());
        person.setPassword(passwordUtil.hashPassword(personDTO.getPassword()));
        person.setRole(personDTO.getRole() != null ? personDTO.getRole() : Role.STUDENT);

        return personRepository.save(person);
    }

    public Person updatePerson(UUID uuid, Person person) throws ValidationException {
        Person existingPerson = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        existingPerson.setName(person.getName());
        existingPerson.setAge(person.getAge());

        if (person.getEmail() != null && !person.getEmail().equals(existingPerson.getEmail())) {
            if (personRepository.findByEmail(person.getEmail()).isPresent()) {
                throw new ValidationException("A user with the email " + person.getEmail() + " already exists.");
            }
            existingPerson.setEmail(person.getEmail());
        }

        // Password is intentionally not updated here; use the password reset flow instead.
        if (person.getRole() != null) {
            existingPerson.setRole(person.getRole());
        }

        return personRepository.save(existingPerson);
    }

    public void deletePerson(UUID uuid) {
        personRepository.deleteById(uuid);
    }

    public Person getPersonByEmail(String email) {
        return personRepository.findByEmail(email).orElseThrow(
                () -> new IllegalStateException("Person with email " + email + " not found"));
    }

    public Person getPersonById(UUID uuid) {
        return personRepository.findById(uuid).orElseThrow(
                () -> new IllegalStateException("Person with id " + uuid + " not found"));
    }

    public Person patchPerson(UUID uuid, Person person) throws ValidationException {
        Person existingPerson = personRepository.findById(uuid)
                .orElseThrow(() -> new ValidationException("Person with id " + uuid + " not found"));

        if (person.getEmail() != null && !person.getEmail().equals(existingPerson.getEmail())) {
            if (personRepository.findByEmail(person.getEmail()).isPresent()) {
                throw new ValidationException("A user with the email " + person.getEmail() + " already exists.");
            }
            existingPerson.setEmail(person.getEmail());
        }
        if (person.getName() != null) {
            existingPerson.setName(person.getName());
        }
        if (person.getAge() != null) {
            existingPerson.setAge(person.getAge());
        }
        if (person.getRole() != null) {
            existingPerson.setRole(person.getRole());
        }

        // Password is intentionally not patched here; use the password reset flow instead.

        return personRepository.save(existingPerson);
    }
}