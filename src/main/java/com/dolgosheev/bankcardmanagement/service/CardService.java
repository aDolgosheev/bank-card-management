package com.dolgosheev.bankcardmanagement.service;

import com.dolgosheev.bankcardmanagement.dto.card.CardCreateRequest;
import com.dolgosheev.bankcardmanagement.dto.card.CardFilterRequest;
import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.Role;
import com.dolgosheev.bankcardmanagement.entity.User;
import com.dolgosheev.bankcardmanagement.repository.CardRepository;
import com.dolgosheev.bankcardmanagement.specification.CardSpecification;
import com.dolgosheev.bankcardmanagement.util.CardNumberEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CardService {
    
    private final CardRepository cardRepository;
    private final UserService userService;
    private final CardNumberEncryptor cardNumberEncryptor;

    @Transactional
    public Card createCard(CardCreateRequest cardCreateRequest) {
        User user = userService.getUserById(cardCreateRequest.getUserId());
        
        // Check if card with this number already exists
        String encryptedCardNumber = cardNumberEncryptor.encrypt(cardCreateRequest.getCardNumber());
        if (cardRepository.existsByCardNumberEncrypted(encryptedCardNumber)) {
            throw new IllegalArgumentException("Card with this number already exists");
        }

        Card card = new Card();
        card.setCardNumberEncrypted(encryptedCardNumber);
        card.setCardholderName(cardCreateRequest.getCardholderName());
        card.setExpirationDate(cardCreateRequest.getExpirationDate());
        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBalance(cardCreateRequest.getInitialBalance());
        card.setUser(user);
        
        Card savedCard = cardRepository.save(card);
        
        // Set masked card number for display
        String decryptedCardNumber = cardNumberEncryptor.decrypt(encryptedCardNumber);
        savedCard.setMaskedCardNumber(CardNumberEncryptor.maskCardNumber(decryptedCardNumber));
        
        return savedCard;
    }

    @Transactional(readOnly = true)
    public Page<Card> getAllCards(Pageable pageable) {
        Page<Card> cards = cardRepository.findAll(pageable);
        
        // Set masked card numbers for display
        cards.forEach(this::setMaskedCardNumber);
        
        return cards;
    }

    @Transactional(readOnly = true)
    public Page<Card> getUserCards(Long userId, Pageable pageable) {
        User user = userService.getUserById(userId);
        Page<Card> cards = cardRepository.findByUser(user, pageable);
        
        // Set masked card numbers for display
        cards.forEach(this::setMaskedCardNumber);
        
        return cards;
    }

    @Transactional(readOnly = true)
    public Page<Card> filterCards(CardFilterRequest filterRequest, Pageable pageable) {
        Specification<Card> spec = Specification.where(null);
        
        if (filterRequest.getStatus() != null) {
            spec = spec.and(CardSpecification.hasStatus(filterRequest.getStatus()));
        }
        
        if (filterRequest.getMinBalance() != null) {
            spec = spec.and(CardSpecification.balanceGreaterThan(filterRequest.getMinBalance()));
        }
        
        if (filterRequest.getMaxBalance() != null) {
            spec = spec.and(CardSpecification.balanceLessThan(filterRequest.getMaxBalance()));
        }
        
        if (filterRequest.getUserId() != null) {
            spec = spec.and(CardSpecification.userIdEquals(filterRequest.getUserId()));
        }
        
        if (filterRequest.getCardholderName() != null) {
            spec = spec.and(CardSpecification.cardholderNameContains(filterRequest.getCardholderName()));
        }
        
        Page<Card> cards = cardRepository.findAll(spec, pageable);
        
        // Set masked card numbers for display
        cards.forEach(this::setMaskedCardNumber);
        
        return cards;
    }

    @Transactional(readOnly = true)
    public Card getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Card with id " + id + " not found"));
        
        // Set masked card number for display
        setMaskedCardNumber(card);
        
        return card;
    }

    @Transactional(readOnly = true)
    public Card getUserCardById(Long userId, Long cardId) {
        User user = userService.getUserById(userId);
        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new NoSuchElementException("Card with id " + cardId + " not found for user with id " + userId));
        
        // Set masked card number for display
        setMaskedCardNumber(card);
        
        return card;
    }

    @Transactional
    public Card updateCardStatus(Long id, Card.CardStatus status) {
        Card card = getCardById(id);
        card.setStatus(status);
        
        // Check card expiration date
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            card.setStatus(Card.CardStatus.EXPIRED);
        }
        
        card = cardRepository.save(card);
        
        // Set masked card number for display
        setMaskedCardNumber(card);
        
        return card;
    }

    @Transactional
    public void deleteCard(Long id) {
        Card card = getCardById(id);
        cardRepository.delete(card);
    }

    @Transactional
    public void requestCardBlock(Long userId, Long cardId) {
        User user = userService.getUserById(userId);
        Card card = cardRepository.findByIdAndUser(cardId, user)
                .orElseThrow(() -> new NoSuchElementException("Card with id " + cardId + " not found for user with id " + userId));
        
        card.setStatus(Card.CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    @Transactional(readOnly = true)
    public void validateCardAccess(String userEmail, Long cardId) {
        User currentUser = userService.getCurrentUser(userEmail);
        Card card = getCardById(cardId);
        
        // Administrator has access to all cards, regular user only to their own
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == Role.ERole.ROLE_ADMIN);
        boolean isOwner = card.getUser().getId().equals(currentUser.getId());
        
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("No access to card with id " + cardId);
        }
    }

    @Transactional(readOnly = true)
    public List<Card> getUserCards(Long userId) {
        User user = userService.getUserById(userId);
        List<Card> cards = cardRepository.findByUser(user);
        
        // Set masked card numbers for display
        cards.forEach(this::setMaskedCardNumber);
        
        return cards;
    }
    
    private void setMaskedCardNumber(Card card) {
        String decryptedCardNumber = cardNumberEncryptor.decrypt(card.getCardNumberEncrypted());
        card.setMaskedCardNumber(CardNumberEncryptor.maskCardNumber(decryptedCardNumber));
    }
} 