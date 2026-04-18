package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.PersonCreateDTO;
import com.andrei.demo.model.Role;
import com.andrei.demo.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

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
        person.setPassword(personDTO.getPassword());
        person.setRole(personDTO.getRole() != null ? personDTO.getRole() : Role.STUDENT);
        return personRepository.save(person);
    }

    public Person updatePerson(UUID uuid, Person person) throws ValidationException{
        Optional<Person> personOptional =
                personRepository.findById(uuid);

        if(personOptional.isEmpty()) {
            throw new ValidationException("Person with id " + uuid + " not found");
        }
        Person existingPerson = personOptional.get();

        existingPerson.setName(person.getName());
        existingPerson.setAge(person.getAge());
        if (person.getEmail() != null && !person.getEmail().equals(existingPerson.getEmail())) {
            if (personRepository.findByEmail(person.getEmail()).isPresent()) {
                throw new ValidationException("A user with the email " + person.getEmail() + " already exists.");
            }
            existingPerson.setEmail(person.getEmail());
        }
        existingPerson.setPassword(person.getPassword());
        if (person.getRole() != null) {
            existingPerson.setRole(person.getRole());
        }
        return personRepository.save(existingPerson);
    }

    public Person updatePerson2(UUID uuid, Person person) throws ValidationException{
        return personRepository
                        .findById(uuid)
                        .map(existingPerson -> {
                            existingPerson.setName(person.getName());
                            existingPerson.setAge(person.getAge());
                            existingPerson.setEmail(person.getEmail());
                            existingPerson.setPassword(person.getPassword());
                            existingPerson.setRole(person.getRole());
                            return personRepository.save(existingPerson);
                        })
                        .orElseThrow(
                                () -> new ValidationException("Person with id " + uuid + " not found")
                        );
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
        if (person.getPassword() != null) {
            existingPerson.setPassword(person.getPassword());
        }
        if (person.getRole() != null) {
            existingPerson.setRole(person.getRole());
        }
        return personRepository.save(existingPerson);
    }
}
