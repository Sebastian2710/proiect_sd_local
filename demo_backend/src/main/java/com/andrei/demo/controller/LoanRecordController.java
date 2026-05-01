package com.andrei.demo.controller;

import com.andrei.demo.config.ValidationException;
import com.andrei.demo.model.LoanRecord;
import com.andrei.demo.model.LoanRecordCreateDTO;
import com.andrei.demo.model.StudentLoanRequestDTO;
import com.andrei.demo.service.LoanRecordService;
import com.andrei.demo.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/loan")
@AllArgsConstructor
@CrossOrigin
public class LoanRecordController {

    private final LoanRecordService loanRecordService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public List<LoanRecord> getAllLoanRecords() {
        return loanRecordService.getAllLoanRecords();
    }

    @GetMapping("/{id}")
    public LoanRecord getLoanRecordById(@PathVariable UUID id) {
        return loanRecordService.getLoanRecordById(id);
    }

    @PostMapping
    public LoanRecord addLoanRecord(@Valid @RequestBody LoanRecordCreateDTO dto) throws ValidationException {
        return loanRecordService.addLoanRecord(dto);
    }

    @PostMapping("/request")
    public LoanRecord requestLoan(
            @Valid @RequestBody StudentLoanRequestDTO dto,
            @RequestHeader("Authorization") String authHeader) throws ValidationException {
        String token = authHeader.substring(7);
        String email = jwtUtil.getEmailFromToken(token);
        return loanRecordService.requestLoan(dto, email);
    }

    @GetMapping("/my")
    public List<LoanRecord> getMyLoanRecords(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String email = jwtUtil.getEmailFromToken(token);
        return loanRecordService.getLoanRecordsByPersonEmail(email);
    }

    @PutMapping("/{id}")
    public LoanRecord updateLoanRecord(
            @PathVariable UUID id,
            @RequestBody LoanRecord recordUpdates) throws ValidationException {
        return loanRecordService.updateLoanRecord(id, recordUpdates);
    }

    @PatchMapping("/{id}")
    public LoanRecord patchLoanRecord(
            @PathVariable UUID id,
            @RequestBody LoanRecord recordUpdates) throws ValidationException {
        return loanRecordService.patchLoanRecord(id, recordUpdates);
    }

    @DeleteMapping("/{id}")
    public void deleteLoanRecord(@PathVariable UUID id) {
        loanRecordService.deleteLoanRecord(id);
    }
}