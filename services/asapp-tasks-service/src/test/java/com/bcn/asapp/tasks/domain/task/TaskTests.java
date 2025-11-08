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

package com.bcn.asapp.tasks.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TaskTests {

    private final TaskId id = TaskId.of(UUID.fromString("d68ca3f3-c27f-4602-9679-64e4b871811d"));

    private final UserId userId = UserId.of(UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d"));

    private final Title title = Title.of("Title");

    private final Description description = Description.of("Description");

    private final StartDate startDate = StartDate.of(Instant.parse("2025-01-01T11:00:00Z"));

    private final EndDate endDate = EndDate.of(Instant.parse("2025-02-02T12:00:00Z"));

    @Nested
    class CreateNewTask {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNull() {
            // When
            var thrown = catchThrowable(() -> Task.create(null, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTitleIsNull() {
            // When
            var thrown = catchThrowable(() -> Task.create(userId, null, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ThenReturnsNewTaskWithNullDescription_GivenDescriptionIsNull() {
            // When
            var actual = Task.create(userId, title, null, startDate, endDate);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isNull();
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

        @Test
        void ThenReturnsNewTaskWithNullStartDate_GivenStartDateIsNull() {
            // When
            var actual = Task.create(userId, title, description, null, endDate);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isNull();
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

        @Test
        void ThenReturnsNewTaskWithNullEndDate_GivenEndDateIsNull() {
            // When
            var actual = Task.create(userId, title, description, startDate, null);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isNull();
        }

        @Test
        void ThenReturnsNewTask_GivenParametersAreValid() {
            // When
            var actual = Task.create(userId, title, description, startDate, endDate);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

    }

    @Nested
    class CreateReconstitutedTask {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(null, userId, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNull() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(id, null, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTitleIsNull() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(id, userId, null, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ThenReturnsReconstitutedTaskWithNullDescription_GivenDescriptionIsNull() {
            // When
            var actual = Task.reconstitute(id, userId, title, null, startDate, endDate);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isNull();
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

        @Test
        void ThenReturnsReconstitutedTaskWithNullStartDate_GivenStartDateIsNull() {
            // When
            var actual = Task.reconstitute(id, userId, title, description, null, endDate);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isNull();
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

        @Test
        void ThenReturnsReconstitutedTaskWithNullEndDate_GivenEndDateIsNull() {
            // When
            var actual = Task.reconstitute(id, userId, title, description, startDate, null);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isNull();
        }

        @Test
        void ThenReturnsReconstitutedTask_GivenParametersAreValid() {
            // When
            var actual = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getTitle()).isEqualTo(title);
            assertThat(actual.getDescription()).isEqualTo(description);
            assertThat(actual.getStartDate()).isEqualTo(startDate);
            assertThat(actual.getEndDate()).isEqualTo(endDate);
        }

    }

    @Nested
    class UpdateTaskData {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNullOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var thrown = catchThrowable(() -> task.update(null, newTitle, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTitleIsNullOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var thrown = catchThrowable(() -> task.update(newUserId, null, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ThenUpdatesAllFields_GivenDescriptionIsNullOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, null, newStartDate, newEndDate);

            // Then
            assertThat(task.getId()).isNull();
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isNull();
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

        @Test
        void ThenUpdatesAllFields_GivenStartDateIsNullOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, null, newEndDate);

            // Then
            assertThat(task.getId()).isNull();
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isNull();
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

        @Test
        void ThenUpdatesAllFields_GivenEndDateIsNullOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, null);

            // Then
            assertThat(task.getId()).isNull();
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isNull();
        }

        @Test
        void ThenUpdatesAllFields_GivenParametersAreValidOnNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);

            // Then
            assertThat(task.getId()).isNull();
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNullOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var thrown = catchThrowable(() -> task.update(null, newTitle, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTitleIsNullOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            var thrown = catchThrowable(() -> task.update(newUserId, null, newDescription, newStartDate, newEndDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ThenUpdatesAllFields_GivenDescriptionIsNullOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, null, newStartDate, newEndDate);

            // Then
            assertThat(task.getId()).isEqualTo(id);
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isNull();
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

        @Test
        void ThenUpdatesAllFields_GivenStartDateIsNullOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, null, newEndDate);

            // Then
            assertThat(task.getId()).isEqualTo(id);
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isNull();
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

        @Test
        void ThenUpdatesAllFields_GivenEndDateIsNullOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, null);

            // Then
            assertThat(task.getId()).isEqualTo(id);
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isNull();
        }

        @Test
        void ThenUpdatesAllFields_GivenParametersAreValidOnReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            var newUserId = UserId.of(UUID.fromString("d0e1f2a3-b4c5-4d6e-7f8a-9b0c1d2e3f4a"));
            var newTitle = Title.of("NewTitle");
            var newDescription = Description.of("NewDescription");
            var newStartDate = StartDate.of(Instant.parse("2025-03-03T13:00:00Z"));
            var newEndDate = EndDate.of(Instant.parse("2025-04-04T14:00:00Z"));

            // When
            task.update(newUserId, newTitle, newDescription, newStartDate, newEndDate);

            // Then
            assertThat(task.getId()).isEqualTo(id);
            assertThat(task.getUserId()).isEqualTo(newUserId);
            assertThat(task.getTitle()).isEqualTo(newTitle);
            assertThat(task.getDescription()).isEqualTo(newDescription);
            assertThat(task.getStartDate()).isEqualTo(newStartDate);
            assertThat(task.getEndDate()).isEqualTo(newEndDate);
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ThenReturnsFalse_GivenOtherTaskIsNull() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenOtherClassIsNotTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);
            var other = "not a task";

            // When
            var actual = task.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenOtherTaskIsSameObject() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(task);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenNewTaskAndReconstitutedTask() {
            // Given
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.equals(task2);
            var actual2 = task2.equals(task1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenThreeReconstitutedTasksWithSameId() {
            // Given
            var task1 = Task.reconstitute(id, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(id, userId, title, description, startDate, endDate);
            var task3 = Task.reconstitute(id, userId, title, description, startDate, endDate);

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
        void ThenReturnsFalse_GivenThreeReconstitutedTasksWithDifferentId() {
            // Given
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
        void ThenReturnsDifferentHashCode_GivenTwoNewTasks() {
            // Given
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenNewTaskAndReconstitutedTask() {
            // Given
            var task1 = Task.create(userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsSameHashCode_GivenTwoReconstitutedTasksWithSameId() {
            // Given
            var task1 = Task.reconstitute(id, userId, title, description, startDate, endDate);
            var task2 = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual1 = task1.hashCode();
            var actual2 = task2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoReconstitutedTasksWithDifferentId() {
            // Given
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
        void ThenReturnsNull_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsId_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ThenReturnsUserId_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ThenReturnsUserId_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

    }

    @Nested
    class GetTitle {

        @Test
        void ThenReturnsTitle_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getTitle();

            // Then
            assertThat(actual).isEqualTo(title);
        }

        @Test
        void ThenReturnsTitle_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getTitle();

            // Then
            assertThat(actual).isEqualTo(title);
        }

    }

    @Nested
    class GetDescription {

        @Test
        void ThenReturnsDescription_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getDescription();

            // Then
            assertThat(actual).isEqualTo(description);
        }

        @Test
        void ThenReturnsDescription_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getDescription();

            // Then
            assertThat(actual).isEqualTo(description);
        }

    }

    @Nested
    class GetStartDate {

        @Test
        void ThenReturnsStartDate_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getStartDate();

            // Then
            assertThat(actual).isEqualTo(startDate);
        }

        @Test
        void ThenReturnsStartDate_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getStartDate();

            // Then
            assertThat(actual).isEqualTo(startDate);
        }

    }

    @Nested
    class GetEndDate {

        @Test
        void ThenReturnsEndDate_GivenNewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

        @Test
        void ThenReturnsEndDate_GivenReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

    }

}
