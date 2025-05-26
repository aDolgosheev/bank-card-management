package com.dolgosheev.bankcardmanagement.service;

import com.dolgosheev.bankcardmanagement.dto.card.CardCreateRequest;
import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.CardRepository;
import com.dolgosheev.bankcardmanagement.util.CardNumberEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Тесты для сервиса управления картами.
 * Проверяет основную функциональность создания, получения и обновления карт.
 * 
 * @author Dolgosheev
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CardNumberEncryptor cardNumberEncryptor;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CardCreateRequest cardCreateRequest;
    private final String CARD_NUMBER = "1234567890123456";
    private final String ENCRYPTED_CARD_NUMBER = "encryptedCardNumber";
    private final String MASKED_CARD_NUMBER = "**** **** **** 3456";

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

        // Create test card
        testCard = new Card();
        testCard.setId(1L);
        testCard.setCardNumberEncrypted(ENCRYPTED_CARD_NUMBER);
        testCard.setCardholderName("Test User");
        testCard.setExpirationDate(LocalDate.now().plusYears(2));
        testCard.setStatus(Card.CardStatus.ACTIVE);
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setUser(testUser);

        // Create card creation request
        cardCreateRequest = new CardCreateRequest();
        cardCreateRequest.setCardNumber(CARD_NUMBER);
        cardCreateRequest.setCardholderName("Test User");
        cardCreateRequest.setExpirationDate(LocalDate.now().plusYears(2));
        cardCreateRequest.setInitialBalance(new BigDecimal("1000.00"));
        cardCreateRequest.setUserId(1L);
    }

    /**
     * Тест успешного создания карты.
     * Проверяет, что карта создается с правильными параметрами.
     */
    @Test
    void createCard_Success() {
        // Arrange
        when(userService.getUserById(anyLong())).thenReturn(testUser);
        when(cardNumberEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_CARD_NUMBER);
        when(cardRepository.existsByCardNumberEncrypted(anyString())).thenReturn(false);
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act
            Card result = cardService.createCard(cardCreateRequest);

            // Assert
            assertNotNull(result);
            assertEquals(testCard.getId(), result.getId());
            assertEquals(testCard.getCardholderName(), result.getCardholderName());

            // Verify
            verify(userService).getUserById(eq(1L));
            verify(cardNumberEncryptor).encrypt(eq(CARD_NUMBER));
            verify(cardRepository).existsByCardNumberEncrypted(eq(ENCRYPTED_CARD_NUMBER));
            verify(cardRepository).save(any(Card.class));
            verify(cardNumberEncryptor).decrypt(eq(ENCRYPTED_CARD_NUMBER));
        }
    }

    /**
     * Тест создания карты с уже существующим номером.
     * Проверяет, что выбрасывается исключение при попытке создать карту с дублирующимся номером.
     */
    @Test
    void createCard_CardNumberAlreadyExists() {
        // Arrange
        when(userService.getUserById(anyLong())).thenReturn(testUser);
        when(cardNumberEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_CARD_NUMBER);
        when(cardRepository.existsByCardNumberEncrypted(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> cardService.createCard(cardCreateRequest));

        // Verify
        verify(userService).getUserById(eq(1L));
        verify(cardNumberEncryptor).encrypt(eq(CARD_NUMBER));
        verify(cardRepository).existsByCardNumberEncrypted(eq(ENCRYPTED_CARD_NUMBER));
        verify(cardRepository, never()).save(any(Card.class));
    }

    /**
     * Тест успешного получения карты по ID.
     * Проверяет, что карта корректно извлекается и маскируется.
     */
    @Test
    void getCardById_Success() {
        // Arrange
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(testCard));
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act
            Card result = cardService.getCardById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(testCard.getId(), result.getId());

            // Verify
            verify(cardRepository).findById(eq(1L));
            verify(cardNumberEncryptor).decrypt(eq(ENCRYPTED_CARD_NUMBER));
        }
    }

    /**
     * Тест проверки доступа администратора к карте.
     * Проверяет, что администратор имеет доступ к любой карте.
     */
    @Test
    void validateCardAccess_Admin() {
        // Arrange - administrator
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        
        // Create admin role
        com.dolgosheev.bankcardmanagement.entity.Role adminRole = new com.dolgosheev.bankcardmanagement.entity.Role();
        adminRole.setId(1L);
        adminRole.setName(com.dolgosheev.bankcardmanagement.entity.Role.ERole.ROLE_ADMIN);
        
        adminUser.setRoles(new HashSet<>(Collections.singletonList(adminRole)));
        
        when(userService.getCurrentUser(anyString())).thenReturn(adminUser);
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(testCard));
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act & Assert
            assertDoesNotThrow(() -> cardService.validateCardAccess("admin@example.com", 1L));

            // Verify
            verify(userService).getCurrentUser(eq("admin@example.com"));
            verify(cardRepository).findById(eq(1L));
        }
    }

    /**
     * Тест проверки доступа владельца к своей карте.
     * Проверяет, что владелец карты имеет к ней доступ.
     */
    @Test
    void validateCardAccess_Owner() {
        // Arrange - card owner
        when(userService.getCurrentUser(anyString())).thenReturn(testUser);
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(testCard));
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act & Assert
            assertDoesNotThrow(() -> cardService.validateCardAccess("test@example.com", 1L));

            // Verify
            verify(userService).getCurrentUser(eq("test@example.com"));
            verify(cardRepository).findById(eq(1L));
        }
    }

    /**
     * Тест проверки доступа пользователя, который не является владельцем карты и не администратор.
     * Проверяет, что выбрасывается исключение AccessDeniedException.
     */
    @Test
    void validateCardAccess_NotOwnerNotAdmin() {
        // Arrange - not owner and not admin
        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail("other@example.com");
        otherUser.setRoles(new HashSet<>());
        
        when(userService.getCurrentUser(anyString())).thenReturn(otherUser);
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(testCard));
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act & Assert
            assertThrows(AccessDeniedException.class, () -> cardService.validateCardAccess("other@example.com", 1L));

            // Verify
            verify(userService).getCurrentUser(eq("other@example.com"));
            verify(cardRepository).findById(eq(1L));
        }
    }

    /**
     * Тест успешного обновления статуса карты.
     * Проверяет, что статус карты корректно обновляется.
     */
    @Test
    void updateCardStatus_Success() {
        // Arrange
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenReturn(testCard);
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act
            Card result = cardService.updateCardStatus(1L, Card.CardStatus.BLOCKED);

            // Assert
            assertNotNull(result);
            assertEquals(Card.CardStatus.BLOCKED, result.getStatus());

            // Verify
            verify(cardRepository).findById(eq(1L));
            verify(cardRepository).save(any(Card.class));
            verify(cardNumberEncryptor, times(2)).decrypt(eq(ENCRYPTED_CARD_NUMBER)); // Вызывается дважды: в getCardById и setMaskedCardNumber
        }
    }

    /**
     * Тест обновления статуса просроченной карты.
     * Проверяет, что статус автоматически устанавливается в EXPIRED для просроченных карт.
     */
    @Test
    void updateCardStatus_ExpiredCard() {
        // Arrange
        Card expiredCard = new Card();
        expiredCard.setId(1L);
        expiredCard.setCardNumberEncrypted(ENCRYPTED_CARD_NUMBER);
        expiredCard.setExpirationDate(LocalDate.now().minusDays(1)); // Card with expired date
        expiredCard.setStatus(Card.CardStatus.ACTIVE);
        
        when(cardRepository.findById(anyLong())).thenReturn(Optional.of(expiredCard));
        when(cardRepository.save(any(Card.class))).thenReturn(expiredCard);
        when(cardNumberEncryptor.decrypt(anyString())).thenReturn(CARD_NUMBER);

        // Мокируем статический метод правильно
        try (MockedStatic<CardNumberEncryptor> mockedStatic = mockStatic(CardNumberEncryptor.class)) {
            mockedStatic.when(() -> CardNumberEncryptor.maskCardNumber(anyString())).thenReturn(MASKED_CARD_NUMBER);

            // Act
            Card result = cardService.updateCardStatus(1L, Card.CardStatus.ACTIVE);

            // Assert
            assertNotNull(result);
            assertEquals(Card.CardStatus.EXPIRED, result.getStatus()); // Status should be EXPIRED regardless of the requested one

            // Verify
            verify(cardRepository).findById(eq(1L));
            verify(cardRepository).save(any(Card.class));
            verify(cardNumberEncryptor, times(2)).decrypt(eq(ENCRYPTED_CARD_NUMBER)); // Вызывается дважды: в getCardById и setMaskedCardNumber
        }
    }
} 