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

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtPairStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;

@ExtendWith(MockitoExtension.class)
class DefaultJwtAuthenticationRevokerTests {

    @Mock
    private JwtPairStore jwtPairStore;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @InjectMocks
    private DefaultJwtAuthenticationRevoker defaultAuthenticationRevoker;

    private final JwtAuthenticationId jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("4fea0a39-9df8-4c95-9673-518fe958fee9"));

    private JwtAuthentication jwtAuthentication;

    @BeforeEach
    void beforeEach() {
        var userId = UserId.of(UUID.fromString("14f8a75b-be5f-4fb3-b479-437f8ff0b092"));
        var token = EncodedToken.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0MyJ9.c2lnMw");
        var subject = Subject.of("user@asapp.com");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
        var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
        var expiration = Expiration.of(issued, 30000L);
        var accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        var refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);
        jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, accessToken, refreshToken);
    }

    @Nested
    class RevokeAuthentication {

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenDeletionFails() {
            // Given
            willThrow(new RuntimeException("Repository deletion exception")).given(jwtAuthenticationRepository)
                                                                            .deleteById(jwtAuthenticationId);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticationRevoker.revokeAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be revoked due to");

            then(jwtPairStore).should(times(1))
                              .delete(jwtAuthentication.getJwtPair());
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthenticationId);

        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenStoreDeleteFails() {
            // Given
            willThrow(new RuntimeException("Redis connection failed")).given(jwtPairStore)
                                                                      .delete(any(JwtPair.class));

            // When
            var thrown = catchThrowable(() -> defaultAuthenticationRevoker.revokeAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be revoked due to");

            then(jwtPairStore).should(times(1))
                              .delete(jwtAuthentication.getJwtPair());
            then(jwtAuthenticationRepository).should(never())
                                             .deleteById(jwtAuthenticationId);

        }

        @Test
        void ThenRevokesAuthentication_GivenIdIsValid() {
            // Given
            willDoNothing().given(jwtAuthenticationRepository)
                           .deleteById(jwtAuthenticationId);

            // When
            defaultAuthenticationRevoker.revokeAuthentication(jwtAuthentication);

            // Then
            then(jwtPairStore).should(times(1))
                              .delete(jwtAuthentication.getJwtPair());
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthenticationId);
        }

    }

}
