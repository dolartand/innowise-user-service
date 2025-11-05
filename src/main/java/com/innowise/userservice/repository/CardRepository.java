package com.innowise.userservice.repository;

import com.innowise.userservice.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card,Long> {

    List<Card> findByUserId(Long userId);

    Optional<Card> findByNumber(String number);

    int countByUserId(Long userId);

    // JPQL

    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.active = :active")
    Page<Card> findActiveCardsWithUser(@Param("active") Boolean active, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE LOWER(c.holder) LIKE LOWER(CONCAT('%', :holder, '%'))")
    List<Card> findByHolderContaining(@Param("holder") String holder);

    // Native sql

    @Modifying
    @Query(value = "UPDATE payment_cards SET active = true WHERE id = :cardId", nativeQuery = true)
    int activateCard(@Param("cardId") Long cardId);

    @Modifying
    @Query(value = "UPDATE payment_cards SET active = false WHERE id = :cardId", nativeQuery = true)
    int deactivateCard(@Param("cardId") Long cardId);
}
