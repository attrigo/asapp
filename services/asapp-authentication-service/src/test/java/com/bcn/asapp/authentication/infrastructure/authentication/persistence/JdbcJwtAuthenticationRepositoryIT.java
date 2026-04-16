/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.infrastructure.authentication.persistence;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationMother.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserMother.aJdbcUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link JdbcJwtAuthenticationRepository} CRUD operations and query strategies against PostgreSQL.
 * <p>
 * Coverage:
 * <li>Persists and retrieves authentication sessions by multiple identifiers (ID, access token, refresh token)</li>
 * <li>Queries authentications by user identifier and expiration criteria</li>
 * <li>Deletes authentication sessions with cascading cleanup</li>
 * <li>Tests actual database operations with TestContainers PostgreSQL</li>
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestContainerConfiguration.class, JacksonAutoConfiguration.class })
class JdbcJwtAuthenticationRepositoryIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class FindByAccessTokenToken {

        @Test
        void ReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);
            var accessToken = createdJwtAuthentication.accessToken()
                                                      .token();

            // When
            var actual = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actual).isNotEmpty();
            assertJwtAuthentication(actual.get(), createdUser);
        }

        @Test
        void ReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // Given
            var encodedAccessToken = encodedAccessToken();

            // When
            var actual = jwtAuthenticationRepository.findByAccessTokenToken(encodedAccessToken);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class FindByRefreshTokenToken {

        @Test
        void ReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);
            var refreshToken = createdJwtAuthentication.refreshToken()
                                                       .token();

            // When
            var actual = jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken);

            // Then
            assertThat(actual).isNotEmpty();
            assertJwtAuthentication(actual.get(), createdUser);
        }

        @Test
        void ReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();

            // When
            var actual = jwtAuthenticationRepository.findByRefreshTokenToken(encodedRefreshToken);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class FindAllByUserId {

        @Test
        void ReturnsJwtAuthentications_JwtAuthenticationsExistForUserId() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var createdJwtAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication2 = createJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication3 = createJwtAuthenticationForUser(createdUser);

            // When
            var actual = jwtAuthenticationRepository.findAllByUserId(userId);

            // Then
            assertThat(actual).hasSize(3)
                              .containsExactlyInAnyOrder(createdJwtAuthentication1, createdJwtAuthentication2, createdJwtAuthentication3);
        }

        @Test
        void ReturnsEmptyList_JwtAuthenticationsNotExistForUserId() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();

            // When
            var actual = jwtAuthenticationRepository.findAllByUserId(userId);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class DeleteAllByUserId {

        @Test
        void DeletesUserJwtAuthentications_JwtAuthenticationsExistForUserId() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();
            var createdJwtAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication2 = createJwtAuthenticationForUser(createdUser);

            // When
            jwtAuthenticationRepository.deleteAllByUserId(userId);

            // Then
            var deletedJwtAuthentication1 = jwtAuthenticationRepository.findByAccessTokenToken(createdJwtAuthentication1.accessToken()
                                                                                                                        .token());
            var deletedJwtAuthentication2 = jwtAuthenticationRepository.findByAccessTokenToken(createdJwtAuthentication2.accessToken()
                                                                                                                        .token());
            assertThat(deletedJwtAuthentication1).isEmpty();
            assertThat(deletedJwtAuthentication2).isEmpty();
        }

    }

    @Nested
    class DeleteAllByRefreshTokenExpiredBefore {

        @Test
        void ReturnsDeletionCount_ExpiredAndActiveJwtAuthenticationsExist() {
            // Given
            var expiredBefore = Instant.now();
            var createdUser = createUser();
            var createdExpiredJwtAuthentication = createExpiredJwtAuthenticationForUser(createdUser);
            var createdActiveJwtAuthentication = createJwtAuthenticationForUser(createdUser);

            // When
            var actual = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var deletedExpiredJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(createdExpiredJwtAuthentication.accessToken()
                                                                                                                                    .token());
            var deletedActiveJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(createdActiveJwtAuthentication.accessToken()
                                                                                                                                  .token());
            assertThat(actual).isEqualTo(1);
            assertThat(deletedExpiredJwtAuthentication).isEmpty();
            assertThat(deletedActiveJwtAuthentication).isNotEmpty();
        }

        @Test
        void ReturnsDeletionCount_RefreshTokenExpired() {
            // Given
            var expiredBefore = Instant.now();
            var createdUser = createUser();
            var createdJwtAuthentication1 = createExpiredJwtAuthenticationForUser(createdUser);
            var createdJwtAuthentication2 = createExpiredJwtAuthenticationForUser(createdUser);

            // When
            var actual = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var deletedJwtAuthentication1 = jwtAuthenticationRepository.findByAccessTokenToken(createdJwtAuthentication1.accessToken()
                                                                                                                        .token());
            var deletedJwtAuthentication2 = jwtAuthenticationRepository.findByAccessTokenToken(createdJwtAuthentication2.accessToken()
                                                                                                                        .token());
            assertThat(actual).isEqualTo(2);
            assertThat(deletedJwtAuthentication1).isEmpty();
            assertThat(deletedJwtAuthentication2).isEmpty();
        }

        @Test
        void ReturnsZero_ActiveJwtAuthenticationsExist() {
            // Given
            var expiredBefore = Instant.now();
            var createdUser = createUser();
            var createdJwtAuthentication = createJwtAuthenticationForUser(createdUser);

            // When
            var actual = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var deletedJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(createdJwtAuthentication.accessToken()
                                                                                                                      .token());
            assertThat(actual).isZero();
            assertThat(deletedJwtAuthentication).isNotEmpty();
        }

        @Test
        void ReturnsZero_JwtAuthenticationsNotExist() {
            // Given
            var expiredBefore = Instant.now();

            // When
            var actual = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            assertThat(actual).isZero();
        }

    }

    // Test Data Creation Helpers

    private JdbcUserEntity createUser() {
        var user = aJdbcUser();
        var createdUser = userRepository.save(user);
        assertThat(createdUser).isNotNull();
        return createdUser;
    }

    private JdbcJwtAuthenticationEntity createJwtAuthenticationForUser(JdbcUserEntity user) {
        var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id())
                                                           .buildJdbc();
        return createJwtAuthentication(jwtAuthentication);
    }

    private JdbcJwtAuthenticationEntity createExpiredJwtAuthenticationForUser(JdbcUserEntity user) {
        var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id())
                                                           .withRefreshTokenExpired()
                                                           .buildJdbc();
        return createJwtAuthentication(jwtAuthentication);
    }

    private JdbcJwtAuthenticationEntity createJwtAuthentication(JdbcJwtAuthenticationEntity jwtAuthentication) {
        var createdJwtAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(createdJwtAuthentication).isNotNull();
        return createdJwtAuthentication;
    }

    // Assertions Helpers

    private void assertJwtAuthentication(JdbcJwtAuthenticationEntity actualJwtAuthentication, JdbcUserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();
        var accessToken = actualJwtAuthentication.accessToken();
        var refreshToken = actualJwtAuthentication.refreshToken();
        assertThatJwt(accessToken.token()).isNotNull()
                                          .isAccessToken()
                                          .hasSubject(expectedUser.username())
                                          .hasClaim(ROLE, expectedRoleName, String.class)
                                          .hasIssuedAt()
                                          .hasExpiration();
        assertThatJwt(refreshToken.token()).isNotNull()
                                           .isRefreshToken()
                                           .hasSubject(expectedUser.username())
                                           .hasClaim(ROLE, expectedRoleName, String.class)
                                           .hasIssuedAt()
                                           .hasExpiration();
    }

}
