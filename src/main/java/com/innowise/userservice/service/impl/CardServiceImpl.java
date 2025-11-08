package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;

    @Override
    @CachePut(value = "card", key = "#result.id()")
    @CacheEvict(value = "cards", key = "#userId")
    @Transactional
    public CardResponseDto addCardToUser(Long userId, CardRequestDto cardRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id " + userId));

        cardRepository.findByNumber(cardRequestDto.number()).ifPresent(card -> {
            throw new BusinessException("Card with number " + cardRequestDto.number() + " already exists");
        });

        Card newCard = cardMapper.toCard(cardRequestDto);
        user.addCard(newCard);

        userRepository.save(user);

        return cardMapper.toCardResponseDto(newCard);
    }

    @Override
    @Cacheable(value = "cards", key = "#userId", unless = "#result.isEmpty()")
    @Transactional(readOnly = true)
    public List<CardResponseDto> findCardsByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id " + userId);
        }

        List<Card> cards = cardRepository.findByUserId(userId);
        return cardMapper.toCardResponseDtoList(cards);
    }

    @Override
    @CachePut(value = "card", key = "#result.id()")
    @CacheEvict(value = "cards", allEntries = true)
    @Transactional
    public CardResponseDto updateCard(Long cardId, CardRequestDto cardRequestDto) {
        Card cardToUpdate = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id " + cardId));

        cardRepository.findByNumber(cardRequestDto.number()).ifPresent(card -> {
            if (!card.getId().equals(cardId)) {
                throw new BusinessException("Card with number " + cardRequestDto.number() + " already taken");
            }
        });

        cardMapper.updateCardFromDto(cardRequestDto, cardToUpdate);
        Card updatedCard = cardRepository.save(cardToUpdate);
        return cardMapper.toCardResponseDto(updatedCard);
    }

    @Override
    @CacheEvict(value = {"card", "cards"}, allEntries = true)
    @Transactional
    public void deleteCard(Long cardId) {
        if  (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card", "id " + cardId);
        }

        cardRepository.deleteById(cardId);
    }

    @Override
    @CacheEvict(value = {"card", "cards"}, allEntries = true)
    @Transactional
    public void changeCardActivity(Long cardId, Boolean isActive) {
        if (!cardRepository.existsById(cardId)) {
            throw new ResourceNotFoundException("Card", "id " + cardId);
        }

        if (isActive) {
            cardRepository.activateCard(cardId);
        } else {
            cardRepository.deactivateCard(cardId);
        }
    }
}
