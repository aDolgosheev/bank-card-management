package com.dolgosheev.bankcardmanagement.repository;

import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {
    Page<Card> findByUser(User user, Pageable pageable);
    List<Card> findByUser(User user);
    Optional<Card> findByIdAndUser(Long id, User user);
    boolean existsByCardNumberEncrypted(String encryptedCardNumber);
} 