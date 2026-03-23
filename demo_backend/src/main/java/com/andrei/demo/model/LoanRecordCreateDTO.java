package com.andrei.demo.model;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class LoanRecordCreateDTO {
    @NotNull(message = "Person ID is required")
    private UUID personId;

    @NotNull(message = "Equipment IDs are required")
    private List<UUID> equipmentIds;

    @NotNull(message = "Expected return date is required")
    @FutureOrPresent(message = "Expected return date cannot be in the past")
    private LocalDate expectedReturnDate;
}