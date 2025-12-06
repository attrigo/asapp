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

package com.bcn.asapp.authentication.infrastructure.security.scheduler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;

/**
 * Scheduled job for cleaning up expired JWT authentications from the database.
 * <p>
 * Runs daily to mark expired authentications and delete old ones beyond retention period.
 *
 * @author attrigo
 * @since 0.2.0
 */
@Component
@ConditionalOnProperty(name = "asapp.security.jwt.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredJwtCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredJwtCleanupScheduler.class);

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code ExpiredJwtCleanupScheduler}.
     *
     * @param jwtAuthenticationRepository the JWT authentication repository
     */
    public ExpiredJwtCleanupScheduler(JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Scheduled cleanup job that deletes expired authentications.
     * <p>
     * Default schedule: Daily at 2:00 AM (cron expression: {@code 0 0 2 * * ?}).
     * <p>
     * Configure via {@code asapp.security.jwt.cleanup.cron-expression} property.
     */
    @Scheduled(cron = "${asapp.security.jwt.cleanup.cron-expression:0 0 2 * * ?}")
    public void cleanupExpiredAuthentications() {
        logger.info("Starting JWT authentication cleanup job");

        var expiredBefore = Instant.now();
        var deletedCount = jwtAuthenticationRepository.deleteAllByRefreshTokenExpiredBefore(expiredBefore);
        logger.info("Deleted {} expired authentications", deletedCount);

        logger.info("JWT authentication cleanup job completed");
    }

}
