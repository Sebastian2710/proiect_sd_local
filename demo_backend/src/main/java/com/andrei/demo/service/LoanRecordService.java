package com.andrei.demo.service;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.Equipment;
import com.andrei.demo.model.LoanRecord;
import com.andrei.demo.model.LoanRecordCreateDTO;
import com.andrei.demo.model.Person;
import com.andrei.demo.repository.EquipmentRepository;
import com.andrei.demo.repository.LoanRecordRepository;
import com.andrei.demo.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
                () -> new IllegalStateException("Loan Record with id " + id + " not found")
        );
    }

    public LoanRecord addLoanRecord(LoanRecordCreateDTO dto) throws ValidationException {
        Person person = personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new ValidationException("Person with id " + dto.getPersonId() + " not found"));

        List<Equipment> equipmentList = equipmentRepository.findAllById(dto.getEquipmentIds());
        if (equipmentList.size() != dto.getEquipmentIds().size()) {
            throw new ValidationException("One or more equipment IDs are invalid.");
        }

        for (Equipment eq : equipmentList) {
            if (eq.getStockCount() <= 0) {
                throw new ValidationException("Equipment '" + eq.getName() + "' is out of stock and cannot be borrowed.");
            }
            eq.setStockCount(eq.getStockCount() - 1);
        }
        equipmentRepository.saveAll(equipmentList);

        LoanRecord loanRecord = new LoanRecord();
        loanRecord.setPerson(person);
        loanRecord.setEquipmentList(equipmentList);
        loanRecord.setLoanDate(LocalDate.now());
        loanRecord.setExpectedReturnDate(dto.getExpectedReturnDate());
        loanRecord.setStatus("ACTIVE");

        return loanRecordRepository.save(loanRecord);
    }

    public LoanRecord updateLoanRecord(UUID id, LoanRecord recordUpdates) throws ValidationException {
        LoanRecord existingRecord = loanRecordRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Loan Record with id " + id + " not found"));

        existingRecord.setExpectedReturnDate(recordUpdates.getExpectedReturnDate());
        existingRecord.setActualReturnDate(recordUpdates.getActualReturnDate());
        existingRecord.setStatus(recordUpdates.getStatus());

        return loanRecordRepository.save(existingRecord);
    }

    public LoanRecord patchLoanRecord(UUID id, LoanRecord recordUpdates) throws ValidationException {
        LoanRecord existingRecord = loanRecordRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Loan Record with id " + id + " not found"));

        if (recordUpdates.getExpectedReturnDate() != null) {
            existingRecord.setExpectedReturnDate(recordUpdates.getExpectedReturnDate());
        }
        if (recordUpdates.getActualReturnDate() != null) {
            existingRecord.setActualReturnDate(recordUpdates.getActualReturnDate());
        }
        if (recordUpdates.getStatus() != null) {
            existingRecord.setStatus(recordUpdates.getStatus());
        }

        return loanRecordRepository.save(existingRecord);
    }

    public void deleteLoanRecord(UUID id) {
        loanRecordRepository.deleteById(id);
    }
}