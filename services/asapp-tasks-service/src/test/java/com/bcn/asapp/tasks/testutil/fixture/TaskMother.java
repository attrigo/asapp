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

package com.bcn.asapp.tasks.testutil.fixture;

import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_DESCRIPTION;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_END_DATE;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_ID;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_START_DATE;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_TITLE;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_TASK_USER_ID;

import java.time.Instant;
import java.util.UUID;

import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskFactory;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskEntity;

/**
 * Provides test data builders for Task domain entities and JdbcTaskEntity instances with fluent API.
 *
 * @since 0.2.0
 */
public final class TaskMother {

    private TaskMother() {}

    public static Task aTask() {
        return aTaskBuilder().build();
    }

    public static JdbcTaskEntity aJdbcTask() {
        return aTaskBuilder().buildJdbc();
    }

    public static Builder aTaskBuilder() {
        return new Builder();
    }

    public static class Builder {

        private UUID taskId;

        private UUID userId;

        private String title;

        private String description;

        private Instant startDate;

        private Instant endDate;

        Builder() {
            this.taskId = DEFAULT_TASK_ID;
            this.userId = DEFAULT_TASK_USER_ID;
            this.title = DEFAULT_TASK_TITLE;
            this.description = DEFAULT_TASK_DESCRIPTION;
            this.startDate = DEFAULT_TASK_START_DATE;
            this.endDate = DEFAULT_TASK_END_DATE;
        }

        public Builder withTaskId(UUID taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withStartDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder withEndDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }

        public Task build() {
            if (taskId == null) {
                return TaskFactory.create(userId, title, description, startDate, endDate);
            } else {
                return TaskFactory.reconstitute(taskId, userId, title, description, startDate, endDate);
            }
        }

        public JdbcTaskEntity buildJdbc() {
            return new JdbcTaskEntity(null, userId, title, description, startDate, endDate);
        }

    }

}
