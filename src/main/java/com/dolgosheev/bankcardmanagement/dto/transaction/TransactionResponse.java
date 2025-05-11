package com.dolgosheev.bankcardmanagement.dto.transaction;

import com.dolgosheev.bankcardmanagement.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TransactionResponse {
    private Long id;
    private String sourceMaskedCardNumber;
    private String targetMaskedCardNumber;
    private BigDecimal amount;
    private LocalDateTime transactionDate;
    private Transaction.TransactionStatus status;
    
    public static TransactionResponse fromTransaction(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSourceCard().getMaskedCardNumber(),
                transaction.getTargetCard().getMaskedCardNumber(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                transaction.getStatus()
        );
    }
} 