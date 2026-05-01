package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.*;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class LoanRecordService {

    private final LoanRecordRepository loanRecordRepository;
    private final PersonRepository personRepository;
    private final EquipmentRepository equipmentRepository;

    public List<LoanRecord> getAllLoanRecords() {
        return loanRecordRepository.findAll();
    }

    public LoanRecord getLoanRecordById(UUID id) {
        return loanRecordRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Loan Record with id " + id + " not found")
        );
    }

    public List<LoanRecord> getLoanRecordsByPersonEmail(String email) {
        return loanRecordRepository.findByPerson_Email(email);
    }

    public LoanRecord addLoanRecord(LoanRecordCreateDTO dto) throws ValidationException {
        Person person = personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new ValidationException(
                        "Person with id " + dto.getPersonId() + " not found"));

        LoanRecord loanRecord = new LoanRecord();
        loanRecord.setPerson(person);
        loanRecord.setLoanDate(LocalDate.now());
        loanRecord.setExpectedReturnDate(dto.getExpectedReturnDate());
        loanRecord.setStatus("ACTIVE");

        List<LoanEquipmentItem> items = buildAndDeductItems(dto.getEquipmentQuantities(), loanRecord);
        loanRecord.setItems(items);

        return loanRecordRepository.save(loanRecord);
    }

    public LoanRecord requestLoan(StudentLoanRequestDTO dto, String personEmail) throws ValidationException {
        Person person = personRepository.findByEmail(personEmail)
                .orElseThrow(() -> new ValidationException(
                        "Person with email " + personEmail + " not found"));

        LoanRecord loanRecord = new LoanRecord();
        loanRecord.setPerson(person);
        loanRecord.setLoanDate(LocalDate.now());
        loanRecord.setExpectedReturnDate(dto.getExpectedReturnDate());
        loanRecord.setStatus("PROCESSING");

        List<LoanEquipmentItem> items = buildAndDeductItems(dto.getEquipmentQuantities(), loanRecord);
        loanRecord.setItems(items);

        return loanRecordRepository.save(loanRecord);
    }

    private List<LoanEquipmentItem> buildAndDeductItems(
            List<EquipmentQuantityDTO> quantityDTOs, LoanRecord loanRecord) throws ValidationException {

        List<LoanEquipmentItem> items = new ArrayList<>();
        List<Equipment> toSave = new ArrayList<>();

        for (EquipmentQuantityDTO eqDTO : quantityDTOs) {
            Equipment equipment = equipmentRepository.findById(eqDTO.getEquipmentId())
                    .orElseThrow(() -> new ValidationException(
                            "Equipment with id " + eqDTO.getEquipmentId() + " not found"));

            if (equipment.getStockCount() < eqDTO.getQuantity()) {
                throw new ValidationException(
                        "Equipment '" + equipment.getName() + "' does not have enough stock " +
                                "(available: " + equipment.getStockCount() +
                                ", requested: " + eqDTO.getQuantity() + ").");
            }

            equipment.setStockCount(equipment.getStockCount() - eqDTO.getQuantity());
            toSave.add(equipment);

            LoanEquipmentItem item = new LoanEquipmentItem();
            item.setEquipment(equipment);
            item.setQuantity(eqDTO.getQuantity());
            item.setLoanRecord(loanRecord);
            items.add(item);
        }

        equipmentRepository.saveAll(toSave);
        return items;
    }

    private void restoreStock(LoanRecord record) {
        List<Equipment> toSave = new ArrayList<>();
        for (LoanEquipmentItem item : record.getItems()) {
            Equipment eq = item.getEquipment();
            eq.setStockCount(eq.getStockCount() + item.getQuantity());
            toSave.add(eq);
        }
        equipmentRepository.saveAll(toSave);
    }

    public LoanRecord updateLoanRecord(UUID id, LoanRecord recordUpdates) throws ValidationException {
        LoanRecord existingRecord = loanRecordRepository.findById(id)
                .orElseThrow(() -> new ValidationException(
                        "Loan Record with id " + id + " not found"));

        boolean wasReturned = "RETURNED".equals(existingRecord.getStatus());

        if (recordUpdates.getExpectedReturnDate() != null) {
            existingRecord.setExpectedReturnDate(recordUpdates.getExpectedReturnDate());
        }
        if (recordUpdates.getActualReturnDate() != null) {
            existingRecord.setActualReturnDate(recordUpdates.getActualReturnDate());
        }
        if (recordUpdates.getStatus() != null) {
            if (!wasReturned && "RETURNED".equals(recordUpdates.getStatus())) {
                restoreStock(existingRecord);
            }
            existingRecord.setStatus(recordUpdates.getStatus());
        }

        return loanRecordRepository.save(existingRecord);
    }

    public LoanRecord patchLoanRecord(UUID id, LoanRecord recordUpdates) throws ValidationException {
        LoanRecord existingRecord = loanRecordRepository.findById(id)
                .orElseThrow(() -> new ValidationException(
                        "Loan Record with id " + id + " not found"));

        boolean wasReturned = "RETURNED".equals(existingRecord.getStatus());

        if (recordUpdates.getExpectedReturnDate() != null) {
            existingRecord.setExpectedReturnDate(recordUpdates.getExpectedReturnDate());
        }
        if (recordUpdates.getActualReturnDate() != null) {
            existingRecord.setActualReturnDate(recordUpdates.getActualReturnDate());
        }
        if (recordUpdates.getStatus() != null) {
            if (!wasReturned && "RETURNED".equals(recordUpdates.getStatus())) {
                restoreStock(existingRecord);
            }
            existingRecord.setStatus(recordUpdates.getStatus());
        }

        return loanRecordRepository.save(existingRecord);
    }

    public void deleteLoanRecord(UUID id) {
        loanRecordRepository.deleteById(id);
    }
}