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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.testutil.JwtAssertions.assertThatJwt;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.JwtAuthenticationDataFaker.fakeJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.RawJwtDataFaker.defaultFakeRawAccessToken;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.RawJwtDataFaker.defaultFakeRawRefreshToken;
import static com.bcn.asapp.authentication.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.authentication.out.entity.JwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.user.out.UserJdbcRepository;
import com.bcn.asapp.authentication.infrastructure.user.out.entity.UserEntity;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestContainerConfiguration.class, JacksonAutoConfiguration.class })
class JwtAuthenticationJdbcRepositoryIT {

    @Autowired
    private UserJdbcRepository userRepository;

    @Autowired
    private JwtAuthenticationJdbcRepository jwtAuthenticationRepository;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class FindByAccessTokenToken {

        @Test
        void DoesNotFindJwtAuthenticationAndReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // When
            var accessToken = defaultFakeRawAccessToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void FindsJwtAuthenticationAndReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

            // When
            var accessToken = jwtAuthenticationCreated.accessToken()
                                                      .token();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isNotEmpty();
            assertJwtAuthentication(actualJwtAuthentication.get(), userCreated);
        }

    }

    @Nested
    class FindByRefreshTokenToken {

        @Test
        void DoesNotFindJwtAuthenticationAndReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // When
            var accessToken = defaultFakeRawRefreshToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void FindsJwtAuthenticationAndReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .build();
            var jwtAuthenticationCreated = jwtAuthenticationRepository.save(jwtAuthentication);
            assertThat(jwtAuthenticationCreated).isNotNull();

            // When
            var refreshToken = jwtAuthenticationCreated.refreshToken()
                                                       .token();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken);

            // Then
            assertThat(actualJwtAuthentication).isNotEmpty();
            assertJwtAuthentication(actualJwtAuthentication.get(), userCreated);
        }

    }

    @Nested
    class DeleteAllByUserId {

        @Test
        void DeletesUserAuthentications_UserHasAuthentications() {
            // Given
            var user = defaultFakeUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var firstJwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                       .build();
            var secondJwtAuthentication = fakeJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                        .build();
            var firstJwtAuthenticationCreated = jwtAuthenticationRepository.save(firstJwtAuthentication);
            var secondJwtAuthenticationCreated = jwtAuthenticationRepository.save(secondJwtAuthentication);
            assertThat(firstJwtAuthenticationCreated).isNotNull();
            assertThat(secondJwtAuthenticationCreated).isNotNull();

            // When
            var userId = userCreated.id();

            jwtAuthenticationRepository.deleteAllJwtAuthenticationByUserId(userId);

            // Then
            var actualFirstJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(firstJwtAuthenticationCreated.accessToken()
                                                                                                                               .token());
            assertThat(actualFirstJwtAuthentication).isEmpty();
            var actualSecondJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(secondJwtAuthenticationCreated.accessToken()
                                                                                                                                 .token());
            assertThat(actualSecondJwtAuthentication).isEmpty();
        }

    }

    private void assertJwtAuthentication(JwtAuthenticationEntity actualJwtAuthentication, UserEntity expectedUser) {
        var expectedRoleName = expectedUser.role();
        var accessToken = actualJwtAuthentication.accessToken();
        var refreshToken = actualJwtAuthentication.refreshToken();

        SoftAssertions.assertSoftly(softAssertions -> {
            assertThatJwt(accessToken.token()).isNotNull()
                                              .isAccessToken()
                                              .hasSubject(expectedUser.username())
                                              .hasClaim(ROLE_CLAIM_NAME, expectedRoleName, String.class)
                                              .hasIssuedAt()
                                              .hasExpiration();
            assertThatJwt(refreshToken.token()).isNotNull()
                                               .isRefreshToken()
                                               .hasSubject(expectedUser.username())
                                               .hasClaim(ROLE_CLAIM_NAME, expectedRoleName, String.class)
                                               .hasIssuedAt()
                                               .hasExpiration();
        });
    }

}
