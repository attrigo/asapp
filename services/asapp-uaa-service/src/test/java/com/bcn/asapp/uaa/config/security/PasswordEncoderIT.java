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
package com.bcn.asapp.uaa.config.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.config.SecurityConfiguration;
import com.bcn.asapp.uaa.security.authentication.verifier.JwtVerifier;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.uaa.security.web.JwtAuthenticationFilter;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
@ContextConfiguration(classes = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
class PasswordEncoderIT {

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.fakePassword = "TEST PASSWORD";
    }

    @Test
    @DisplayName("GIVEN password is Bcrypt encoded WHEN matches passwords THEN password matches")
    void BcryptEncodedPassword_PasswordEncoderMatches_PasswordMatches() {
        // Given
        var bcryptEncoder = new BCryptPasswordEncoder();
        var fakePasswordBcryptEncoded = "{bcrypt}" + bcryptEncoder.encode(fakePassword);

        // When
        var rawPassword = fakePassword;

        var actual = passwordEncoder.matches(rawPassword, fakePasswordBcryptEncoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN password is Argon2 encoded WHEN matches passwords THEN password matches")
    void Argon2EncodedPassword_PasswordEncoderMatches_PasswordMatches() {
        // Given
        var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordArgon2Encoded = "{argon2@SpringSecurity_v5_8}" + argon2Encoder.encode(fakePassword);

        // When
        var rawPassword = fakePassword;

        var actual = passwordEncoder.matches(rawPassword, fakePasswordArgon2Encoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN password Pbkdf2 encoded WHEN matches passwords THEN password matches")
    void Pbkdf2EncodedPassword_PasswordEncoderMatches_PasswordMatches() {
        // Given
        var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordPbkdf2Encoded = "{pbkdf2@SpringSecurity_v5_8}" + pbkdf2Encoder.encode(fakePassword);

        // When
        var rawPassword = fakePassword;

        var actual = passwordEncoder.matches(rawPassword, fakePasswordPbkdf2Encoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN password Scrypt encoded WHEN matches passwords THEN password matches")
    void ScryptEncodedPassword_PasswordEncoderMatches_PasswordMatches() {
        // Given
        var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordScryptEncoded = "{scrypt@SpringSecurity_v5_8}" + scryptEncoder.encode(fakePassword);

        // When
        var rawPassword = fakePassword;

        var actual = passwordEncoder.matches(rawPassword, fakePasswordScryptEncoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN password is Noop encoded WHEN matches passwords THEN password matches")
    void NoopEncodedPassword_PasswordEncoderMatches_PasswordMatches() {
        // Given
        var fakePasswordNoopEncoded = "{noop}TEST PASSWORD";

        // When
        var rawPassword = fakePassword;

        var actual = passwordEncoder.matches(rawPassword, fakePasswordNoopEncoded);

        // Then
        assertTrue(actual);
    }

}
