package com.dolgosheev.bankcardmanagement.controller;

import com.dolgosheev.bankcardmanagement.dto.card.CardCreateRequest;
import com.dolgosheev.bankcardmanagement.dto.card.CardFilterRequest;
import com.dolgosheev.bankcardmanagement.dto.card.CardResponse;
import com.dolgosheev.bankcardmanagement.dto.card.CardStatusUpdateRequest;
import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.service.CardService;
import com.dolgosheev.bankcardmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Bank Cards", description = "API for managing bank cards")
@SecurityRequirement(name = "bearerAuth")
public class CardController {
    
    private final CardService cardService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create new card", description = "Create a new bank card (admin only)")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest cardCreateRequest) {
        Card card = cardService.createCard(cardCreateRequest);
        return ResponseEntity.ok(CardResponse.fromCard(card, userService.getUserById(card.getUser().getId()).getEmail()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all cards", description = "Get paginated list of all cards (admin only)")
    public ResponseEntity<Page<CardResponse>> getAllCards(Pageable pageable, Authentication authentication) {
        Page<Card> cards = cardService.getAllCards(pageable);
        return ResponseEntity.ok(cards.map(card -> 
            CardResponse.fromCard(card, userService.getUserById(card.getUser().getId()).getEmail())
        ));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Filter cards", description = "Filter and paginate cards by various parameters (admin only)")
    public ResponseEntity<Page<CardResponse>> filterCards(
            @Valid CardFilterRequest filterRequest, 
            Pageable pageable) {
        Page<Card> cards = cardService.filterCards(filterRequest, pageable);
        return ResponseEntity.ok(cards.map(card -> 
            CardResponse.fromCard(card, userService.getUserById(card.getUser().getId()).getEmail())
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardService.validateCardAccess(authentication.name, #id)")
    @Operation(summary = "Get card by ID", description = "Get card information by its ID")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id, Authentication authentication) {
        Card card = cardService.getCardById(id);
        return ResponseEntity.ok(CardResponse.fromCard(card, userService.getUserById(card.getUser().getId()).getEmail()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update card status", description = "Update bank card status (admin only)")
    public ResponseEntity<CardResponse> updateCardStatus(
            @PathVariable Long id, 
            @Valid @RequestBody CardStatusUpdateRequest statusUpdateRequest) {
        Card card = cardService.updateCardStatus(id, statusUpdateRequest.getStatus());
        return ResponseEntity.ok(CardResponse.fromCard(card, userService.getUserById(card.getUser().getId()).getEmail()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete card", description = "Delete bank card by its ID (admin only)")
    public ResponseEntity<?> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.ok().body("Card successfully deleted");
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.validateUserAccess(authentication.name, #userId)")
    @Operation(summary = "Get user cards", description = "Get paginated list of user's cards")
    public ResponseEntity<Page<CardResponse>> getUserCards(
            @PathVariable Long userId, 
            Pageable pageable,
            Authentication authentication) {
        Page<Card> cards = cardService.getUserCards(userId, pageable);
        String userEmail = userService.getUserById(userId).getEmail();
        return ResponseEntity.ok(cards.map(card -> CardResponse.fromCard(card, userEmail)));
    }

    @PostMapping("/user/{userId}/card/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN') or @userService.validateUserAccess(authentication.name, #userId)")
    @Operation(summary = "Request card blocking", description = "Request to block a user's card")
    public ResponseEntity<?> requestCardBlock(
            @PathVariable Long userId, 
            @PathVariable Long cardId,
            Authentication authentication) {
        cardService.requestCardBlock(userId, cardId);
        return ResponseEntity.ok().body("Card block request successfully completed");
    }

    @GetMapping("/user/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or @userService.validateUserAccess(authentication.name, #userId)")
    @Operation(summary = "Get all user cards", description = "Get complete list of user's cards without pagination")
    public ResponseEntity<List<CardResponse>> getAllUserCards(
            @PathVariable Long userId,
            Authentication authentication) {
        List<Card> cards = cardService.getUserCards(userId);
        String userEmail = userService.getUserById(userId).getEmail();
        return ResponseEntity.ok(cards.stream()
                .map(card -> CardResponse.fromCard(card, userEmail))
                .collect(Collectors.toList()));
    }
} 