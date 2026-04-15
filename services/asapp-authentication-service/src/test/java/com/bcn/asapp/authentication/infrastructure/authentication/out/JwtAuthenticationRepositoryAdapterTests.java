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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJdbcJwtAuthentication;
import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.anAuthenticatedJwtAuthentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;

/**
 * Tests {@link JwtAuthenticationRepositoryAdapter} domain exception translation and persistence error handling.
 * <p>
 * Coverage:
 * <li>Authentication lookup by access token returns the authentication when found</li>
 * <li>Authentication lookup by access token throws domain exception when not found</li>
 * <li>Authentication lookup by refresh token returns the authentication when found</li>
 * <li>Authentication lookup by refresh token throws domain exception when not found</li>
 * <li>Authentication deletion by ID completes without error when database operation succeeds</li>
 * <li>Authentication deletion by ID translates database failures to persistence exception</li>
 * <li>Bulk authentication deletion by user ID completes without error when database operation succeeds</li>
 * <li>Bulk authentication deletion by user ID translates database failures to persistence exception</li>
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationRepositoryAdapterTests {

    @Mock
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Mock
    private JwtAuthenticationMapper jwtAuthenticationMapper;

    @InjectMocks
    private JwtAuthenticationRepositoryAdapter jwtAuthenticationRepositoryAdapter;

    @Nested
    class FindByAccessToken {

        @Test
        void ReturnsAuthentication_AccessTokenFound() {
            // Given
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var accessToken = jwtAuthentication.accessToken()
                                               .encodedToken();
            var jwtAuthenticationJdbEntity = aJdbcJwtAuthentication();

            given(jwtAuthenticationRepository.findByAccessTokenToken(accessToken.value())).willReturn(Optional.of(jwtAuthenticationJdbEntity));
            given(jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationJdbEntity)).willReturn(jwtAuthentication);

            // When
            var actual = jwtAuthenticationRepositoryAdapter.findByAccessToken(accessToken);

            // Then
            assertThat(actual).isEqualTo(jwtAuthentication);

            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessTokenToken(accessToken.value());
            then(jwtAuthenticationMapper).should(times(1))
                                         .toJwtAuthentication(jwtAuthenticationJdbEntity);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_AccessTokenNotFound() {
            // Given
            var accessToken = EncodedToken.of("access.token.value");

            given(jwtAuthenticationRepository.findByAccessTokenToken(accessToken.value())).willReturn(Optional.empty());

            // When
            var actual = catchThrowable(() -> jwtAuthenticationRepositoryAdapter.findByAccessToken(accessToken));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in repository for access token");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessTokenToken(accessToken.value());
        }

    }

    @Nested
    class FindByRefreshToken {

        @Test
        void ReturnsAuthentication_RefreshTokenFound() {
            // Given
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var refreshToken = jwtAuthentication.refreshToken()
                                                .encodedToken();
            var jwtAuthenticationJdbEntity = aJdbcJwtAuthentication();

            given(jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken.value())).willReturn(Optional.of(jwtAuthenticationJdbEntity));
            given(jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationJdbEntity)).willReturn(jwtAuthentication);

            // When
            var actual = jwtAuthenticationRepositoryAdapter.findByRefreshToken(refreshToken);

            // Then
            assertThat(actual).isEqualTo(jwtAuthentication);

            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshTokenToken(refreshToken.value());
            then(jwtAuthenticationMapper).should(times(1))
                                         .toJwtAuthentication(jwtAuthenticationJdbEntity);
        }

        @Test
        void ThrowsAuthenticationNotFoundException_RefreshTokenNotFound() {
            // Given
            var refreshToken = EncodedToken.of("refresh.token.value");

            given(jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken.value())).willReturn(Optional.empty());

            // When
            var actual = catchThrowable(() -> jwtAuthenticationRepositoryAdapter.findByRefreshToken(refreshToken));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in repository for refresh token");

            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshTokenToken(refreshToken.value());
        }

    }

    @Nested
    class DeleteById {

        @Test
        void DeletesAuthentication_DatabaseOperationSucceeds() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("b2c3d4e5-f6a7-4890-b1c2-d3e4f5a6b7c8"));

            // When & Then
            assertThatCode(() -> jwtAuthenticationRepositoryAdapter.deleteById(jwtAuthenticationId)).doesNotThrowAnyException();

            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthenticationId.value());
        }

        @Test
        void ThrowsAuthenticationPersistenceException_DatabaseOperationFails() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("b2c3d4e5-f6a7-4890-b1c2-d3e4f5a6b7c8"));

            willThrow(new DataRetrievalFailureException("Database error")).given(jwtAuthenticationRepository)
                                                                          .deleteById(jwtAuthenticationId.value());

            // When
            var actual = catchThrowable(() -> jwtAuthenticationRepositoryAdapter.deleteById(jwtAuthenticationId));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessage("Could not delete authentication from repository")
                              .hasCauseInstanceOf(DataRetrievalFailureException.class);

            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthenticationId.value());
        }

    }

    @Nested
    class DeleteAllByUserId {

        @Test
        void DeletesAllAuthentications_DatabaseOperationSucceeds() {
            // Given
            var userId = UserId.of(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"));

            // When & Then
            assertThatCode(() -> jwtAuthenticationRepositoryAdapter.deleteAllByUserId(userId)).doesNotThrowAnyException();

            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId.value());
        }

        @Test
        void ThrowsAuthenticationPersistenceException_DatabaseOperationFails() {
            // Given
            var userId = UserId.of(UUID.fromString("a1b2c3d4-e5f6-4789-a0b1-c2d3e4f5a6b7"));

            willThrow(new DataRetrievalFailureException("Database error")).given(jwtAuthenticationRepository)
                                                                          .deleteAllByUserId(userId.value());

            // When
            var actual = catchThrowable(() -> jwtAuthenticationRepositoryAdapter.deleteAllByUserId(userId));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessage("Could not delete authentications for user from repository")
                              .hasCauseInstanceOf(DataRetrievalFailureException.class);

            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteAllByUserId(userId.value());
        }

    }

}
