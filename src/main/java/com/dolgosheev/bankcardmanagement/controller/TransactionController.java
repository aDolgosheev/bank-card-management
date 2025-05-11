package com.dolgosheev.bankcardmanagement.controller;

import com.dolgosheev.bankcardmanagement.dto.transaction.TransactionRequest;
import com.dolgosheev.bankcardmanagement.dto.transaction.TransactionResponse;
import com.dolgosheev.bankcardmanagement.entity.Transaction;
import com.dolgosheev.bankcardmanagement.service.TransactionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "API for managing transactions between cards")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {
    
    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new transaction between user's cards")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest transactionRequest,
            Authentication authentication) {
        Transaction transaction = transactionService.createTransaction(transactionRequest, authentication.getName());
        return ResponseEntity.ok(TransactionResponse.fromTransaction(transaction));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions", description = "Get paginated list of all transactions (admin only)")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(Pageable pageable) {
        Page<Transaction> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(transactions.map(TransactionResponse::fromTransaction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @transactionService.validateTransactionAccess(authentication.name, #id)")
    @Operation(summary = "Get transaction by ID", description = "Get transaction information by its ID")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id, Authentication authentication) {
        Transaction transaction = transactionService.getTransactionById(id);
        return ResponseEntity.ok(TransactionResponse.fromTransaction(transaction));
    }

    @GetMapping("/card/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @cardService.validateCardAccess(authentication.name, #cardId)")
    @Operation(summary = "Get card transactions", description = "Get paginated list of transactions for a specific card")
    public ResponseEntity<Page<TransactionResponse>> getCardTransactions(
            @PathVariable Long cardId, 
            Pageable pageable, 
            Authentication authentication) {
        Page<Transaction> transactions = transactionService.getCardTransactions(cardId, pageable);
        return ResponseEntity.ok(transactions.map(TransactionResponse::fromTransaction));
    }

    @GetMapping("/user/{userId}/card/{cardId}")
    @PreAuthorize("hasRole('ADMIN') or @userService.validateUserAccess(authentication.name, #userId)")
    @Operation(summary = "Get user card transactions", description = "Get paginated list of user's transactions for a specific card")
    public ResponseEntity<Page<TransactionResponse>> getUserCardTransactions(
            @PathVariable Long userId,
            @PathVariable Long cardId,
            Pageable pageable,
            Authentication authentication) {
        Page<Transaction> transactions = transactionService.getUserCardTransactions(userId, cardId, pageable);
        return ResponseEntity.ok(transactions.map(TransactionResponse::fromTransaction));
    }
} 