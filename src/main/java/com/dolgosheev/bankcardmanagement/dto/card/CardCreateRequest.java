package com.dolgosheev.bankcardmanagement.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequest {
    
    @NotBlank(message = "Card number cannot be empty")
    @Pattern(regexp = "^\\d{16}$", message = "Card number must contain 16 digits")
    private String cardNumber;
    
    @NotBlank(message = "Cardholder name cannot be empty")
    private String cardholderName;
    
    @NotNull(message = "Card expiration date cannot be empty")
    private LocalDate expirationDate;
    
    @NotNull(message = "Initial balance cannot be empty")
    @Positive(message = "Initial balance must be a positive number")
    private BigDecimal initialBalance;
    
    @NotNull(message = "User ID cannot be empty")
    private Long userId;
} 