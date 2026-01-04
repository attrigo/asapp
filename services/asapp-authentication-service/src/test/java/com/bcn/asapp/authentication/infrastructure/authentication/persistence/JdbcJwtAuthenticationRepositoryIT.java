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

package com.bcn.asapp.authentication.infrastructure.authentication.persistence;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedAccessToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestEncodedTokenFactory.defaultTestEncodedRefreshToken;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestJdbcUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

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
        void ReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // When
            var accessToken = defaultTestEncodedAccessToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void ReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = createDefaultUser();

            var previousJwtAuthentication = createDefaultJwtAuthentication(user);

            // When
            var accessToken = previousJwtAuthentication.accessToken()
                                                       .token();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isNotEmpty();
            assertJwtAuthentication(actualJwtAuthentication.get(), user);
        }

    }

    @Nested
    class FindByRefreshTokenToken {

        @Test
        void ReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // When
            var accessToken = defaultTestEncodedRefreshToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void ReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = createDefaultUser();

            var previousJwtAuthentication = createDefaultJwtAuthentication(user);

            // When
            var refreshToken = previousJwtAuthentication.refreshToken()
                                                        .token();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken);

            // Then
            assertThat(actualJwtAuthentication).isNotEmpty();
            assertJwtAuthentication(actualJwtAuthentication.get(), user);
        }

    }

    @Nested
    class FindAllByUserId {

        @Test
        void ReturnsEmptyList_JwtAuthenticationsNotExistForUser() {
            // Given
            var user = createDefaultUser();

            // When
            var userId = user.id();

            var actualJwtAuthentications = jwtAuthenticationRepository.findAllByUserId(userId);

            // Then
            assertThat(actualJwtAuthentications).isEmpty();
        }

        @Test
        void ReturnsListOfJwtAuthentications_MultipleUserJwtAuthenticationsExist() {
            // Given
            var user = createDefaultUser();

            var previousJwtAuthentication1 = createDefaultJwtAuthentication(user);
            var previousJwtAuthentication2 = createDefaultJwtAuthentication(user);
            var previousJwtAuthentication3 = createDefaultJwtAuthentication(user);

            // When
            var userId = user.id();

            var actualJwtAuthentications = jwtAuthenticationRepository.findAllByUserId(userId);

            // Then
            assertThat(actualJwtAuthentications).hasSize(3)
                                                .containsExactlyInAnyOrder(previousJwtAuthentication1, previousJwtAuthentication2, previousJwtAuthentication3);
        }

    }

    @Nested
    class DeleteAllByUserId {

        @Test
        void DeletesUserJwtAuthentications_UserJwtAuthenticationsExist() {
            // Given
            var user = createDefaultUser();

            var previousJwtAuthentication1 = createDefaultJwtAuthentication(user);
            var previousJwtAuthentication2 = createDefaultJwtAuthentication(user);

            // When
            var userId = user.id();

            jwtAuthenticationRepository.deleteAllByUserId(userId);

            // Then
            var actualPreviousJwtAuthentication1 = jwtAuthenticationRepository.findByAccessTokenToken(previousJwtAuthentication1.accessToken()
                                                                                                                                .token());
            var actualPreviousJwtAuthentication2 = jwtAuthenticationRepository.findByAccessTokenToken(previousJwtAuthentication2.accessToken()
                                                                                                                                .token());
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualPreviousJwtAuthentication1).isEmpty();
                assertThat(actualPreviousJwtAuthentication2).isEmpty();
            });
        }

    }

    @Nested
    class DeleteAllByRefreshTokenExpiredBefore {

        @Test
        void ReturnsZero_JwtAuthenticationsNotExist() {
            // When
            var expiredBefore = Instant.now();

            var actualDeleteCount = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            assertThat(actualDeleteCount).isEqualTo(0);
        }

        @Test
        void ReturnsZero_ActiveJwtAuthenticationsExist() {
            // Given
            var user = createDefaultUser();

            var activeJwtAuthentication = createDefaultJwtAuthentication(user);

            // When
            var expiredBefore = Instant.now();

            var actualDeleteCount = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var actualActiveJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(activeJwtAuthentication.accessToken()
                                                                                                                          .token());
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualDeleteCount).isEqualTo(0);
                assertThat(actualActiveJwtAuthentication).isNotEmpty();
            });
        }

        @Test
        void ReturnsAmountOfExpiredJwtAuthenticationsDeleted_ExpiredAndActiveJwtAuthenticationsExist() {
            // Given
            var user = createDefaultUser();

            var expiredJwtAuthentication = createExpiredJwtAuthentication(user);
            var validJwtAuthentication = createDefaultJwtAuthentication(user);

            // When
            var expiredBefore = Instant.now();

            var actualDeleteCount = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var actualExpiredJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(expiredJwtAuthentication.accessToken()
                                                                                                                            .token());
            var actualValidJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(validJwtAuthentication.accessToken()
                                                                                                                        .token());
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualDeleteCount).isEqualTo(1);
                assertThat(actualExpiredJwtAuthentication).isEmpty();
                assertThat(actualValidJwtAuthentication).isNotEmpty();
            });
        }

        @Test
        void ReturnsAmountOfExpiredJwtAuthenticationsDeleted__RefreshTokenExpired() {
            // Given
            var user = createDefaultUser();

            var expiredJwtAuthentication1 = createExpiredJwtAuthentication(user);
            var expiredJwtAuthentication2 = createExpiredJwtAuthentication(user);

            // When
            var expiredBefore = Instant.now();

            var actualDeleteCount = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);

            // Then
            var actualExpiredJwtAuthentication1 = jwtAuthenticationRepository.findByAccessTokenToken(expiredJwtAuthentication1.accessToken()
                                                                                                                              .token());
            var actualExpiredJwtAuthentication2 = jwtAuthenticationRepository.findByAccessTokenToken(expiredJwtAuthentication2.accessToken()
                                                                                                                              .token());
            SoftAssertions.assertSoftly(softAssertions -> {
                assertThat(actualDeleteCount).isEqualTo(2);
                assertThat(actualExpiredJwtAuthentication1).isEmpty();
                assertThat(actualExpiredJwtAuthentication2).isEmpty();
            });
        }

    }

    private JdbcUserEntity createDefaultUser() {
        var user = defaultTestJdbcUser();
        var userCreated = userRepository.save(user);
        assertThat(userCreated).isNotNull();

        return userCreated;
    }

    private JdbcJwtAuthenticationEntity createDefaultJwtAuthentication(JdbcUserEntity user) {
        var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(user.id())
                                                              .buildJdbcEntity();
        var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(jwtAuthenticationCreated).isNotNull();

        return jwtAuthenticationCreated;
    }

    private JdbcJwtAuthenticationEntity createExpiredJwtAuthentication(JdbcUserEntity user) {
        var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(user.id())
                                                              .withRefreshTokenExpired()
                                                              .buildJdbcEntity();
        var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(jwtAuthenticationCreated).isNotNull();

        return jwtAuthenticationCreated;
    }

    private void assertJwtAuthentication(JdbcJwtAuthenticationEntity actualJwtAuthentication, JdbcUserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();
        var accessToken = actualJwtAuthentication.accessToken();
        var refreshToken = actualJwtAuthentication.refreshToken();

        SoftAssertions.assertSoftly(softAssertions -> {
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
        });
    }

}
