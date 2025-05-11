package com.dolgosheev.bankcardmanagement.specification;

import com.dolgosheev.bankcardmanagement.entity.Card;
import com.dolgosheev.bankcardmanagement.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class CardSpecification {

    public static Specification<Card> hasStatus(Card.CardStatus status) {
        return (root, query, criteriaBuilder) -> 
                status == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Card> balanceGreaterThan(BigDecimal balance) {
        return (root, query, criteriaBuilder) ->
                balance == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThan(root.get("balance"), balance);
    }

    public static Specification<Card> balanceLessThan(BigDecimal balance) {
        return (root, query, criteriaBuilder) ->
                balance == null ? criteriaBuilder.conjunction() : criteriaBuilder.lessThan(root.get("balance"), balance);
    }

    public static Specification<Card> belongsToUser(User user) {
        return (root, query, criteriaBuilder) ->
                user == null ? criteriaBuilder.conjunction() : criteriaBuilder.equal(root.get("user"), user);
    }

    public static Specification<Card> userIdEquals(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (userId == null) {
                return criteriaBuilder.conjunction();
            }
            Join<Card, User> userJoin = root.join("user");
            return criteriaBuilder.equal(userJoin.get("id"), userId);
        };
    }

    public static Specification<Card> cardholderNameContains(String name) {
        return (root, query, criteriaBuilder) ->
                name == null ? criteriaBuilder.conjunction() : 
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("cardholderName")), 
                        "%" + name.toLowerCase() + "%");
    }
} 