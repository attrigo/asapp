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

package com.bcn.asapp.authentication.application.user.in.service;

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJwtAuthenticationBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.TokenStore;
import com.bcn.asapp.authentication.application.user.UserPersistenceException;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Tests {@link DeleteUserService} token deactivation and cascading deletion.
 * <p>
 * Coverage:
 * <li>DB deletion failures propagate without executing token deactivation</li>
 * <li>Token deactivation failures propagate after DB deletion has completed</li>
 * <li>Successful deletion cascades to authentication records and deactivates all user tokens</li>
 */
@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTests {

    @Mock
    private TokenStore tokenStore;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @InjectMocks
    private DeleteUserService deleteUserService;

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsTrue_UserExistsWithoutAuthentications() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of());
            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var actual = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(actual).isTrue();

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(tokenStore).should(never())
                            .delete(any(JwtPair.class));
        }

        @Test
        void ReturnsTrue_UserHasMultipleAuthentications() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var jwtAuthentication1 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token1.access", "token1.refresh")
                                                                .build();
            var jwtAuthentication2 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token2.access", "token2.refresh")
                                                                .build();
            var jwtPair1 = jwtAuthentication1.getJwtPair();
            var jwtPair2 = jwtAuthentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(jwtAuthentication1, jwtAuthentication2));
            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var actual = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(actual).isTrue();

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(tokenStore).should(times(1))
                            .delete(jwtPair1);
            then(tokenStore).should(times(1))
                            .delete(jwtPair2);
            then(tokenStore).should(never())
                            .save(any(JwtPair.class));
        }

        @Test
        void ReturnsFalse_UserNotExists() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of());
            given(userRepository.deleteById(userId)).willReturn(false);

            // When
            var actual = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(actual).isFalse();

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(tokenStore).should(never())
                            .delete(any(JwtPair.class));
            then(tokenStore).should(never())
                            .save(any(JwtPair.class));
        }

        @Test
        void ThrowsAuthenticationPersistenceException_AuthenticationsDeletionFails() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var jwtAuthentication1 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token1.access", "token1.refresh")
                                                                .build();
            var jwtAuthentication2 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token2.access", "token2.refresh")
                                                                .build();
            var authenticationPersistenceException = new AuthenticationPersistenceException("Could not delete authentications for user from repository",
                    new RuntimeException("Repository delete failed"));

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(jwtAuthentication1, jwtAuthentication2));
            willThrow(authenticationPersistenceException).given(jwtAuthenticationRepository)
                                                         .deleteAllByUserId(userId);

            // When
            var actual = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessage("Could not delete authentications for user from repository");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(never())
                                .deleteById(any(UserId.class));
            then(tokenStore).should(never())
                            .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsUserPersistenceException_UserDeletionFails() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var jwtAuthentication1 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token1.access", "token1.refresh")
                                                                .build();
            var jwtAuthentication2 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token2.access", "token2.refresh")
                                                                .build();
            var userPersistenceException = new UserPersistenceException("Could not delete user from repository",
                    new RuntimeException("Repository delete failed"));

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(jwtAuthentication1, jwtAuthentication2));
            willThrow(userPersistenceException).given(userRepository)
                                               .deleteById(userId);

            // When
            var actual = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(UserPersistenceException.class)
                              .hasMessage("Could not delete user from repository");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(tokenStore).should(never())
                            .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_TokenDeactivationFails() {
            // Given
            var userIdValue = UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7");
            var userId = UserId.of(userIdValue);
            var jwtAuthentication1 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token1.access", "token1.refresh")
                                                                .build();
            var jwtAuthentication2 = aJwtAuthenticationBuilder().withUserId(userIdValue)
                                                                .withTokenValues("token2.access", "token2.refresh")
                                                                .build();
            var tokenStoreException = new TokenStoreException("Could not delete tokens from fast-access store",
                    new RuntimeException("Token store connection failed"));
            var jwtPair1 = jwtAuthentication1.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(jwtAuthentication1, jwtAuthentication2));
            given(userRepository.deleteById(userId)).willReturn(true);
            willThrow(tokenStoreException).given(tokenStore)
                                          .delete(jwtPair1);

            // When
            var actual = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not delete tokens from fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(tokenStore).should(times(1))
                            .delete(jwtPair1);
            then(tokenStore).should(never())
                            .save(any(JwtPair.class));
        }

    }

}
