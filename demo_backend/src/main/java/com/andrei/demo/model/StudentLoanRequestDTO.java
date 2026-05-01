package com.andrei.demo.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StudentLoanRequestDTO {

    @NotNull(message = "Equipment quantities are required")
    @Size(min = 1, message = "At least one equipment item is required")
    @Valid
    private List<EquipmentQuantityDTO> equipmentQuantities;

    @NotNull(message = "Expected return date is required")
    @FutureOrPresent(message = "Expected return date cannot be in the past")
    private LocalDate expectedReturnDate;
}