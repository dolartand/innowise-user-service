package com.innowise.userservice.service;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;

import java.util.List;

public interface CardService {

    CardResponseDto addCardToUser(Long userId, CardRequestDto cardRequestDto);

    List<CardResponseDto> findCardsByUserId(Long userId);

    CardResponseDto updateCard(Long cardId, CardRequestDto cardRequestDto, Long userId);

    void deleteCard(Long cardId);

    void changeCardActivity(Long cardId, Boolean isActive);
}
