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

package com.bcn.asapp.tasks.domain.task;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates {@link Task} instances from raw primitive values.
 * <p>
 * Constructs domain value objects internally and delegates to {@link Task}'s package-private factory methods, keeping primitive-to-value-object translation in
 * one place.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class TaskFactory {

    private TaskFactory() {}

    /**
     * Creates a new task from raw values without a persistent ID.
     *
     * @param userId      the user's unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @return a new {@link Task} instance
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static Task create(UUID userId, String title, String description, Instant startDate, Instant endDate) {
        var userIdVO = UserId.of(userId);
        var titleVO = Title.of(title);
        var descriptionVO = Description.ofNullable(description);
        var startDateVO = StartDate.ofNullable(startDate);
        var endDateVO = EndDate.ofNullable(endDate);

        return Task.create(userIdVO, titleVO, descriptionVO, startDateVO, endDateVO);
    }

    /**
     * Reconstitutes a task from raw values with a persistent ID.
     *
     * @param id          the task's unique identifier
     * @param userId      the user's unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @return a reconstituted {@link Task} instance
     * @throws IllegalArgumentException if any required parameter is invalid
     */
    public static Task reconstitute(UUID id, UUID userId, String title, String description, Instant startDate, Instant endDate) {
        var idVO = TaskId.of(id);
        var userIdVO = UserId.of(userId);
        var titleVO = Title.of(title);
        var descriptionVO = Description.ofNullable(description);
        var startDateVO = StartDate.ofNullable(startDate);
        var endDateVO = EndDate.ofNullable(endDate);

        return Task.reconstitute(idVO, userIdVO, titleVO, descriptionVO, startDateVO, endDateVO);
    }

}
