package com.dolgosheev.bankcardmanagement.service;

import com.dolgosheev.bankcardmanagement.dto.transaction.TransactionRequest;
import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.Role;
import com.dolgosheev.bankcardmanagement.entity.Transaction;
import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CardService cardService;

    @Mock
    private UserService userService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Card sourceCard;
    private Card targetCard;
    private Transaction testTransaction;
    private TransactionRequest transactionRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("password");
        testUser.setRoles(new HashSet<>());

        // Create test cards
        sourceCard = new Card();
        sourceCard.setId(1L);
        sourceCard.setCardNumberEncrypted("encryptedSourceCardNumber");
        sourceCard.setCardholderName("Test User");
        sourceCard.setExpirationDate(LocalDate.now().plusYears(2));
        sourceCard.setStatus(Card.CardStatus.ACTIVE);
        sourceCard.setBalance(new BigDecimal("1000.00"));
        sourceCard.setUser(testUser);

        targetCard = new Card();
        targetCard.setId(2L);
        targetCard.setCardNumberEncrypted("encryptedTargetCardNumber");
        targetCard.setCardholderName("Test User");
        targetCard.setExpirationDate(LocalDate.now().plusYears(2));
        targetCard.setStatus(Card.CardStatus.ACTIVE);
        targetCard.setBalance(new BigDecimal("500.00"));
        targetCard.setUser(testUser);

        // Create test transaction
        testTransaction = new Transaction();
        testTransaction.setId(1L);
        testTransaction.setSourceCard(sourceCard);
        testTransaction.setTargetCard(targetCard);
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setTransactionDate(LocalDateTime.now());
        testTransaction.setStatus(Transaction.TransactionStatus.COMPLETED);

        // Create transaction request
        transactionRequest = new TransactionRequest();
        transactionRequest.setSourceCardId(1L);
        transactionRequest.setTargetCardId(2L);
        transactionRequest.setAmount(new BigDecimal("100.00"));
    }

    @Test
    void createTransaction_Success() {
        // Arrange
        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardService.getCardById(1L)).thenReturn(sourceCard);
        when(cardService.getCardById(2L)).thenReturn(targetCard);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // Act
        Transaction result = transactionService.createTransaction(transactionRequest, "test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());
        assertEquals(testTransaction.getAmount(), result.getAmount());
        assertEquals(Transaction.TransactionStatus.COMPLETED, result.getStatus());

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(cardService).getCardById(eq(1L));
        verify(cardService).getCardById(eq(2L));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void createTransaction_InsufficientFunds() {
        // Arrange
        BigDecimal largeAmount = new BigDecimal("2000.00"); // More than available on source card
        transactionRequest.setAmount(largeAmount);

        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardService.getCardById(1L)).thenReturn(sourceCard);
        when(cardService.getCardById(2L)).thenReturn(targetCard);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            transactionService.createTransaction(transactionRequest, "test@example.com")
        );

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(cardService).getCardById(eq(1L));
        verify(cardService).getCardById(eq(2L));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_CardInactive() {
        // Arrange
        sourceCard.setStatus(Card.CardStatus.BLOCKED); // Source card is blocked

        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardService.getCardById(1L)).thenReturn(sourceCard);
        when(cardService.getCardById(2L)).thenReturn(targetCard);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            transactionService.createTransaction(transactionRequest, "test@example.com")
        );

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(cardService).getCardById(eq(1L));
        verify(cardService).getCardById(eq(2L));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_TargetCardInactive() {
        // Arrange
        targetCard.setStatus(Card.CardStatus.BLOCKED); // Target card is blocked

        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardService.getCardById(1L)).thenReturn(sourceCard);
        when(cardService.getCardById(2L)).thenReturn(targetCard);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            transactionService.createTransaction(transactionRequest, "test@example.com")
        );

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(cardService).getCardById(eq(1L));
        verify(cardService).getCardById(eq(2L));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransaction_NotCardOwner() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        sourceCard.setUser(otherUser); // Card belongs to another user

        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardService.getCardById(1L)).thenReturn(sourceCard);
        when(cardService.getCardById(2L)).thenReturn(targetCard);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> 
            transactionService.createTransaction(transactionRequest, "test@example.com")
        );

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(cardService).getCardById(eq(1L));
        verify(cardService).getCardById(eq(2L));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionById_Success() {
        // Arrange
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(testTransaction));

        // Act
        Transaction result = transactionService.getTransactionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testTransaction.getId(), result.getId());

        // Verify
        verify(transactionRepository).findById(eq(1L));
    }

    @Test
    void validateTransactionAccess_Admin() {
        // Arrange - administrator
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        
        // Create admin role
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(Role.ERole.ROLE_ADMIN);
        
        adminUser.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
        
        when(userService.getCurrentUser(anyString())).thenReturn(adminUser);
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(testTransaction));

        // Act & Assert
        assertDoesNotThrow(() -> transactionService.validateTransactionAccess("admin@example.com", 1L));

        // Verify
        verify(userService).getCurrentUser(eq("admin@example.com"));
        verify(transactionRepository).findById(eq(1L));
    }

    @Test
    void validateTransactionAccess_CardOwner() {
        // Arrange - card owner
        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(testTransaction));

        // Act & Assert
        assertDoesNotThrow(() -> transactionService.validateTransactionAccess("test@example.com", 1L));

        // Verify
        verify(userService).getCurrentUser(eq("test@example.com"));
        verify(transactionRepository).findById(eq(1L));
    }

    @Test
    void validateTransactionAccess_NotOwnerNotAdmin() {
        // Arrange - not owner and not admin
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail("other@example.com");
        otherUser.setRoles(new HashSet<>());
        
        // Cards belong to another user
        User cardOwner = new User();
        cardOwner.setId(4L);
        cardOwner.setEmail("cardowner@example.com");
        sourceCard.setUser(cardOwner);
        targetCard.setUser(cardOwner);
        
        when(userService.getCurrentUser(anyString())).thenReturn(otherUser);
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(testTransaction));

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> 
            transactionService.validateTransactionAccess("other@example.com", 1L)
        );

        // Verify
        verify(userService).getCurrentUser(eq("other@example.com"));
        verify(transactionRepository).findById(eq(1L));
    }
} 