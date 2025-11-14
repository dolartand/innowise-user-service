package com.innowise.userservice.mapper;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;
import com.innowise.userservice.entity.Card;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface CardMapper {
    Card toCard(CardRequestDto cardRequestDto);

    CardResponseDto toCardResponseDto(Card card);

    List<CardResponseDto> toCardResponseDtoList(List<Card> cards);

    void updateCardFromDto(CardRequestDto cardRequestDto, @MappingTarget Card card);
}
