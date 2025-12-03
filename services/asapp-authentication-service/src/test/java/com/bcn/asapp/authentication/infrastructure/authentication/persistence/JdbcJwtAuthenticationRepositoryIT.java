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
        void DoesNotFindJwtAuthenticationAndReturnsEmptyOptional_JwtAuthenticationNotExists() {
            // When
            var accessToken = defaultTestEncodedAccessToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByAccessTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void FindsJwtAuthenticationAndReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .buildJdbcEntity();
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
            var accessToken = defaultTestEncodedRefreshToken();

            var actualJwtAuthentication = jwtAuthenticationRepository.findByRefreshTokenToken(accessToken);

            // Then
            assertThat(actualJwtAuthentication).isEmpty();
        }

        @Test
        void FindsJwtAuthenticationAndReturnsJwtAuthentication_JwtAuthenticationExists() {
            // Given
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                  .buildJdbcEntity();
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
            var user = defaultTestJdbcUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            var jwtAuthentication1 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .buildJdbcEntity();
            var jwtAuthentication2 = testJwtAuthenticationBuilder().withUserId(userCreated.id())
                                                                   .buildJdbcEntity();
            var jwtAuthenticationCreated1 = jwtAuthenticationRepository.save(jwtAuthentication1);
            var jwtAuthenticationCreated2 = jwtAuthenticationRepository.save(jwtAuthentication2);
            assertThat(jwtAuthenticationCreated1).isNotNull();
            assertThat(jwtAuthenticationCreated2).isNotNull();

            // When
            var userId = userCreated.id();

            jwtAuthenticationRepository.deleteAllJwtAuthenticationByUserId(userId);

            // Then
            var expectedJwtAuthentication1 = jwtAuthenticationRepository.findByAccessTokenToken(jwtAuthenticationCreated1.accessToken()
                                                                                                                         .token());
            assertThat(expectedJwtAuthentication1).isEmpty();
            var expectedJwtAuthentication2 = jwtAuthenticationRepository.findByAccessTokenToken(jwtAuthenticationCreated2.accessToken()
                                                                                                                         .token());
            assertThat(expectedJwtAuthentication2).isEmpty();
        }

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
