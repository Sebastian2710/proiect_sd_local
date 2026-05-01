package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Person;
import com.andrei.demo.model.RegisterPersonDto;
import com.andrei.demo.service.PersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@CrossOrigin
public class RegisterController {

    private final PersonService personService;

    @PostMapping("/register")
    public Person register(@Valid @RequestBody RegisterPersonDto dto) throws ValidationException {
        return personService.registerStudent(dto);
    }
}