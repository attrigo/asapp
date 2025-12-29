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

import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.user.UserPersistenceException;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.JwtType;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTests {

    @Mock
    private JwtStore jwtStore;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @InjectMocks
    private DeleteUserService deleteUserService;

    private final UUID userIdValue = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final String usernameValue = "user@asapp.com";

    private final Role role = USER;

    @Nested
    class DeleteUserById {

        @Test
        void ThrowsTokenStoreException_DeactivateTokensFails() {
            // Given
            var userId = UserId.of(userIdValue);
            var authentication1 = createJwtAuthentication("token1.access", "token1.refresh");
            var authentication2 = createJwtAuthentication("token2.access", "token2.refresh");
            var jwtPair1 = authentication1.getJwtPair();
            var jwtPair2 = authentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(authentication1, authentication2));
            willThrow(new TokenStoreException("Could not delete tokens from fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .delete(jwtPair1);

            // When
            var thrown = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(TokenStoreException.class)
                              .hasMessageContaining("Could not delete tokens from fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(times(1))
                          .delete(jwtPair1);
            then(jwtAuthenticationRepository).should(never())
                                             .deleteAllByUserId(any(UserId.class));
            then(userRepository).should(never())
                                .deleteById(any(UserId.class));
        }

        @Test
        void ThrowsAuthenticationPersistenceException_DeleteAuthenticationsFailsAndCompensationSucceeds() {
            // Given
            var userId = UserId.of(userIdValue);
            var authentication1 = createJwtAuthentication("token1.access", "token1.refresh");
            var authentication2 = createJwtAuthentication("token2.access", "token2.refresh");
            var jwtPair1 = authentication1.getJwtPair();
            var jwtPair2 = authentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(authentication1, authentication2));
            willThrow(new AuthenticationPersistenceException("Could not delete authentications for user from repository",
                    new RuntimeException("Repository delete failed"))).given(jwtAuthenticationRepository)
                                                                      .deleteAllByUserId(userId);

            // When
            var thrown = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessageContaining("Could not delete authentications for user from repository");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(times(1))
                          .delete(jwtPair1);
            then(jwtStore).should(times(1))
                          .delete(jwtPair2);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(never())
                                .deleteById(any(UserId.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair1);
            then(jwtStore).should(times(1))
                          .save(jwtPair2);
        }

        @Test
        void ThrowsUserPersistenceException_DeleteUserFailsAndCompensationSucceeds() {
            // Given
            var userId = UserId.of(userIdValue);
            var authentication1 = createJwtAuthentication("token1.access", "token1.refresh");
            var authentication2 = createJwtAuthentication("token2.access", "token2.refresh");
            var jwtPair1 = authentication1.getJwtPair();
            var jwtPair2 = authentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(authentication1, authentication2));
            willThrow(new UserPersistenceException("Could not delete user from repository", new RuntimeException("Repository delete failed"))).given(
                    userRepository)
                                                                                                                                              .deleteById(
                                                                                                                                                      userId);

            // When
            var thrown = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(UserPersistenceException.class)
                              .hasMessageContaining("Could not delete user from repository");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(times(1))
                          .delete(jwtPair1);
            then(jwtStore).should(times(1))
                          .delete(jwtPair2);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(jwtStore).should(times(1))
                          .save(jwtPair1);
            then(jwtStore).should(times(1))
                          .save(jwtPair2);
        }

        @Test
        void ThrowsCompensatingTransactionException_RepositoryFailsAndCompensationFails() {
            // Given
            var userId = UserId.of(userIdValue);
            var authentication1 = createJwtAuthentication("token1.access", "token1.refresh");
            var authentication2 = createJwtAuthentication("token2.access", "token2.refresh");
            var jwtPair1 = authentication1.getJwtPair();
            var jwtPair2 = authentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(authentication1, authentication2));
            willThrow(new AuthenticationPersistenceException("Could not delete authentications for user from repository",
                    new RuntimeException("Repository delete failed"))).given(jwtAuthenticationRepository)
                                                                      .deleteAllByUserId(userId);
            willThrow(new RuntimeException("Compensation failed")).given(jwtStore)
                                                                  .save(jwtPair1);

            // When
            var thrown = catchThrowable(() -> deleteUserService.deleteUserById(userIdValue));

            // Then
            assertThat(thrown).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessageContaining("Failed to compensate token deactivation after repository deletion failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(times(1))
                          .delete(jwtPair1);
            then(jwtStore).should(times(1))
                          .delete(jwtPair2);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(never())
                                .deleteById(any(UserId.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair1);
        }

        @Test
        void DeletesUserAndReturnsTrue_UserHasNoAuthentications() {
            // Given
            var userId = UserId.of(userIdValue);

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(Collections.emptyList());
            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var result = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(result).isTrue();

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
        }

        @Test
        void DeletesUserAndReturnsTrue_UserHasMultipleAuthentications() {
            // Given
            var userId = UserId.of(userIdValue);
            var authentication1 = createJwtAuthentication("token1.access", "token1.refresh");
            var authentication2 = createJwtAuthentication("token2.access", "token2.refresh");
            var jwtPair1 = authentication1.getJwtPair();
            var jwtPair2 = authentication2.getJwtPair();

            given(jwtAuthenticationRepository.findAllByUserId(userId)).willReturn(List.of(authentication1, authentication2));
            given(userRepository.deleteById(userId)).willReturn(true);

            // When
            var result = deleteUserService.deleteUserById(userIdValue);

            // Then
            assertThat(result).isTrue();

            then(jwtAuthenticationRepository).should(times(1))
                                             .findAllByUserId(userId);
            then(jwtStore).should(times(1))
                          .delete(jwtPair1);
            then(jwtStore).should(times(1))
                          .delete(jwtPair2);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId);
            then(userRepository).should(times(1))
                                .deleteById(userId);
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

    }

    private JwtAuthentication createJwtAuthentication(String accessTokenValue, String refreshTokenValue) {
        var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
        var userId = UserId.of(userIdValue);
        var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
        var refreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
        var jwtPair = JwtPair.of(accessToken, refreshToken);

        return JwtAuthentication.authenticated(authenticationId, userId, jwtPair);
    }

    private Jwt createJwt(JwtType type, String tokenValue) {
        var encodedToken = EncodedToken.of(tokenValue);
        var subject = Subject.of(usernameValue);
        var claims = JwtClaims.of("role", role.name(), "token_use", type == JwtType.ACCESS_TOKEN ? "access" : "refresh");
        var issued = Issued.of(Instant.now());
        var expiration = Expiration.of(issued, 300000L);

        return Jwt.of(encodedToken, type, subject, claims, issued, expiration);
    }

}
