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

package com.bcn.asapp.tasks.testutil.fixture;

import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtClaimNames.TOKEN_USE;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Centralized test data constants and utilities shared across all test factories.
 *
 * @since 0.2.0
 */
final class TestFactoryConstants {

    // Task constants

    static final UUID DEFAULT_TASK_ID = UUID.fromString("c3d4e5f6-a7b8-4901-c2d3-e4f5a6b7c8d9");

    static final UUID DEFAULT_TASK_USER_ID = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

    static final String DEFAULT_TASK_TITLE = "Title";

    static final String DEFAULT_TASK_DESCRIPTION = "Description";

    static final Instant DEFAULT_TASK_START_DATE = Instant.parse("2025-01-01T10:00:00Z");

    static final Instant DEFAULT_TASK_END_DATE = Instant.parse("2025-01-02T10:00:00Z");

    // JWT constants

    static final String DEFAULT_SUBJECT = "user@asapp.com";

    static final String DEFAULT_ROLE = "USER";

    static final Map<String, Object> DEFAULT_ACCESS_TOKEN_CLAIMS = Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, DEFAULT_ROLE);

    static final Map<String, Object> DEFAULT_REFRESH_TOKEN_CLAIMS = Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, DEFAULT_ROLE);

    static final Long EXPIRATION_TIME_MILLIS = 300000L; // 5 minutes

    static final Long TOKEN_EXPIRED_OFFSET_MILLIS = 60000L; // 1 minute

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

    private TestFactoryConstants() {}

}
