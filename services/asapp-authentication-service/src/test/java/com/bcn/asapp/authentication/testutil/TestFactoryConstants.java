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

package com.bcn.asapp.authentication.testutil;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.user.Role.USER;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralized test data constants and utilities shared across all test factories.
 *
 * @since 0.2.0
 */
final class TestFactoryConstants {

    // User constants
    static final String DEFAULT_USERNAME = "user@asapp.com";

    static final String DEFAULT_PASSWORD = "TEST@09_password?!";

    static final String DEFAULT_SUBJECT = "user@asapp.com";

    static final String DEFAULT_ROLE = USER.name();

    // JWT constants
    static final Long EXPIRATION_TIME_MILLIS = 300000L; // 5 minutes

    static final Long TOKEN_EXPIRED_OFFSET_MILLIS = 60000L; // 1 minute

    static final Map<String, Object> DEFAULT_ACCESS_TOKEN_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, DEFAULT_ROLE);

    static final Map<String, Object> DEFAULT_REFRESH_TOKEN_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, DEFAULT_ROLE);

    static final Instant DEFAULT_ISSUED = Instant.parse("2025-01-01T10:00:00Z");

    /**
     * Generate a random issue-at timestamp between now and 4.5 minutes before now.
     * <p>
     * This utility ensures tokens are issued within a realistic timeframe relative to their expiration time.
     *
     * @return random Instant between (now - 4.5 minutes) and now
     */
    static Instant generateRandomIssueAt() {
        var now = Instant.now();
        var fourMinsAgo = Instant.now()
                                 .minusMillis(EXPIRATION_TIME_MILLIS - 30000);
        var startMillis = fourMinsAgo.toEpochMilli();
        var endMillis = now.toEpochMilli();
        var randomMillis = ThreadLocalRandom.current()
                                            .nextLong(startMillis, endMillis + 1);
        return Instant.ofEpochMilli(randomMillis);
    }

    private TestFactoryConstants() {
        throw new AssertionError("Cannot instantiate utility class");
    }

}
