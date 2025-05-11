package com.dolgosheev.bankcardmanagement.dto.card;

import com.dolgosheev.bankcardmanagement.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class CardResponse {
    private Long id;
    private String maskedCardNumber;
    private String cardholderName;
    private LocalDate expirationDate;
    private Card.CardStatus status;
    private BigDecimal balance;
    private Long userId;
    private String userEmail;
    
    // Static method to convert card to DTO
    public static CardResponse fromCard(Card card, String userEmail) {
        return new CardResponse(
                card.getId(),
                card.getMaskedCardNumber(),
                card.getCardholderName(),
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId(),
                userEmail
        );
    }
} 