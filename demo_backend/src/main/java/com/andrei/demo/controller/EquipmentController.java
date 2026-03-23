package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.service.EquipmentService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/equipment")
@AllArgsConstructor
@CrossOrigin
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public List<Equipment> getAllEquipment() {
        return equipmentService.getAllEquipment();
    }

    @GetMapping("/{id}")
    public Equipment getEquipmentById(@PathVariable UUID id) {
        return equipmentService.getEquipmentById(id);
    }

    @PostMapping
    public Equipment addEquipment(@Valid @RequestBody Equipment equipment) {
        return equipmentService.addEquipment(equipment);
    }

    @PutMapping("/{id}")
    public Equipment updateEquipment(
            @PathVariable UUID id,
            @Valid @RequestBody Equipment equipment) throws ValidationException {
        return equipmentService.updateEquipment(id, equipment);
    }

    @PatchMapping("/{id}")
    public Equipment patchEquipment(
            @PathVariable UUID id,
            @Valid @RequestBody Equipment equipment) throws ValidationException {
        return equipmentService.patchEquipment(id, equipment);
    }

    @DeleteMapping("/{id}")
    public void deleteEquipment(@PathVariable UUID id) {
        equipmentService.deleteEquipment(id);
    }
}