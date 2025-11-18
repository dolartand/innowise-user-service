package com.innowise.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.card.CardRequestDto;
import com.innowise.userservice.entity.Card;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.CardRepository;
import com.innowise.userservice.repository.UserRepository;
import com.innowise.userservice.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("CardController integration tests")
public class CardControllerIT extends BaseIntegrationTest {

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

    @Value("${jwt.secret}")
    private String jwtSecret;

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
        @DisplayName("should successfully add card to user when authenticated as owner")
        void shouldAddCardToUser_WhenAuthenticatedAsOwner() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.number").value("1234-5678-9012-3456"))
                    .andExpect(jsonPath("$.holder").value("IVAN IVANOV"))
                    .andExpect(jsonPath("$.active").value(true));

            List<Card> cards = cardRepository.findByUserId(user.getId());
            assertThat(cards).hasSize(1);
            assertThat(cards.getFirst().getNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("should successfully add card to user when authenticated as admin")
        void shouldAddCardToUser_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");
            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.number").value("1234-5678-9012-3456"));
        }

        @Test
        @DisplayName("should return 403 when user tries to add card to another user")
        void shouldReturn403_WhenUserTriesToAddCardToAnotherUser() throws Exception {
            User user1 = createAndSaveUser("Ivan", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "petr@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");
            String user1Token = generateToken(user1.getId(), user1.getEmail(), "USER");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user2.getId())
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when no authentication provided")
        void shouldReturn401_WhenNoAuthentication() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when user not found")
        void shouldReturnNotFound_WhenUserNotExists() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3456");
            String token = generateToken(999L, "nonexistent@example.com", "USER");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", 999L)
                            .header("Authorization", "Bearer " + token)
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
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")));
        }

        @Test
        @DisplayName("should return 409 when user has 5 cards")
        void shouldReturnConflict_WhenUserHas5Cards() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            for (int i = 1; i <= 5; i++) {
                createAndSaveCard(user, String.format("1234-5678-9012-345%d", i));
            }

            CardRequestDto requestDto = createCardRequestDto("1234-5678-9012-3460");

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already has 5 cards")));
        }

        @Test
        @DisplayName("should return 400 when invalid card data")
        void shouldReturnBadRequest_WhenInvalidCardData() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            CardRequestDto requestDto = CardRequestDto.builder()
                    .number("invalid")
                    .holder("IVAN IVANOV")
                    .expirationDate(LocalDate.now().minusDays(1))
                    .active(true)
                    .build();

            mockMvc.perform(post("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token)
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
        @DisplayName("should successfully return user cards when authenticated as owner")
        void shouldReturnUserCards_WhenAuthenticatedAsOwner() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            createAndSaveCard(user, "1234-5678-9012-3451");
            createAndSaveCard(user, "1234-5678-9012-3452");
            createAndSaveCard(user, "1234-5678-9012-3453");

            String token = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("should successfully return user cards when authenticated as admin")
        void shouldReturnUserCards_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            createAndSaveCard(user, "1234-5678-9012-3451");

            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("should return 403 when user tries to get cards of another user")
        void shouldReturn403_WhenUserTriesToGetCardsOfAnotherUser() throws Exception {
            User user1 = createAndSaveUser("Ivan", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "petr@example.com");
            createAndSaveCard(user2, "1234-5678-9012-3451");

            String user1Token = generateToken(user1.getId(), user1.getEmail(), "USER");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user2.getId())
                            .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return empty list when user has no cards")
        void shouldReturnEmptyList_WhenUserHasNoCards() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should use cache on second request")
        void shouldUseCache_OnSecondRequest() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            cardRepository.deleteById(card.getId());

            mockMvc.perform(get("/api/v1/users/{userId}/cards", user.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/cards/{cardId}")
    class UpdateCardTests {

        @Test
        @DisplayName("should successfully update card when authenticated as owner")
        void shouldUpdateCard_WhenAuthenticatedAsOwner() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card existingCard = createAndSaveCard(user, "1234-5678-9012-3456");
            String token = generateToken(user.getId(), user.getEmail(), "USER");

            CardRequestDto updateDto = CardRequestDto.builder()
                    .number("1234-5678-9012-3456")
                    .holder("PETR PETROV")
                    .expirationDate(LocalDate.now().plusYears(3))
                    .active(false)
                    .build();

            mockMvc.perform(put("/api/v1/cards/{cardId}", existingCard.getId())
                            .header("Authorization", "Bearer " + token)
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
        @DisplayName("should successfully update card when authenticated as admin")
        void shouldUpdateCard_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            Card existingCard = createAndSaveCard(user, "1234-5678-9012-3456");
            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            CardRequestDto updateDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(put("/api/v1/cards/{cardId}", existingCard.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 403 when user tries to update card of another user")
        void shouldReturn403_WhenUserTriesToUpdateCardOfAnotherUser() throws Exception {
            User user1 = createAndSaveUser("Ivan", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "petr@example.com");
            Card user2Card = createAndSaveCard(user2, "1234-5678-9012-3456");
            String user1Token = generateToken(user1.getId(), user1.getEmail(), "USER");

            CardRequestDto updateDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(put("/api/v1/cards/{cardId}", user2Card.getId())
                            .header("Authorization", "Bearer " + user1Token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("You cant update this card")));
        }

        @Test
        @DisplayName("should return 404 when card doesn't exist")
        void shouldReturnNotFound_WhenCardDoesNotExist() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            String token = generateToken(user.getId(), user.getEmail(), "USER");
            CardRequestDto updateDto = createCardRequestDto("1234-5678-9012-3456");

            mockMvc.perform(put("/api/v1/cards/{cardId}", 999L)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/cards/{cardId}")
    class DeleteCardTests {

        @Test
        @DisplayName("should successfully delete card when authenticated as admin")
        void shouldDeleteCard_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(delete("/api/v1/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            assertThat(cardRepository.findById(card.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to delete card")
        void shouldReturn403_WhenRegularUserTriesToDeleteCard() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            String userToken = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(delete("/api/v1/cards/{cardId}", card.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Cards should cascade delete when user deleted")
        void shouldCascadeDelete_WhenUserDeleted() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            Card card1 = createAndSaveCard(user, "1234-5678-9012-3451");
            Card card2 = createAndSaveCard(user, "1234-5678-9012-3452");
            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(delete("/api/v1/users/{id}", user.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());

            assertThat(cardRepository.findById(card1.getId())).isEmpty();
            assertThat(cardRepository.findById(card2.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Test PATCH /api/v1/cards/{cardId}/activity")
    class ChangeCardActivityTests {

        @Test
        @DisplayName("should activate card when authenticated as admin")
        void shouldActivateCard_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            card.setActive(false);
            cardRepository.save(card);

            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/api/v1/cards/{cardId}/activity", card.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .param("isActive", "true"))
                    .andExpect(status().isOk());

            Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(updatedCard.getActive()).isTrue();
        }

        @Test
        @DisplayName("should deactivate card when authenticated as admin")
        void shouldDeactivateCard_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "admin@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            String adminToken = generateToken(admin.getId(), admin.getEmail(), "ADMIN");

            mockMvc.perform(patch("/api/v1/cards/{cardId}/activity", card.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .param("isActive", "false"))
                    .andExpect(status().isOk());

            Card updatedCard = cardRepository.findById(card.getId()).orElseThrow();
            assertThat(updatedCard.getActive()).isFalse();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to change card activity")
        void shouldReturn403_WhenRegularUserTriesToChangeCardActivity() throws Exception {
            User user = createAndSaveUser("Ivan", "ivan@example.com");
            Card card = createAndSaveCard(user, "1234-5678-9012-3456");
            String userToken = generateToken(user.getId(), user.getEmail(), "USER");

            mockMvc.perform(patch("/api/v1/cards/{cardId}/activity", card.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .param("isActive", "false"))
                    .andExpect(status().isForbidden());
        }
    }

    private String generateToken(Long userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        long expirationTime = 1000 * 60 * 60; // 1 час

        return Jwts.builder()
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key)
                .compact();
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