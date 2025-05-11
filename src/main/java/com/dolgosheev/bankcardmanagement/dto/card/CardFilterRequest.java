package com.dolgosheev.bankcardmanagement.dto.card;

import com.dolgosheev.bankcardmanagement.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardFilterRequest {
    private Card.CardStatus status;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private String cardholderName;
    private Long userId;
} 