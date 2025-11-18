package com.innowise.userservice.service.impl;

import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.dto.card.CardResponseDto;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.exception.BusinessException;
import com.innowise.userservice.exception.CardLimitExceededException;
import com.innowise.userservice.exception.ForbiddenException;
import com.innowise.userservice.exception.ResourceNotFoundException;
import com.innowise.userservice.mapper.CardMapper;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardService unit tests")
public class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardServiceImpl cardService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("addCardToUser tests")
    class AddCardToUserTests {

        @Test
        @DisplayName("should successfully ad card to user")
        void shouldAddCardToUser_Success() {
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User user = createTestUser(userId, 2);
            Card newCard = createTestCard(null, user);
            CardResponseDto expected = createTestCardResponseDto(1L);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(cardRepository.findByNumber(requestDto.number())).thenReturn(Optional.empty());
            when(cardMapper.toCard(requestDto)).thenReturn(newCard);
            when(userRepository.save(user)).thenReturn(user);
            when(cardMapper.toCardResponseDto(newCard)).thenReturn(expected);

            CardResponseDto result = cardService.addCardToUser(userId, requestDto);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.number()).isEqualTo(requestDto.number());

            verify(userRepository, times(1)).findById(userId);
            verify(cardRepository, times(1)).findByNumber(requestDto.number());
            verify(userRepository, times(1)).save(user);
        }

        @Test
        @DisplayName("should trow ResourceNotFoundException when dont find user")
        void shouldThrowResourceNotFoundException_WhenUserNotFound() {
            Long userId = 999L;
            CardRequestDto requestDto = createTestCardRequestDto();

            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.addCardToUser(userId, requestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User")
                    .hasMessageContaining("id " + userId);

            verify(userRepository, times(1)).findById(userId);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when card number already exists")
        void shouldThrowBusinessException_WhenCardNumberAlreadyExists() {
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User user = createTestUser(userId, 2);
            Card existingCard = createTestCard(10L, user);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(cardRepository.findByNumber(requestDto.number())).thenReturn(Optional.of(existingCard));

            assertThatThrownBy(() -> cardService.addCardToUser(userId, requestDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Card with number ")
                    .hasMessageContaining("already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw CardLimitExceededException when user already has 5 cards")
        void shouldThrowCardLimitExceededException_WhenUserAlreadyHas5Cards() {
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User user = createTestUser(userId, 5);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(cardRepository.findByNumber(requestDto.number())).thenReturn(Optional.empty());

            Card newCard = createTestCard(null, user);
            when(cardMapper.toCard(requestDto)).thenReturn(newCard);

            assertThatThrownBy(() -> cardService.addCardToUser(userId, requestDto))
                    .isInstanceOf(CardLimitExceededException.class)
                    .hasMessageContaining("already has 5 cards");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findCardsByUserId tests")
    class FindCardByUserIdTests {

        @Test
        @DisplayName("should successfully return list of user's cards")
        void shouldReturnUserCards_Success() {
            Long userId = 1L;
            User user = createTestUser(userId, 3);
            List<Card> cards = user.getCards();
            List<CardResponseDto> expectedDtos = List.of(
                    createTestCardResponseDto(1L),
                    createTestCardResponseDto(2L),
                    createTestCardResponseDto(3L)
            );

            when(userRepository.existsById(userId)).thenReturn(true);
            when(cardRepository.findByUserId(userId)).thenReturn(cards);
            when(cardMapper.toCardResponseDtoList(cards)).thenReturn(expectedDtos);

            List<CardResponseDto> result = cardService.findCardsByUserId(userId);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result.getFirst().id()).isEqualTo(1L);

            verify(userRepository, times(1)).existsById(userId);
            verify(cardRepository, times(1)).findByUserId(userId);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when user not found")
        void shouldThrowResourceNotFoundException_WhenUserNotFound() {
            Long userId = 999L;
            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> cardService.findCardsByUserId(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");

            verify(userRepository, times(1)).existsById(userId);
            verify(cardRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("updateCard tests")
    class UpdateCardTests {

        @Test
        @DisplayName("should successfully update card when user is owner")
        void shouldUpdateCard_WhenUserIsOwner() {
            Long cardId = 1L;
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User user = createTestUser(userId, 1);
            Card existingCard = createTestCard(cardId, user);
            Card updatedCard = createTestCard(cardId, user);
            CardResponseDto expected = createTestCardResponseDto(cardId);

            setupSecurityContext(userId, "USER");

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(cardRepository.findByNumber(requestDto.number())).thenReturn(Optional.empty());
            when(cardRepository.save(existingCard)).thenReturn(updatedCard);
            when(cardMapper.toCardResponseDto(updatedCard)).thenReturn(expected);

            CardResponseDto result = cardService.updateCard(cardId, requestDto, userId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(cardId);

            verify(cardRepository, times(1)).findById(cardId);
            verify(cardMapper, times(1)).updateCardFromDto(requestDto, existingCard);
            verify(cardRepository, times(1)).save(existingCard);
        }

        @Test
        @DisplayName("should successfully update card when user is admin")
        void shouldUpdateCard_WhenUserIsAdmin() {
            Long cardId = 1L;
            Long cardOwnerId = 1L;
            Long adminId = 2L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User cardOwner = createTestUser(cardOwnerId, 1);
            Card existingCard = createTestCard(cardId, cardOwner);
            Card updatedCard = createTestCard(cardId, cardOwner);
            CardResponseDto expected = createTestCardResponseDto(cardId);

            // Настраиваем SecurityContext для админа
            setupSecurityContext(adminId, "ADMIN");

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(cardRepository.findByNumber(requestDto.number())).thenReturn(Optional.empty());
            when(cardRepository.save(existingCard)).thenReturn(updatedCard);
            when(cardMapper.toCardResponseDto(updatedCard)).thenReturn(expected);

            CardResponseDto result = cardService.updateCard(cardId, requestDto, adminId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(cardId);

            verify(cardRepository, times(1)).findById(cardId);
            verify(cardRepository, times(1)).save(existingCard);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not owner and not admin")
        void shouldThrowForbiddenException_WhenUserIsNotOwnerAndNotAdmin() {
            Long cardId = 1L;
            Long cardOwnerId = 1L;
            Long anotherUserId = 2L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User cardOwner = createTestUser(cardOwnerId, 1);
            Card existingCard = createTestCard(cardId, cardOwner);

            // Настраиваем SecurityContext для обычного пользователя (не владельца)
            setupSecurityContext(anotherUserId, "USER");

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));

            assertThatThrownBy(() -> cardService.updateCard(cardId, requestDto, anotherUserId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("You cant update this card");

            verify(cardRepository, times(1)).findById(cardId);
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when card not found")
        void shouldThrowResourceNotFoundException_WhenCardNotFound() {
            Long cardId = 999L;
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();

            when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cardService.updateCard(cardId, requestDto, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Card");

            verify(cardRepository, times(1)).findById(cardId);
            verify(cardRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw BusinessException when card number taken by another card")
        void  shouldThrowBusinessException_WhenCardNumberTakenByAnotherCard() {
            Long cardId = 1L;
            Long anotherCardId = 2L;
            Long userId = 1L;
            CardRequestDto requestDto = createTestCardRequestDto();
            User user = createTestUser(userId, 2);
            Card existingCard = createTestCard(cardId, user);
            Card cardWithSameNumber = createTestCard(anotherCardId, user);

            setupSecurityContext(userId, "USER");

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
            when(cardRepository.findByNumber(requestDto.number()))
                    .thenReturn(Optional.of(cardWithSameNumber));

            assertThatThrownBy(() -> cardService.updateCard(cardId, requestDto, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already taken");

            verify(cardRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteCard tests")
    class DeleteCardTests {

        @Test
        @DisplayName("should successfully delete card")
        void shouldDeleteCard_Success() {
            Long cardId = 1L;
            when(cardRepository.existsById(cardId)).thenReturn(true);

            cardService.deleteCard(cardId);

            verify(cardRepository, times(1)).existsById(cardId);
            verify(cardRepository, times(1)).deleteById(cardId);
        }

        @Test
        @DisplayName("shpuld throw ResourceNotFoundException when card doesnt exists")
        void shouldThrowResourceNotFoundException_WhenCardNotFound() {
            Long cardId = 999L;
            when(cardRepository.existsById(cardId)).thenReturn(false);

            assertThatThrownBy(() -> cardService.deleteCard(cardId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Card");

            verify(cardRepository, times(1)).existsById(cardId);
            verify(cardRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("changeCardActivity tests")
    class ChangeCardActivityTests {

        @Test
        @DisplayName("should activate card")
        void shouldActivateCard_Success() {
            Long cardId = 1L;
            Boolean isActive = true;

            when(cardRepository.existsById(cardId)).thenReturn(true);
            when(cardRepository.activateCard(cardId)).thenReturn(1);

            cardService.changeCardActivity(cardId, isActive);

            verify(cardRepository, times(1)).existsById(cardId);
            verify(cardRepository, times(1)).activateCard(cardId);
            verify(cardRepository, never()).deactivateCard(any());
        }

        @Test
        @DisplayName("should deactivate card")
        void shouldDeactivateCard_Success() {
            Long cardId = 1L;
            Boolean isActive = false;

            when(cardRepository.existsById(cardId)).thenReturn(true);
            when(cardRepository.deactivateCard(cardId)).thenReturn(1);

            cardService.changeCardActivity(cardId, isActive);

            verify(cardRepository, times(1)).existsById(cardId);
            verify(cardRepository, times(1)).deactivateCard(cardId);
            verify(cardRepository, never()).activateCard(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when card doesn't exist")
        void shouldThrowResourceNotFoundException_WhenCardNotFound() {
            Long cardId = 999L;
            when(cardRepository.existsById(cardId)).thenReturn(false);

            assertThatThrownBy(() -> cardService.changeCardActivity(cardId, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Card");

            verify(cardRepository, times(1)).existsById(cardId);
            verify(cardRepository, never()).activateCard(any());
            verify(cardRepository, never()).deactivateCard(any());
        }
    }

    private void setupSecurityContext(Long userId, String role) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

        lenient().when(authentication.getPrincipal()).thenReturn(userId);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getAuthorities()).thenReturn((Collection) List.of(authority));
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }


    private User createTestUser(Long id, int numberOfCards) {
        User user = User.builder()
                .id(id)
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("ivan@example.com")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .cards(new ArrayList<>())
                .build();

        for (int i = 1; i <= numberOfCards; i++) {
            Card card = createTestCard((long) i, user);
            user.getCards().add(card);
        }
        return user;
    }

    private Card createTestCard(Long id, User user) {
        return Card.builder()
                .id(id)
                .user(user)
                .number("1234-5678-9012-3456")
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private CardRequestDto createTestCardRequestDto() {
        return CardRequestDto.builder()
                .number("1234-5678-9012-3456")
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();
    }

    private CardResponseDto createTestCardResponseDto(Long id) {
        return CardResponseDto.builder()
                .id(id)
                .number("1234-5678-9012-3456")
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();
    }
}