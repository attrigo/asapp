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

import static com.bcn.asapp.authentication.testutil.TestFactory.TestJwtAuthenticationFactory.testJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestJdbcUser;
import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.testUserBuilder;
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
    void setUp() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class CleanupExpiredAuthentications {

        @Test
        void CompletesSuccessfully_ThereAreNotAuthentication() {
            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainingAuths = jwtAuthenticationRepository.findAll();
            assertThat(remainingAuths).isEmpty();
        }

        @Test
        void PreservesActiveAuthentications_ThereAreOnlyActiveAuthentications() {
            // Given
            var user = createDefaultUser();

            var activeJwtAuthentication1 = createDefaultJwtAuthentication(user);
            var activeJwtAuthentication2 = createDefaultJwtAuthentication(user);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainingAuths = jwtAuthenticationRepository.findAll();
            assertThat(remainingAuths).hasSize(2)
                                      .containsExactlyInAnyOrder(activeJwtAuthentication1, activeJwtAuthentication2);
        }

        @Test
        void DeletesOnlyExpiredAuthentications_ThereAreActiveAndExpiredAuthentications() {
            // Given
            var user = createDefaultUser();

            createExpiredJwtAuthentication(user);
            var activeJwtAuthentication = createDefaultJwtAuthentication(user);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainingAuths = jwtAuthenticationRepository.findAll();
            assertThat(remainingAuths).hasSize(1)
                                      .containsExactly(activeJwtAuthentication);
        }

        @Test
        void DeletesExpiredAuthentications_ThereAreExpiredAuthentications() {
            // Given
            var user = createDefaultUser();

            var expiredJwtAuthentication1 = createExpiredJwtAuthentication(user);
            var expiredJwtAuthentication2 = createExpiredJwtAuthentication(user);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainingAuths = jwtAuthenticationRepository.findAll();
            assertThat(remainingAuths).isEmpty();
        }

        @Test
        void DeletesMultipleExpiredAuthenticationsForDifferentUsers_MultipleUsersWithExpiredTokens() {
            // Given
            var user1 = testUserBuilder().withUsername("user1@asapp.com")
                                         .buildJdbcEntity();
            var user2 = testUserBuilder().withUsername("user2@asapp.com")
                                         .buildJdbcEntity();
            user1 = userRepository.save(user1);
            user2 = userRepository.save(user2);
            assertThat(user1).isNotNull();
            assertThat(user2).isNotNull();

            createExpiredJwtAuthentication(user1);
            createExpiredJwtAuthentication(user2);
            var activeJwtAuthentication = createDefaultJwtAuthentication(user1);

            // When
            expiredJwtCleanupScheduler.cleanupExpiredAuthentications();

            // Then
            var remainingAuths = jwtAuthenticationRepository.findAll();
            assertThat(remainingAuths).hasSize(1)
                                      .containsExactly(activeJwtAuthentication);
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

}
