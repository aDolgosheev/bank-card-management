package com.dolgosheev.bankcardmanagement.repository;

import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySourceCardOrTargetCard(Card sourceCard, Card targetCard, Pageable pageable);
    List<Transaction> findBySourceCardOrTargetCard(Card sourceCard, Card targetCard);
} 