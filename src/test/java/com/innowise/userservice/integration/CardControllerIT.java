package com.innowise.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("CardController integration tests")
public class CardControllerIT extends BaseIntegrationTest{

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();
        cacheManager.getCacheNames()
                .forEach(cacheName -> {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cache.clear();
                    }
                });
    }

    @Nested
    @DisplayName("Test POST /api/v1/users/{userId}/cards")
    class AddCardToUserTests {

        @Test
        @DisplayName("should successfully add card to user")
        void shouldAddCardToUser_Success() throws Exception {
            // Given
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");

            // When & Then
            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.number").value("1234-5678-9012-3456"))
                    .andExpect(jsonPath("$.holder").value("IVAN IVANOV"))
                    .andExpect(jsonPath("$.active").value(true));

            // Проверяем что карта сохранилась в БД
            List<Card> cards = cardRepository.findByUserId(user.getId());
            assertThat(cards).hasSize(1);
            assertThat(cards.getFirst().getNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturnNotFound_WhenUserNotExists() throws Exception {
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", 999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("User not found")));
        }

        @Test
        @DisplayName("should return 409 when card number exists")
        void shouldReturnConflict_WhenCardNumberExists() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            String cardNumber = "1234-5678-9012-3456";

            createAndSaveCard(user, cardNumber);

            CardRequestDto requestDto = createCardRequestDto(cardNumber);

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")));
        }

        @Test
        @DisplayName("should return 409 when user has 5 cards")
        void shouldReturnConflict_WhenUserHas5Cards() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");

            // Добавляем 5 карт
            for (int i = 1; i <= 5; i++) {
                createAndSaveCard(user, String.format("1234-5678-9012-345%d", i));
            }

            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3460");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already has 5 cards")));

            List<Card> cards = cardRepository.findByUserId(user.getId());
            assertThat(cards).hasSize(5);
        }

        @Test
        @DisplayName("should return 400 when invalid card data")
        void shouldReturnBadRequest_WhenInvalidCardData() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");

            CardRequestDto requestDto = CardRequestDto.builder()
                    .number("invalid") // невалидный формат номера
                    .holder("IVAN IVANOV")
                    .expirationDate(LocalDate.now().minusDays(1)) // дата в прошлом
                    .active(true)
                    .build();

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/users/{userId}/cards")
    class GetUserCardsTests {

        @Test
        @DisplayName("should successfully return user cards")
        void shouldSuccessfullyReturnUserCards() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            createAndSaveCard(user, "1234-5678-9012-3451");
            createAndSaveCard(user, "1234-5678-9012-3452");
            createAndSaveCard(user, "1234-5678-9012-3453");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].number").exists())
                    .andExpect(jsonPath("$[1].number").exists())
                    .andExpect(jsonPath("$[2].number").exists());
        }

        @Test
        @DisplayName("should return empty list when user has no cards")
        void shouldReturnEmptyList_WhenUserHasNoCards() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should use cache on second request")
        void shouldUseCache_OnSecondRequest() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId()))
                    .andExpect(status().isOk());

            cardRepository.deleteById(card.getId());

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/cards/{cardId}")
    class UpdateCardTests {

        @Test
        @DisplayName("should successfully update card")
        void shouldSuccessfullyUpdateCard() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card existingCard = createAndSaveCard(user, "1234-5678-9012-3456");

            CardRequestDto updateDto = CardRequestDto.builder()
                    .number("1234-5678-9012-3456")
                    .holder("PETR PETROV")
                    .expirationDate(LocalDate.now().plusYears(3))
                    .active(false)
                    .build();

            mockMvc.perform(put("/api/v1/cards/{cardId}", existingCard.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(existingCard.getId()))
                    .andExpect(jsonPath("$.holder").value("PETR PETROV"))
                    .andExpect(jsonPath("$.active").value(false));

            Card updatedCard = cardRepository.findById(existingCard.getId()).orElseThrow();
            assertThat(updatedCard.getHolder()).isEqualTo("PETR PETROV");
            assertThat(updatedCard.getActive()).isFalse();
        }

        @Test
        @DisplayName("should return 404 when card doesnt find")
        void shouldReturnNotFound_WhenCardDoesNotExist() throws Exception {
            CardRequestDto updateDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(put("/api/v1/cards/{cardId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/cards/{cardId}")
    class DeleteCardTests {

        @Test
        @DisplayName("should successfully delete card")
        void shouldSuccessfullyDeleteCard() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");

            mockMvc.perform(delete("/api/v1/cards/{cardId}", card.getId()))
                    .andExpect(status().isNoContent());

            assertThat(cardRepository.findById(card.getId())).isEmpty();
        }

        @Test
        @DisplayName("Cards should cascade delete when user deleted")
        void shouldCascadeDelete_WhenUserDeleted() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card1 = createAndSaveCard(user, "1234-5678-9012-3451");
            Card card2 = createAndSaveCard(user, "1234-5678-9012-3452");

            mockMvc.perform(delete("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isNoContent());

            assertThat(cardRepository.findById(card1.getId())).isEmpty();
            assertThat(cardRepository.findById(card2.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Test PATCH /api/v1/cards/{cardId}/activity")
    class ChangeCardActivityTests {

        @Test
        @DisplayName("should activate card")
        void shouldActivateCard() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            card.setActive(false);
            cardRepository.save(card);

            mockMvc.perform(patch("/api/v1/cards/{cardId}/activity", card.getId())
                            .param("isActive", "true"))
                    .andExpect(status().isOk());

            Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(updatedCard.getActive()).isTrue();
        }

        @Test
        @DisplayName("should deactivate card")
        void shouldDeactivateCard() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");

            mockMvc.perform(patch("/api/v1/cards/{cardId}/activity", card.getId())
                            .param("isActive", "false"))
                    .andExpect(status().isOk());

            Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(updatedCard.getActive()).isFalse();
        }
    }

    private User createAndSaveUser(String name, String email) {
        User user = User.builder()
                .name(name)
                .surname("Ivanov")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email(email)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    private Card createAndSaveCard(User user, String number) {
        Card card = Card.builder()
                .user(user)
                .number(number)
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();
        return cardRepository.save(card);
    }

    private CardRequestDto createCardRequestDto(String number) {
        return CardRequestDto.builder()
                .number(number)
                .holder("IVAN IVANOV")
                .expirationDate(LocalDate.now().plusYears(2))
                .active(true)
                .build();
    }
}
