package com.innowise.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("InternalUserController integration tests")
public class InternalUserControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should successfully create user with valid service key")
    void shouldCreateUser_WithServiceKey() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(2005, 12, 22))
                .email("ivan@example.com")
                .active(true)
                .build();

        MvcResult result = mockMvc.perform(post("/internal/users")
                        .header("X-Service-Key", TEST_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Ivan"))
                .andExpect(jsonPath("$.email").value("ivan@example.com"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        UserResponseDto createdUser = objectMapper.readValue(responseBody, UserResponseDto.class);

        User userInDb = userRepository.findById(createdUser.id()).orElseThrow();
        assertThat(userInDb.getName()).isEqualTo("Ivan");
    }

    @Test
    @DisplayName("should return 403 when service key is missing")
    void shouldReturn403_WhenServiceKeyMissing() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(2005, 12, 22))
                .email("ivan@example.com")
                .active(true)
                .build();

        mockMvc.perform(post("/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 403 when service key is invalid")
    void shouldReturn403_WhenServiceKeyInvalid() throws Exception {
        UserRequestDto requestDto = UserRequestDto.builder()
                .name("Ivan")
                .surname("Ivanov")
                .birthDate(LocalDate.of(2005, 12, 22))
                .email("ivan@example.com")
                .active(true)
                .build();

        mockMvc.perform(post("/internal/users")
                        .header("X-Service-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should get user by email with service key")
    void shouldGetUserByEmail_WithServiceKey() throws Exception {
        User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

        mockMvc.perform(get("/internal/users/by-email")
                        .header("X-Service-Key", TEST_SERVICE_KEY)
                        .param("email", "ivan@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value("ivan@example.com"));
    }

    @Test
    @DisplayName("should get user by id with service key")
    void shouldGetUserById_WithServiceKey() throws Exception {
        User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

        mockMvc.perform(get("/internal/users/{userId}", user.getId())
                        .header("X-Service-Key", TEST_SERVICE_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Ivan"));
    }

    @Test
    @DisplayName("should delete user with service key")
    void shouldDeleteUser_WithServiceKey() throws Exception {
        User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

        mockMvc.perform(delete("/internal/users/{userId}", user.getId())
                        .header("X-Service-Key", TEST_SERVICE_KEY))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    private User createAndSaveUser(String name, String surname, String email) {
        User user = User.builder()
                .name(name)
                .surname(surname)
                .birthDate(LocalDate.of(1990, 1, 1))
                .email(email)
                .active(true)
                .build();
        return userRepository.save(user);
    }
}
