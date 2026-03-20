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

package com.bcn.asapp.authentication.infrastructure.security.scheduler;

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aJdbcUser;
import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aUserBuilder;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link ExpiredJwtCleanupScheduler} expired authentication cleanup from database.
 * <p>
 * Coverage:
 * <li>Deletes authentications where refresh token expired before current time</li>
 * <li>Preserves authentications where refresh token not yet expired</li>
 * <li>Handles multiple expired authentications in single cleanup</li>
 * <li>Handles cleanup when no expired authentications exist</li>
 */
@SpringBootTest
@Import(TestContainerConfiguration.class)
class ExpiredJwtCleanupSchedulerIT {

    @Autowired
    private ExpiredJwtCleanupScheduler expiredJwtCleanupScheduler;

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
    class CleanupExpiredAuthentications {

        @Test
        void DeletesExpiredAuthentications_ExpiredAuthenticationsExist() {
            // Given
            var createdUser = createUser();
            createExpiredJwtAuthenticationForUser(createdUser);
            createExpiredJwtAuthenticationForUser(createdUser);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainAuthentications = jwtAuthenticationRepository.findAll();
            assertThat(remainAuthentications).isEmpty();
        }

        @Test
        void DeletesOnlyExpiredAuthentications_ActiveAndExpiredAuthenticationsExist() {
            // Given
            var createdUser = createUser();
            var createdActiveAuthentication = createJwtAuthenticationForUser(createdUser);
            createExpiredJwtAuthenticationForUser(createdUser);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainAuthentications = jwtAuthenticationRepository.findAll();
            assertThat(remainAuthentications).hasSize(1)
                                             .containsExactly(createdActiveAuthentication);
        }

        @Test
        void DeletesOnlyExpiredAuthentications_ActiveAndExpiredAuthenticationsExistForMultipleUsers() {
            // Given
            var user1 = aUserBuilder().withUsername("user1@asapp.com")
                                      .buildJdbc();
            var user2 = aUserBuilder().withUsername("user2@asapp.com")
                                      .buildJdbc();
            var createdUser1 = createUser(user1);
            var createdUser2 = createUser(user2);
            var createdActiveAuthentication = createJwtAuthenticationForUser(createdUser1);
            createExpiredJwtAuthenticationForUser(createdUser1);
            createExpiredJwtAuthenticationForUser(createdUser2);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainAuthentications = jwtAuthenticationRepository.findAll();
            assertThat(remainAuthentications).hasSize(1)
                                             .containsExactly(createdActiveAuthentication);
        }

        @Test
        void PreservesActiveAuthentications_OnlyActiveAuthenticationsExist() {
            // Given
            var createdUser = createUser();
            var createdAuthentication1 = createJwtAuthenticationForUser(createdUser);
            var createdAuthentication2 = createJwtAuthenticationForUser(createdUser);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var authentications = jwtAuthenticationRepository.findAll();
            assertThat(authentications).hasSize(2)
                                       .containsExactlyInAnyOrder(createdAuthentication1, createdAuthentication2);
        }

        @Test
        void DeletesExpiredAuthentications_AuthenticationsNotExist() {
            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var authentications = jwtAuthenticationRepository.findAll();
            assertThat(authentications).isEmpty();
        }

    }

    // Test Data Creation Helpers

    private JdbcUserEntity createUser() {
        var user = aJdbcUser();
        return createUser(user);
    }

    private JdbcUserEntity createUser(JdbcUserEntity user) {
        var createdUser = userRepository.save(user);
        assertThat(createdUser).isNotNull();
        return createdUser;
    }

    private JdbcJwtAuthenticationEntity createJwtAuthenticationForUser(JdbcUserEntity user) {
        var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id())
                                                           .buildJdbc();
        var createdJwtAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(createdJwtAuthentication).isNotNull();
        return createdJwtAuthentication;
    }

    private void createExpiredJwtAuthenticationForUser(JdbcUserEntity user) {
        var jwtAuthentication = aJwtAuthenticationBuilder().withUserId(user.id())
                                                           .withRefreshTokenExpired()
                                                           .buildJdbc();
        var createdJwtAuthentication = jwtAuthenticationRepository.save(jwtAuthentication);
        assertThat(createdJwtAuthentication).isNotNull();
    }

}
