package com.innowise.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.integration.BaseIntegrationTest;
import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@AutoConfigureMockMvc
@DisplayName("UserController integration test")
@Transactional
public class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Nested
    @DisplayName("Test POST /api/v1/users")
    class CreateUserTests {

        @Test
        @DisplayName("should successfully create user")
        void shouldCreateUser_Success() throws Exception {
            UserRequestDto requestDto = UserRequestDto.builder()
                    .name("Ivan")
                    .surname("Ivanov")
                    .birthDate(LocalDate.of(2005, 12, 22))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Ivan"))
                    .andExpect(jsonPath("$.surname").value("Ivanov"))
                    .andExpect(jsonPath("$.email").value("ivan@example.com"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            UserResponseDto createdUser = objectMapper.readValue(responseBody, UserResponseDto.class);

            User userInDb = userRepository.findById(createdUser.id()).orElseThrow();
            assertThat(userInDb.getName()).isEqualTo("Ivan");
            assertThat(userInDb.getEmail()).isEqualTo("ivan@example.com");
        }

        @Test
        @DisplayName("should return 400 when invalid data")
        void shouldReturn400WhenInvalidData() throws Exception {
            UserRequestDto requestDto = UserRequestDto.builder()
                    .name("Iv")
                    .surname("Ivanov")
                    .birthDate(LocalDate.of(2005, 12, 22))
                    .email("invalidEmail")
                    .active(true)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("should return 409 when email exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            User existingUser = User.builder()
                    .name("Ivan")
                    .surname("Ivanov")
                    .birthDate(LocalDate.of(2005, 12, 22))
                    .email("ivan@example.com")
                    .active(true)
                    .build();
            userRepository.save(existingUser);

            UserRequestDto requestDto = UserRequestDto.builder()
                    .name("John")
                    .surname("Doe")
                    .birthDate(LocalDate.of(2005, 12, 22))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("already exists")));
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user by id")
        void shouldGetUserById_Success() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.name").value("Ivan"))
                    .andExpect(jsonPath("$.surname").value("Ivanov"))
                    .andExpect(jsonPath("$.email").value("ivan@example.com"));
        }

        @Test
        @DisplayName("should return 404 when user doesnt exists")
        void shouldReturnNotFound_WhenUserNotExists() throws Exception {
            mockMvc.perform(get("/api/v1/users/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("User not found")));
        }

        @Test
        @DisplayName("should use cache on second request")
        void shouldUseCache_OnSecondRequest() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isOk());

            userRepository.deleteById(user.getId());

            mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ivan"));

            var cache = cacheManager.getCache("user");
            assertThat(cache).isNotNull();
            assertThat(cache.get(user.getId())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/users")
    class GetAllUserTests {

        @Test
        @DisplayName("should successfully return list of users with pagination")
        void shouldGetAllUsers_Success() throws Exception {
            createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            createAndSaveUser("Petr", "Petrov", "petr@example.com");
            createAndSaveUser("Sidor", "Sidorov", "sidor@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should filter users by name")
        void shouldFilterUsersByName_Success() throws Exception {
            createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            createAndSaveUser("Petr", "Petrov", "petr@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .param("name", "Ivan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Ivan"));
        }

        @Test
        @DisplayName("should filter users by activity")
        void shouldFilterUsersByActive_Success() throws Exception {
            User activeUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            activeUser.setActive(true);
            userRepository.save(activeUser);

            User inactiveUser = createAndSaveUser("Petr", "Petrov", "petr@example.com");
            inactiveUser.setActive(false);
            userRepository.save(inactiveUser);

            mockMvc.perform(get("/api/v1/users")
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].active").value(true));
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/users/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("should successfully update user")
        void shouldUpdateUser_Success() throws Exception {
            User existingUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Petr")
                    .surname("Petrov")
                    .birthDate(LocalDate.of(1991, 2, 2))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", existingUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(existingUser.getId()))
                    .andExpect(jsonPath("$.name").value("Petr"))
                    .andExpect(jsonPath("$.surname").value("Petrov"));

            User updatedUser = userRepository.findById(existingUser.getId()).orElseThrow();
            assertThat(updatedUser.getName()).isEqualTo("Petr");
            assertThat(updatedUser.getSurname()).isEqualTo("Petrov");
        }

        @Test
        @DisplayName("should clear cache after updating")
        void  shouldClearCacheAfterUpdating() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId()));

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Petr")
                    .surname("Petrov")
                    .birthDate(user.getBirthDate())
                    .email(user.getEmail())
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)));

            mockMvc.perform(get("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Petr"));
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/users/{id}")
    class DeleteUserTests {

        @Test
        @DisplayName("should successfully delete user")
        void shouldDeleteUser_Success() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(delete("/api/v1/users/{id}", user.getId()))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 404 when dont find user to delete")
        void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
            mockMvc.perform(delete("/api/v1/users/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
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
