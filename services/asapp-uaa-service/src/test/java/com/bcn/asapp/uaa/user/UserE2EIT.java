/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.bcn.asapp.uaa.user;

import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.uaa.UserRestAPIURL.USERS_UPDATE_BY_ID_FULL_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.AsappUAAServiceApplication;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.testconfig.SecurityTestConfiguration;
import com.bcn.asapp.uaa.testutil.JwtFaker;

@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true)
@Import(SecurityTestConfiguration.class)
@SpringBootTest(classes = AsappUAAServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class UserE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtFaker jwtFaker;

    private User authUser;

    private String bearerToken;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private String fakeUserEncodedPassword;

    private Role fakeUserRole;

    @BeforeEach
    void beforeEach() {
        accessTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        var fakeAuthUser = new User(null, "TEST USERNAME", "{bcrypt}$2a$10$rEmV9yiyLvVkeE8F5I/MiOzaiIvHXuMgraBRRa9SIm11UW7o18jUm", Role.USER);
        authUser = userRepository.save(fakeAuthUser);
        assertNotNull(authUser);

        var fakeAccessTokenAsString = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
        var fakeAccessToken = new AccessToken(null, authUser.id(), fakeAccessTokenAsString, Instant.now(), Instant.now());
        var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
        assertNotNull(accessTokenSaved);

        var fakeRefreshTokenAsString = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
        var fakeRefreshToken = new com.bcn.asapp.uaa.security.core.RefreshToken(null, authUser.id(), fakeRefreshTokenAsString, Instant.now(), Instant.now());
        var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
        assertNotNull(refreshTokenSaved);

        this.bearerToken = "Bearer " + fakeAccessTokenAsString;

        this.fakeUserUsername = "TEST USERNAME 0";
        this.fakeUserPassword = "TEST PASSWORD 0";
        this.fakeUserEncodedPassword = "{bcrypt}$2a$10$rEmV9yiyLvVkeE8F5I/MiOzaiIvHXuMgraBRRa9SIm11UW7o18jUm";
        this.fakeUserRole = Role.USER;
    }

    @Nested
    class GetUserById {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN get a user by id THEN does not get the user And returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_GetUserById_DoesNotGetTheUserAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToFind = UUID.randomUUID();

            webTestClient.get()
                         .uri(USERS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN get a user by id THEN gets the user And returns HTTP response with status OK And the body with the user found")
        void UserIdExists_GetUserById_GetsUserAndReturnsStatusOKAndBodyWithUserFound() {
            // Given
            var fakeUserToFind = new User(null, fakeUserUsername, fakeUserEncodedPassword, Role.USER);
            var userToFindSaved = userRepository.save(fakeUserToFind);
            assertNotNull(userToFindSaved);

            // When & Then
            var idToFind = userToFindSaved.id();

            var expectedUser = new UserDTO(idToFind, fakeUserUsername, "********", fakeUserRole.name());

            webTestClient.get()
                         .uri(USERS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(UserDTO.class)
                         .isEqualTo(expectedUser);
        }

    }

    @Nested
    class GetAllUsers {

        @Test
        @DisplayName("GIVEN there are users WHEN get all users THEN gets all users And returns HTTP response with status OK And the body with the users found")
        void ThereAreUsers_GetAllUsers_GetsAllUsersAndReturnsStatusOKAndBodyWithUsersFound() {
            var fakeUserUsername1 = fakeUserUsername + "1";
            var fakeUserUsername2 = fakeUserUsername + "2";
            var fakeUserUsername3 = fakeUserUsername + "3";
            var fakeUserPassword1 = fakeUserPassword + "1";
            var fakeUserPassword2 = fakeUserPassword + "2";
            var fakeUserPassword3 = fakeUserPassword + "3";

            // Given
            var fakeUserToFind1 = new User(null, fakeUserUsername1, fakeUserPassword1, fakeUserRole);
            var fakeUserToFind2 = new User(null, fakeUserUsername2, fakeUserPassword2, fakeUserRole);
            var fakeUserToFind3 = new User(null, fakeUserUsername3, fakeUserPassword3, fakeUserRole);
            var usersToFindSaved = userRepository.saveAll(List.of(fakeUserToFind1, fakeUserToFind2, fakeUserToFind3));
            var userIdsToFind = usersToFindSaved.stream()
                                                .map(User::id)
                                                .toList();
            assertNotNull(userIdsToFind);
            assertEquals(3L, userIdsToFind.size());

            // When & Then
            var expectedAuthUser = new UserDTO(authUser.id(), authUser.username(), "********", authUser.role()
                                                                                                       .name());
            var expectedUser1 = new UserDTO(userIdsToFind.getFirst(), fakeUserUsername1, "********", fakeUserRole.name());
            var expectedUser2 = new UserDTO(userIdsToFind.get(1), fakeUserUsername2, "********", fakeUserRole.name());
            var expectedUser3 = new UserDTO(userIdsToFind.getLast(), fakeUserUsername3, "********", fakeUserRole.name());
            var expectedUsers = List.of(expectedAuthUser, expectedUser1, expectedUser2, expectedUser3);

            webTestClient.get()
                         .uri(USERS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBodyList(UserDTO.class)
                         .hasSize(4)
                         .isEqualTo(expectedUsers);
        }

    }

    @Nested
    class CreateUser {

        @Test
        @DisplayName("GIVEN user has id field WHEN create a user THEN creates the user ignoring the given user id And returns HTTP response with status CREATED And the body with the user created")
        void UserHasIdField_CreateUser_CreatesUserIgnoringUserIdAndReturnsStatusCreatedAndBodyWithUserCreated() {
            var anotherFakeUserId = UUID.randomUUID();

            // When & Then
            String userToCreate = String.format("""
                    {
                        "id": "%s",
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }""", anotherFakeUserId, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var response = webTestClient.post()
                                        .uri(USERS_CREATE_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userToCreate)
                                        .exchange()
                                        .expectStatus()
                                        .isCreated()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UserDTO.class)
                                        .value(user -> assertThat(user.id(), allOf(not(anotherFakeUserId), notNullValue())))
                                        .value(user -> assertThat(user.username(), equalTo(fakeUserUsername)))
                                        .value(user -> assertThat(user.password(), equalTo("********")))
                                        .value(user -> assertThat(user.role(), equalTo(fakeUserRole.name())))
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
//            var expectedUser = new User(response.id(), response.username(), fakeUserEncodedPassword, Role.valueOf(response.role()));
            var optionalActualUser = userRepository.findById(response.id());
            assertTrue(optionalActualUser.isPresent());
            var actualUser = optionalActualUser.get();
            assertEquals(response.id(), actualUser.id());
            assertEquals(response.username(), actualUser.username());
            assertTrue(passwordEncoder.matches(fakeUserPassword, actualUser.password()));
            assertEquals(response.role(), actualUser.role()
                                                    .name());
        }

        @Test
        @DisplayName("GIVEN user fields are valid WHEN create a user THEN creates the user And returns HTTP response with status CREATED And the body with the user created")
        void UserFieldsAreValid_CreateUser_CreatesUserAndReturnsStatusCreatedAndBodyWithUserCreated() {
            // When & Then
            String userToCreate = String.format("""
                    {
                        "id": "",
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }""", fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            var response = webTestClient.post()
                                        .uri(USERS_CREATE_FULL_PATH)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userToCreate)
                                        .exchange()
                                        .expectStatus()
                                        .isCreated()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UserDTO.class)
                                        .value(user -> assertThat(user.id(), notNullValue()))
                                        .value(user -> assertThat(user.username(), equalTo(fakeUserUsername)))
                                        .value(user -> assertThat(user.password(), equalTo("********")))
                                        .value(user -> assertThat(user.role(), equalTo(fakeUserRole.name())))
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var optionalActualUser = userRepository.findById(response.id());
            assertTrue(optionalActualUser.isPresent());
            var actualUser = optionalActualUser.get();
            assertEquals(response.id(), actualUser.id());
            assertEquals(response.username(), actualUser.username());
            assertTrue(passwordEncoder.matches(fakeUserPassword, actualUser.password()));
            assertEquals(response.role(), actualUser.role()
                                                    .name());
        }

    }

    @Nested
    class UpdateUserById {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN update a user by id THEN does not update the user And returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_UpdateUserById_DoesNotUpdateTheUserAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToUpdate = UUID.randomUUID();
            var userToUpdate = new UserDTO(null, fakeUserUsername, fakeUserPassword, fakeUserRole.name());

            webTestClient.put()
                         .uri(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                         .bodyValue(userToUpdate)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN user id exists And new user data has id field WHEN update a user by id THEN updates all user fields except the id And returns HTTP response with status OK And the body with the user updated")
        void UserIdExistsAndNewUserDataHasIdField_UpdateUserById_UpdatesAllUserFieldsExceptIdAndReturnsStatusOkAndBodyWithUserUpdated() {
            var anotherFakeUserUsername = fakeUserUsername + "2";
            var anotherFakeUserPassword = fakeUserPassword + "2";
            var anotherFakeUserRole = Role.ADMIN.name();

            // Given
            var fakeUserToUpdate = new User(null, fakeUserUsername, fakeUserEncodedPassword, Role.USER);
            var userToUpdateSaved = userRepository.save(fakeUserToUpdate);
            assertNotNull(userToUpdateSaved);

            // When & Then
            var idToUpdate = userToUpdateSaved.id();
            String userToUpdate = String.format("""
                    {
                        "id": "%s",
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }""", UUID.randomUUID(), anotherFakeUserUsername, anotherFakeUserPassword, anotherFakeUserRole);

            var expectedUserDTO = new UserDTO(idToUpdate, anotherFakeUserUsername, "********", anotherFakeUserRole);

            var response = webTestClient.put()
                                        .uri(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userToUpdate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UserDTO.class)
                                        .isEqualTo(expectedUserDTO)
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var optionalActualUser = userRepository.findById(response.id());
            assertTrue(optionalActualUser.isPresent());
            var actualUser = optionalActualUser.get();
            assertEquals(response.id(), actualUser.id());
            assertEquals(response.username(), actualUser.username());
            assertTrue(passwordEncoder.matches(anotherFakeUserPassword, actualUser.password()));
            assertEquals(response.role(), actualUser.role()
                                                    .name());
        }

        @Test
        @DisplayName("GIVEN user id exists And new user data fields are valid WHEN update a user THEN updates all user fields And returns HTTP response with status OK And the body with the user updated")
        void UserIdExistsAndNewUserDataFieldsAreValid_UpdateUser_UpdatesAllUserFieldsAndReturnsStatusOkAndBodyWithUserUpdated() {
            var anotherFakeUserUsername = fakeUserUsername + "2";
            var anotherFakeUserPassword = fakeUserPassword + "2";
            var anotherFakeUserRole = Role.ADMIN.name();

            // Given
            var fakeUserToUpdate = new User(null, fakeUserUsername, fakeUserEncodedPassword, Role.USER);
            var userToUpdateSaved = userRepository.save(fakeUserToUpdate);
            assertNotNull(userToUpdateSaved);

            // When & Then
            var idToUpdate = userToUpdateSaved.id();
            String userToUpdate = String.format("""
                    {
                        "id": "%s",
                        "username": "%s",
                        "password": "%s",
                        "role": "%s"
                    }""", UUID.randomUUID(), anotherFakeUserUsername, anotherFakeUserPassword, anotherFakeUserRole);

            var expectedUserDTO = new UserDTO(idToUpdate, anotherFakeUserUsername, "********", anotherFakeUserRole);

            var response = webTestClient.put()
                                        .uri(USERS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                        .header(HttpHeaders.AUTHORIZATION, bearerToken)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(userToUpdate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(UserDTO.class)
                                        .isEqualTo(expectedUserDTO)
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var optionalActualUser = userRepository.findById(response.id());
            assertTrue(optionalActualUser.isPresent());
            var actualUser = optionalActualUser.get();
            assertEquals(response.id(), actualUser.id());
            assertEquals(response.username(), actualUser.username());
            assertTrue(passwordEncoder.matches(anotherFakeUserPassword, actualUser.password()));
            assertEquals(response.role(), actualUser.role()
                                                    .name());
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN delete a user by id THEN does not delete the user And returns HTTP response with status NOT_FOUND And without body")
        void UserIdNotExists_DeleteUserById_DoesNotDeleteUserAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToDelete = UUID.randomUUID();

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, idToDelete)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN delete a user by id THEN deletes the user And returns HTTP response with status NO_CONTENT And without body")
        void UserIdExists_DeleteUserById_DeletesUserAndReturnsStatusNoContentAndWithoutBody() {
            // Given
            var fakeUserToDelete = new User(null, fakeUserUsername, fakeUserEncodedPassword, Role.USER);
            var userToDeleteSaved = userRepository.save(fakeUserToDelete);
            assertNotNull(userToDeleteSaved);

            // When & Then
            var idToDelete = userToDeleteSaved.id();

            webTestClient.delete()
                         .uri(USERS_DELETE_BY_ID_FULL_PATH, idToDelete)
                         .header(HttpHeaders.AUTHORIZATION, bearerToken)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNoContent()
                         .expectBody()
                         .isEmpty();

            assertFalse(userRepository.findById(userToDeleteSaved.id())
                                      .isPresent());
        }

    }

}
