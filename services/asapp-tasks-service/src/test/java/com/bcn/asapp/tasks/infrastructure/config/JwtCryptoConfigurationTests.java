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

package com.bcn.asapp.tasks.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Base64;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.crypto.MACVerifier;

/**
 * Tests {@link JwtCryptoConfiguration} JWT cryptographic bean creation.
 * <p>
 * Coverage:
 * <li>Creates MACVerifier bean from a valid secret key</li>
 * <li>Rejects a secret key too short for verification</li>
 */
class JwtCryptoConfigurationTests {

    private static final String VALID_SECRET = Base64.getEncoder()
                                                     .encodeToString(new byte[32]);

    private static final String SHORT_SECRET = Base64.getEncoder()
                                                     .encodeToString("short_secret".getBytes());

    private final JwtCryptoConfiguration configuration = new JwtCryptoConfiguration();

    @Nested
    class MacVerifier {

        @Test
        void ReturnsMacVerifier_ValidSecret() {
            // When
            var actual = configuration.macVerifier(VALID_SECRET);

            // Then
            assertThat(actual).isInstanceOf(MACVerifier.class);
        }

        @Test
        void ThrowsIllegalStateException_ShortSecret() {
            // When
            var actual = catchThrowable(() -> configuration.macVerifier(SHORT_SECRET));

            // Then
            assertThat(actual).isInstanceOf(IllegalStateException.class)
                              .hasMessageContaining("Invalid JWT secret key:");
        }

    }

}
