package com.innowise.userservice.controller;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing user cards
 * Paths:
 * - /api/v1/users/{userId}/cards - for cards of specific user
 * - /api/v1/cards/{id} - for specific card
 */
@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    /**
     * Adding a new card to user (user can add card only to itself)
     * @param userId
     * @param cardRequestDto new card data
     * @return created card data
     */
    @PostMapping("users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<CardResponseDto> addCardToUser(
            @PathVariable Long userId,
            @Valid @RequestBody CardRequestDto cardRequestDto
    ) {
        CardResponseDto createdCard = cardService.addCardToUser(userId, cardRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCard);
    }

    /**
     * Receiving all user cards (user can get only itself)
     * @param userId
     * @return list of user cards
     */
    @GetMapping("users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal")
    public ResponseEntity<List<CardResponseDto>> getUserCards(@PathVariable Long userId) {
        List<CardResponseDto> cards = cardService.findCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    /**
     * Full card data update (user can update only itself card)
     * @param cardId
     * @param cardRequestDto new card data
     * @return updated card data
     */
    @PutMapping("cards/{cardId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CardResponseDto> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequestDto cardRequestDto,
            @AuthenticationPrincipal Long userId
    ) {
        CardResponseDto updatedCard = cardService.updateCard(cardId, cardRequestDto, userId);
        return ResponseEntity.ok(updatedCard);
    }

    /**
     * Delete card (only ADMIN or owner)
     * @param cardId
     * @return 204 NO CONTENT
     */
    @DeleteMapping("cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change card activity status (only ADMIN)
     * @param cardId
     * @param isActive new activity status
     * @return 200 OK
     */
    @PatchMapping("cards/{cardId}/activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeCardActivity(
            @PathVariable Long cardId,
            @RequestParam Boolean isActive
    )  {
        cardService.changeCardActivity(cardId, isActive);
        return ResponseEntity.ok().build();
    }
}
