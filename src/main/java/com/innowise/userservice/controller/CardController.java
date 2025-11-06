package com.innowise.userservice.controller;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;
import com.innowise.userservice.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping("users/{userId}/cards")
    public ResponseEntity<CardResponseDto> addCardToUser(
            @PathVariable Long userId,
            @Valid @RequestBody CardRequestDto cardRequestDto
    ) {
        CardResponseDto createdCard = cardService.addCardToUser(userId, cardRequestDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdCard);
    }

    @GetMapping("users/{userId}/cards")
    public ResponseEntity<List<CardResponseDto>> getUserCards(@PathVariable Long userId) {
        List<CardResponseDto> cards = cardService.findCardsByUserId(userId);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("cards/{cardId}")
    public ResponseEntity<CardResponseDto> updateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequestDto cardRequestDto
    ) {
        CardResponseDto updatedCard = cardService.updateCard(cardId, cardRequestDto);
        return ResponseEntity.ok(updatedCard);
    }

    @DeleteMapping("cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("cards/{cardId}/activity")
    public ResponseEntity<Void> changeCardActivity(
            @PathVariable Long cardId,
            @RequestParam Boolean isActive
    )  {
        cardService.changeCardActivity(cardId, isActive);
        return ResponseEntity.ok().build();
    }
}
