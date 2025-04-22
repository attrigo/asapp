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
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtTokenProvider.class })
@TestPropertySource(locations = "classpath:application.properties")
@ExtendWith(SpringExtension.class)
class PasswordEncoderIT {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.fakePassword = "TEST PASSWORD";
    }

    @Test
    @DisplayName("GIVEN username password has been stored with Bcrypt encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void UserPasswordHasBcryptEncode_MatchPasswordWithPasswordEncoder_PasswordMatches() {
        var bcryptEncoder = new BCryptPasswordEncoder();
        var fakePasswordBcryptEncoded = "{bcrypt}" + bcryptEncoder.encode(fakePassword);

        // When
        var actual = passwordEncoder.matches(fakePassword, fakePasswordBcryptEncoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN username password has been stored with Argon2 encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void UserPasswordHasArgon2Encode_MatchPasswordWithPasswordEncoder_PasswordMatches() {
        // Given
        var argon2Encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordArgon2Encoded = "{argon2@SpringSecurity_v5_8}" + argon2Encoder.encode(fakePassword);

        // When
        var actual = passwordEncoder.matches(fakePassword, fakePasswordArgon2Encoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN username password has been stored with Pbkdf2 encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void UserPasswordHasPbkdf2Encode_MatchPasswordWithPasswordEncoder_PasswordMatches() {
        // Given
        var pbkdf2Encoder = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordPbkdf2Encoded = "{pbkdf2@SpringSecurity_v5_8}" + pbkdf2Encoder.encode(fakePassword);

        // When
        var actual = passwordEncoder.matches(fakePassword, fakePasswordPbkdf2Encoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN username password has been stored with Scrypt encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void UserPasswordHasScryptEncode_MatchPasswordWithPasswordEncoder_PasswordMatches() {
        // Given
        var scryptEncoder = SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8();
        var fakePasswordScryptEncoded = "{scrypt@SpringSecurity_v5_8}" + scryptEncoder.encode(fakePassword);

        var actual = passwordEncoder.matches(fakePassword, fakePasswordScryptEncoded);

        // Then
        assertTrue(actual);
    }

    @Test
    @DisplayName("GIVEN username password has been stored without encoding WHEN login a user THEN returns HTTP response with status OK And the body with the generated authentication")
    void UserPasswordHasNoopEncode_MatchPasswordWithPasswordEncoder_PasswordMatches() {
        // Given
        var fakePasswordNoopEncoded = "{noop}TEST PASSWORD";

        // When
        var actual = passwordEncoder.matches(fakePassword, fakePasswordNoopEncoded);

        // Then
        assertTrue(actual);
    }

}
