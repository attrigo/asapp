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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Task} persistence states, field updates, and identity equality.
 * <p>
 * Coverage:
 * <li>Creates new task with user ID and title, optional description, start date, and end date, null ID</li>
 * <li>Creates reconstituted task with ID, user ID, and title, optional description, start date, and end date</li>
 * <li>Updates task data (user ID, title, description, start date, end date) on both states</li>
 * <li>Validates user ID and title required for both states</li>
 * <li>Validates ID required only for reconstituted state</li>
 * <li>Implements identity-based equality using ID for reconstituted tasks, unique hash for new tasks</li>
 */
class TaskTests {

    @Nested
    class CreateNewTask {

        @Test
        void ReturnsNewTask_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.create(userId, title, description, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullDescription_NullDescription() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.create(userId, title, null, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isNull();
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullStartDate_NullStartDate() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.create(userId, title, description, null, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isNull();
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullEndDate_NullEndDate() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));

            // When
            var actual = Task.create(userId, title, description, startDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = catchThrowable(() -> Task.create(null, title, description, startDate, endDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = catchThrowable(() -> Task.create(userId, null, description, startDate, endDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

    }

    @Nested
    class CreateReconstitutedTask {

        @Test
        void ReturnsReconstitutedTask_ValidParameters() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullDescription_NullDescription() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.reconstitute(taskId, userId, title, null, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isNull();
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullStartDate_NullStartDate() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = Task.reconstitute(taskId, userId, title, description, null, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isNull();
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(endDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullEndDate_NullEndDate() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));

            // When
            var actual = Task.reconstitute(taskId, userId, title, description, startDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(title);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(description);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(startDate);
                softly.assertThat(actual.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = catchThrowable(() -> Task.reconstitute(null, userId, title, description, startDate, endDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = catchThrowable(() -> Task.reconstitute(taskId, null, title, description, startDate, endDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

            // When
            var actual = catchThrowable(() -> Task.reconstitute(taskId, userId, null, description, startDate, endDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

    }

    @Nested
    class UpdateTaskData {

        @Test
        void UpdatesAllFields_ValidParametersOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isNull();
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullDescriptionOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, null, newStartDate, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isNull();
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isNull();
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullStartDateOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, null, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isNull();
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isNull();
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullEndDateOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isNull();
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_ValidParametersOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullDescriptionOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, null, newStartDate, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isNull();
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullStartDateOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, null, newEndDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isNull();
                softly.assertThat(task.getEndDate()).as("end date").isEqualTo(newEndDate);
                // @formatter:on
            });
        }

        @Test
        void UpdatesAllFields_NullEndDateOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(task.getId()).as("ID").isEqualTo(taskId);
                softly.assertThat(task.getUserId()).as("user ID").isEqualTo(newUserId);
                softly.assertThat(task.getTitle()).as("title").isEqualTo(newTitle);
                softly.assertThat(task.getDescription()).as("description").isEqualTo(newDescription);
                softly.assertThat(task.getStartDate()).as("start date").isEqualTo(newStartDate);
                softly.assertThat(task.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserIdOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var actual = catchThrowable(() -> task.update(null, newTitle, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitleOnNewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var actual = catchThrowable(() -> task.update(newUserId, null, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserIdOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var actual = catchThrowable(() -> task.update(null, newTitle, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitleOnReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var actual = catchThrowable(() -> task.update(newUserId, null, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ReturnsTrue_SameObjectOtherTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(task);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsTrue_ThreeReconstitutedTasksSameId() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task1 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);
            var task3 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.equals(task2);
            var actual2 = task2.equals(task3);
            var actual3 = task1.equals(task3);

            // Then
            assertThat(actual1).isTrue();
            assertThat(actual2).isTrue();
            assertThat(actual3).isTrue();
        }

        @Test
        void ReturnsFalse_NullOtherTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);
            var other = "not a task";

            // When
            var actual = task.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_NewTaskAndReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.equals(task2);
            var actual2 = task2.equals(task1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ReturnsFalse_ThreeReconstitutedTasksDifferentId() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var taskId1 = TaskId.of(UUID.fromString("e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b"));
            var taskId2 = TaskId.of(UUID.fromString("f2a3b4c5-d6e7-4f8a-9b0c-1d2e3f4a5b6c"));
            var taskId3 = TaskId.of(UUID.fromString("a3b4c5d6-e7f8-4a9b-0c1d-2e3f4a5b6c7d"));
            var task1 = Task.reconstitute(taskId1, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId2, userId, title, description, startDate, endDate);
            var task3 = Task.reconstitute(taskId3, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.equals(task2);
            var actual2 = task2.equals(task3);
            var actual3 = task1.equals(task3);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
            assertThat(actual3).isFalse();
        }

    }

    @Nested
    class HashCode {

        @Test
        void ReturnsSameHashCode_TwoReconstitutedTasksSameId() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task1 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoNewTasks() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_NewTaskAndReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoReconstitutedTasksDifferentId() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var taskId1 = TaskId.of(UUID.fromString("b4c5d6e7-f8a9-4b0c-1d2e-3f4a5b6c7d8e"));
            var taskId2 = TaskId.of(UUID.fromString("c5d6e7f8-a9b0-4c1d-2e3f-4a5b6c7d8e9f"));
            var task1 = Task.reconstitute(taskId1, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(taskId2, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

    }

    @Nested
    class GetId {

        @Test
        void ReturnsId_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getId();

            // Then
            assertThat(actual).isEqualTo(taskId);
        }

        @Test
        void ReturnsNull_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getId();

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ReturnsUserId_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ReturnsUserId_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

    }

    @Nested
    class GetTitle {

        @Test
        void ReturnsTitle_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getTitle();

            // Then
            assertThat(actual).isEqualTo(title);
        }

        @Test
        void ReturnsTitle_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getTitle();

            // Then
            assertThat(actual).isEqualTo(title);
        }

    }

    @Nested
    class GetDescription {

        @Test
        void ReturnsDescription_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getDescription();

            // Then
            assertThat(actual).isEqualTo(description);
        }

        @Test
        void ReturnsDescription_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getDescription();

            // Then
            assertThat(actual).isEqualTo(description);
        }

    }

    @Nested
    class GetStartDate {

        @Test
        void ReturnsStartDate_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getStartDate();

            // Then
            assertThat(actual).isEqualTo(startDate);
        }

        @Test
        void ReturnsStartDate_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getStartDate();

            // Then
            assertThat(actual).isEqualTo(startDate);
        }

    }

    @Nested
    class GetEndDate {

        @Test
        void ReturnsEndDate_NewTask() {
            // Given
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

        @Test
        void ReturnsEndDate_ReconstitutedTask() {
            // Given
            var taskId = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));
            var userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));
            var title = Title.of("Title");
            var description = Description.of("Description");
            var startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));
            var endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));
            var task = Task.reconstitute(taskId, userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

    }

}
