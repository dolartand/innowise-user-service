package com.innowise.userservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.userservice.dto.user.UserRequestDto;
import com.innowise.userservice.dto.user.UserResponseDto;
import com.innowise.userservice.entity.User;
import com.innowise.userservice.repository.UserRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("UserController integration tests")
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
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "ivan@example.com")
                            .header("X-User-Role", "USER")
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
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "ivan@example.com")
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        @DisplayName("should return 409 when email exists")
        void shouldReturn409WhenEmailExists() throws Exception {
            User existingUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            UserRequestDto requestDto = UserRequestDto.builder()
                    .name("John")
                    .surname("Doe")
                    .birthDate(LocalDate.of(2005, 12, 22))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(post("/api/v1/users")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", "1")
                            .header("X-User-Email", "ivan@example.com")
                            .header("X-User-Role", "USER")
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
        @DisplayName("should return user by id when authenticated as owner")
        void shouldGetUserById_WhenAuthenticatedAsOwner() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.name").value("Ivan"))
                    .andExpect(jsonPath("$.surname").value("Ivanov"))
                    .andExpect(jsonPath("$.email").value("ivan@example.com"));
        }

        @Test
        @DisplayName("should return user by id when authenticated as admin")
        void shouldGetUserById_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.name").value("Ivan"));
        }

        @Test
        @DisplayName("should return 403 when user tries to get another user's data")
        void shouldReturn403_WhenUserTriesToGetAnotherUserData() throws Exception {
            User user1 = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "Petrov", "petr@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user2.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user1.getId().toString())
                            .header("X-User-Email", user1.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 401 when no authentication headers provided")
        void shouldReturn401_WhenNoAuthentication() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when user doesn't exist")
        void shouldReturnNotFound_WhenUserNotExists() throws Exception {
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN"))  // Админ может смотреть любого
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("User not found")));
        }

        @Test
        @DisplayName("should use cache on second request")
        void shouldUseCache_OnSecondRequest() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk());

            userRepository.deleteById(user.getId());

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ivan"));

            var cache = cacheManager.getCache("user");
            assertThat(cache).isNotNull();
            assertThat(cache.get(user.getId())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("should successfully return list of users when authenticated as admin")
        void shouldGetAllUsers_WhenAuthenticatedAsAdmin() throws Exception {
            createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            createAndSaveUser("Petr", "Petrov", "petr@example.com");
            createAndSaveUser("Sidor", "Sidorov", "sidor@example.com");

            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(4))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should return 403 when regular user tries to get all users")
        void shouldReturn403_WhenRegularUserTriesToGetAllUsers() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should filter users by name when authenticated as admin")
        void shouldFilterUsersByName_WhenAuthenticatedAsAdmin() throws Exception {
            createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            createAndSaveUser("Petr", "Petrov", "petr@example.com");

            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .param("name", "Ivan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Ivan"));
        }

        @Test
        @DisplayName("should filter users by activity when authenticated as admin")
        void shouldFilterUsersByActive_WhenAuthenticatedAsAdmin() throws Exception {
            User activeUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            activeUser.setActive(true);
            userRepository.save(activeUser);

            User inactiveUser = createAndSaveUser("Petr", "Petrov", "petr@example.com");
            inactiveUser.setActive(false);
            userRepository.save(inactiveUser);

            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(get("/api/v1/users")
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .param("active", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[?(@.email == 'ivan@example.com')].active").value(true));
        }
    }

    @Nested
    @DisplayName("Test PUT /api/v1/users/{id}")
    class UpdateUserTests {

        @Test
        @DisplayName("should successfully update user when authenticated as owner")
        void shouldUpdateUser_WhenAuthenticatedAsOwner() throws Exception {
            User existingUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Petr")
                    .surname("Petrov")
                    .birthDate(LocalDate.of(1991, 2, 2))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", existingUser.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", existingUser.getId().toString())
                            .header("X-User-Email", existingUser.getEmail())
                            .header("X-User-Role", "USER")
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
        @DisplayName("should successfully update user when authenticated as admin")
        void shouldUpdateUser_WhenAuthenticatedAsAdmin() throws Exception {
            User existingUser = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Petr")
                    .surname("Petrov")
                    .birthDate(LocalDate.of(1991, 2, 2))
                    .email("ivan@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", existingUser.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Petr"));
        }

        @Test
        @DisplayName("should return 403 when user tries to update another user")
        void shouldReturn403_WhenUserTriesToUpdateAnotherUser() throws Exception {
            User user1 = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "Petrov", "petr@example.com");

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Updated")
                    .surname("Name")
                    .birthDate(LocalDate.of(1991, 2, 2))
                    .email("petr@example.com")
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", user2.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user1.getId().toString())
                            .header("X-User-Email", user1.getEmail())
                            .header("X-User-Role", "USER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should clear cache after updating")
        void shouldClearCacheAfterUpdating() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                    .header("X-Service-Key", TEST_SERVICE_KEY)
                    .header("X-User-Id", user.getId().toString())
                    .header("X-User-Email", user.getEmail())
                    .header("X-User-Role", "USER"));

            UserRequestDto updateDto = UserRequestDto.builder()
                    .name("Petr")
                    .surname("Petrov")
                    .birthDate(user.getBirthDate())
                    .email(user.getEmail())
                    .active(true)
                    .build();

            mockMvc.perform(put("/api/v1/users/{id}", user.getId())
                    .header("X-Service-Key", TEST_SERVICE_KEY)
                    .header("X-User-Id", user.getId().toString())
                    .header("X-User-Email", user.getEmail())
                    .header("X-User-Role", "USER")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateDto)));

            mockMvc.perform(get("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Petr"));
        }
    }

    @Nested
    @DisplayName("Test DELETE /api/v1/users/{id}")
    class DeleteUserTests {

        @Test
        @DisplayName("should successfully delete user when authenticated as admin")
        void shouldDeleteUser_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(delete("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNoContent());

            assertThat(userRepository.findById(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to delete user")
        void shouldReturn403_WhenRegularUserTriesToDeleteUser() throws Exception {
            User user1 = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "Petrov", "petr@example.com");

            mockMvc.perform(delete("/api/v1/users/{id}", user2.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user1.getId().toString())
                            .header("X-User-Email", user1.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 403 when user tries to delete themselves")
        void shouldReturn403_WhenUserTriesToDeleteThemselves() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(delete("/api/v1/users/{id}", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user.getId().toString())
                            .header("X-User-Email", user.getEmail())
                            .header("X-User-Role", "USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when user doesnt exist")
        void shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(delete("/api/v1/users/{id}", 999L)
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Test PATCH /api/v1/users/{id}/activity")
    class ChangeUserActivityTests {

        @Test
        @DisplayName("should activate user when authenticated as admin")
        void shouldActivateUser_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            user.setActive(false);
            userRepository.save(user);

            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(patch("/api/v1/users/{id}/activity", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .param("isActive", "true"))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getActive()).isTrue();
        }

        @Test
        @DisplayName("should deactivate user when authenticated as admin")
        void shouldDeactivateUser_WhenAuthenticatedAsAdmin() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User admin = createAndSaveUser("Admin", "Admin", "admin@example.com");

            mockMvc.perform(patch("/api/v1/users/{id}/activity", user.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", admin.getId().toString())
                            .header("X-User-Email", admin.getEmail())
                            .header("X-User-Role", "ADMIN")
                            .param("isActive", "false"))
                    .andExpect(status().isOk());

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getActive()).isFalse();
        }

        @Test
        @DisplayName("should return 403 when regular user tries to change activity")
        void shouldReturn403_WhenRegularUserTriesToChangeActivity() throws Exception {
            User user1 = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");
            User user2 = createAndSaveUser("Petr", "Petrov", "petr@example.com");

            mockMvc.perform(patch("/api/v1/users/{id}/activity", user2.getId())
                            .header("X-Service-Key", TEST_SERVICE_KEY)
                            .header("X-User-Id", user1.getId().toString())
                            .header("X-User-Email", user1.getEmail())
                            .header("X-User-Role", "USER")
                            .param("isActive", "false"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Test GET /api/v1/users/by-email/{email}")
    class GetUserByEmailTests {

        @Test
        @DisplayName("should return user by email without authentication headers")
        void shouldGetUserByEmail_WithoutAuthentication() throws Exception {
            User user = createAndSaveUser("Ivan", "Ivanov", "ivan@example.com");

            mockMvc.perform(get("/api/v1/users/by-email/{email}", user.getEmail())
                            .header("X-Service-Key", TEST_SERVICE_KEY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId()))
                    .andExpect(jsonPath("$.email").value("ivan@example.com"))
                    .andExpect(jsonPath("$.name").value("Ivan"))
                    .andExpect(jsonPath("$.surname").value("Ivanov"));
        }

        @Test
        @DisplayName("should return 404 when user by email doesnt exists")
        void shouldReturnNotFound_WhenUserByEmailNotExists() throws Exception {
            mockMvc.perform(get("/api/v1/users/by-email/{email}", "noone@example.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value(Matchers.containsString("User not found")));
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