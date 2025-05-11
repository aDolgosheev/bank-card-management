package com.dolgosheev.bankcardmanagement.service;

import com.dolgosheev.bankcardmanagement.dto.transaction.TransactionRequest;
import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.Transaction;
import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final CardService cardService;
    private final UserService userService;

    @Transactional
    public Transaction createTransaction(TransactionRequest transactionRequest, String userEmail) {
        User currentUser = userService.getCurrentUser(userEmail);
        Card sourceCard = cardService.getCardById(transactionRequest.getSourceCardId());
        Card targetCard = cardService.getCardById(transactionRequest.getTargetCardId());
        
        // Check if user has access to both cards
        if (!sourceCard.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No access to source card with id " + sourceCard.getId());
        }
        
        if (!targetCard.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("No access to target card with id " + targetCard.getId());
        }
        
        // Check if cards are active
        if (sourceCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active");
        }
        
        if (targetCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new IllegalStateException("Target card is not active");
        }
        
        // Check if source card has sufficient funds
        BigDecimal amount = transactionRequest.getAmount();
        if (sourceCard.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds on source card");
        }
        
        // Create transaction
        Transaction transaction = new Transaction();
        transaction.setSourceCard(sourceCard);
        transaction.setTargetCard(targetCard);
        transaction.setAmount(amount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        try {
            // Deduct funds from source card
            sourceCard.setBalance(sourceCard.getBalance().subtract(amount));
            
            // Add funds to target card
            targetCard.setBalance(targetCard.getBalance().add(amount));
            
            // Update transaction status
            savedTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        } catch (Exception e) {
            // Mark transaction as failed in case of error
            savedTransaction.setStatus(Transaction.TransactionStatus.FAILED);
            throw new RuntimeException("Error executing transaction", e);
        }
        
        return transactionRepository.save(savedTransaction);
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Transaction with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getCardTransactions(Long cardId, Pageable pageable) {
        Card card = cardService.getCardById(cardId);
        return transactionRepository.findBySourceCardOrTargetCard(card, card, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getUserCardTransactions(Long userId, Long cardId, Pageable pageable) {
        Card card = cardService.getUserCardById(userId, cardId);
        return transactionRepository.findBySourceCardOrTargetCard(card, card, pageable);
    }

    @Transactional(readOnly = true)
    public void validateTransactionAccess(String userEmail, Long transactionId) {
        User currentUser = userService.getCurrentUser(userEmail);
        Transaction transaction = getTransactionById(transactionId);
        
        Card sourceCard = transaction.getSourceCard();
        Card targetCard = transaction.getTargetCard();
        
        // User has access to transaction if they own one of the cards or are admin
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == com.dolgosheev.bankcardmanagement.entity.Role.ERole.ROLE_ADMIN);
        boolean isSourceCardOwner = sourceCard.getUser().getId().equals(currentUser.getId());
        boolean isTargetCardOwner = targetCard.getUser().getId().equals(currentUser.getId());
        
        if (!isAdmin && !isSourceCardOwner && !isTargetCardOwner) {
            throw new AccessDeniedException("No access to transaction with id " + transactionId);
        }
    }
} 