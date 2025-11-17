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

package com.bcn.asapp.authentication.infrastructure.config.cleanup;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
 * @since 0.3.0
 * @author attrigo
 */
@Component
@ConditionalOnProperty(name = "asapp.cleanup.jwt.enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredJwtCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpiredJwtCleanupScheduler.class);

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    private final CleanupConfigurationProperties cleanupProperties;

    /**
     * Constructs a new {@code ExpiredJwtCleanupScheduler}.
     *
     * @param jwtAuthenticationRepository the JWT authentication repository
     * @param cleanupProperties           the cleanup configuration properties
     */
    public ExpiredJwtCleanupScheduler(JwtAuthenticationRepository jwtAuthenticationRepository, CleanupConfigurationProperties cleanupProperties) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.cleanupProperties = cleanupProperties;
    }

    /**
     * Scheduled cleanup job that marks expired authentications and deletes old ones.
     * <p>
     * Executes in two steps: 1. Mark authentications as expired (set expiredAt timestamp) 2. Delete authentications expired beyond retention period
     */
    @Scheduled(cron = "${asapp.cleanup.jwt.cron:0 0 2 * * ?}")
    public void cleanupExpiredAuthentications() {
        logger.info("Starting JWT authentication cleanup job");

        // Step 1: Mark expired authentications
        var markedCount = jwtAuthenticationRepository.markExpiredAuthentications();
        logger.info("Marked {} authentications as expired", markedCount);

        // Step 2: Delete old expired authentications
        var retentionDays = cleanupProperties.getRetentionDays();
        var cutoffDate = Instant.now()
                                .minus(retentionDays, ChronoUnit.DAYS);
        var deletedCount = jwtAuthenticationRepository.deleteAuthenticationsExpiredBefore(cutoffDate);
        logger.info("Deleted {} authentications older than {} days", deletedCount, retentionDays);

        logger.info("JWT authentication cleanup job completed");
    }

}
