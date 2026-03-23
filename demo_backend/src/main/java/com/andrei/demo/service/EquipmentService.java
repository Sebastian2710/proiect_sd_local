package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.repository.EquipmentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class EquipmentService {
    private final EquipmentRepository equipmentRepository;

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public Equipment getEquipmentById(UUID id) {
        return equipmentRepository.findById(id).orElseThrow(
                () -> new IllegalStateException("Equipment with id " + id + " not found")
        );
    }

    public Equipment addEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public Equipment updateEquipment(UUID id, Equipment equipmentUpdates) throws ValidationException {
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Equipment with id " + id + " not found"));

        existingEquipment.setName(equipmentUpdates.getName());
        existingEquipment.setDescription(equipmentUpdates.getDescription());
        existingEquipment.setStockCount(equipmentUpdates.getStockCount());

        return equipmentRepository.save(existingEquipment);
    }

    public Equipment patchEquipment(UUID id, Equipment equipmentUpdates) throws ValidationException {
        Equipment existingEquipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Equipment with id " + id + " not found"));

        if (equipmentUpdates.getName() != null) {
            existingEquipment.setName(equipmentUpdates.getName());
        }
        if (equipmentUpdates.getDescription() != null) {
            existingEquipment.setDescription(equipmentUpdates.getDescription());
        }
        if (equipmentUpdates.getStockCount() != null) {
            existingEquipment.setStockCount(equipmentUpdates.getStockCount());
        }

        return equipmentRepository.save(existingEquipment);
    }

    public void deleteEquipment(UUID id) {
        equipmentRepository.deleteById(id);
    }
}