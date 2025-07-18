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
package com.bcn.asapp.uaa.security.authentication.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.testutil.JwtFaker;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@ExtendWith(SpringExtension.class)
class RefreshTokenMatcherTests {

    @Mock
    private UserRepository userRepositoryMock;

    @Mock
    private RefreshTokenRepository refreshTokenRepositoryMock;

    @InjectMocks
    private RefreshTokenSessionMatcher refreshTokenSessionMatcher;

    private JwtFaker jwtFaker;

    private UUID fakeUserId;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();

        this.fakeUserId = UUID.randomUUID();
        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class Match {

        @Test
        @DisplayName("Given refresh token username not exists WHEN match a refresh token THEN throws UsernameNotFoundException")
        void RefreshTokenUsernameNotExists_Match_ThrowsUsernameNotFoundException() {
            // Given
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.empty());

            // When
            var decodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            Executable executable = () -> refreshTokenSessionMatcher.match(decodedJwt);

            // Then
            var exceptionThrown = assertThrows(UsernameNotFoundException.class, executable);
            assertEquals("User not found with username: " + fakeUsername, exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("Given refresh token not matches WHEN match a refresh token THEN returns false")
        void RefreshTokenNotMatches_Match_ReturnsFalse() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));

            given(refreshTokenRepositoryMock.existsByUserIdAndJwt(any(UUID.class), anyString())).willReturn(false);

            // When
            var decodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            var actualMatch = refreshTokenSessionMatcher.match(decodedJwt);

            // Then
            assertFalse(actualMatch);
        }

        @Test
        @DisplayName("Given refresh token matches WHEN match a refresh token THEN returns true")
        void RefreshTokenMatches_Match_ReturnsTrue() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));

            given(refreshTokenRepositoryMock.existsByUserIdAndJwt(any(UUID.class), anyString())).willReturn(true);

            // When
            var decodedJwt = jwtFaker.fakeDecodedJwt(jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN));

            var actualMatch = refreshTokenSessionMatcher.match(decodedJwt);

            // Then
            assertTrue(actualMatch);
        }

    }

}
