package com.dolgosheev.bankcardmanagement.dto.card;

import com.dolgosheev.bankcardmanagement.entity.Card;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusUpdateRequest {
    
    @NotNull(message = "Card status cannot be empty")
    private Card.CardStatus status;
} 