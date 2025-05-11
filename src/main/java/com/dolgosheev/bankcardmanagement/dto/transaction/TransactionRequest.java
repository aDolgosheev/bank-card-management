package com.dolgosheev.bankcardmanagement.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    @NotNull(message = "Source card ID cannot be empty")
    private Long sourceCardId;
    
    @NotNull(message = "Target card ID cannot be empty")
    private Long targetCardId;
    
    @NotNull(message = "Transaction amount cannot be empty")
    @Positive(message = "Transaction amount must be a positive number")
    private BigDecimal amount;
} 