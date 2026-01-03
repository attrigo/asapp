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
        void ThrowsIllegalArgumentException_NullUserId() {
            // When
            var thrown = catchThrowable(() -> Task.create(null, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // When
            var thrown = catchThrowable(() -> Task.create(userId, null, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ReturnsNewTaskWithNullDescription_NullDescription() {
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
        void ReturnsNewTaskWithNullStartDate_NullStartDate() {
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
        void ReturnsNewTaskWithNullEndDate_NullEndDate() {
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
        void ReturnsNewTask_ValidParameters() {
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
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(null, userId, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(id, null, title, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // When
            var thrown = catchThrowable(() -> Task.reconstitute(id, userId, null, description, startDate, endDate));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null");
        }

        @Test
        void ReturnsReconstitutedTaskWithNullDescription_NullDescription() {
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
        void ReturnsReconstitutedTaskWithNullStartDate_NullStartDate() {
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
        void ReturnsReconstitutedTaskWithNullEndDate_NullEndDate() {
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
        void ReturnsReconstitutedTask_ValidParameters() {
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
        void ThrowsIllegalArgumentException_NullUserIdOnNewTask() {
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
        void ThrowsIllegalArgumentException_NullTitleOnNewTask() {
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
        void UpdatesAllFields_NullDescriptionOnNewTask() {
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
        void UpdatesAllFields_NullStartDateOnNewTask() {
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
        void UpdatesAllFields_NullEndDateOnNewTask() {
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
        void UpdatesAllFields_ValidParametersOnNewTask() {
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
        void ThrowsIllegalArgumentException_NullUserIdOnReconstitutedTask() {
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
        void ThrowsIllegalArgumentException_NullTitleOnReconstitutedTask() {
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
        void UpdatesAllFields_NullDescriptionOnReconstitutedTask() {
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
        void UpdatesAllFields_NullStartDateOnReconstitutedTask() {
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
        void UpdatesAllFields_NullEndDateOnReconstitutedTask() {
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
        void UpdatesAllFields_ValidParametersOnReconstitutedTask() {
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
        void ReturnsFalse_NullOtherTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);
            var other = "not a task";

            // When
            var actual = task.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsTrue_SameObjectOtherTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.equals(task);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_NewTaskAndReconstitutedTask() {
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
        void ReturnsTrue_ThreeReconstitutedTasksSameId() {
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
        void ReturnsFalse_ThreeReconstitutedTasksDifferentId() {
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
        void ReturnsDifferentHashCode_TwoNewTasks() {
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
        void ReturnsDifferentHashCode_NewTaskAndReconstitutedTask() {
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
        void ReturnsSameHashCode_TwoReconstitutedTasksSameId() {
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
        void ReturnsDifferentHashCode_TwoReconstitutedTasksDifferentId() {
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
        void ReturnsNull_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ReturnsId_ReconstitutedTask() {
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
        void ReturnsUserId_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ReturnsUserId_ReconstitutedTask() {
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
        void ReturnsTitle_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getTitle();

            // Then
            assertThat(actual).isEqualTo(title);
        }

        @Test
        void ReturnsTitle_ReconstitutedTask() {
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
        void ReturnsDescription_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getDescription();

            // Then
            assertThat(actual).isEqualTo(description);
        }

        @Test
        void ReturnsDescription_ReconstitutedTask() {
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
        void ReturnsStartDate_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getStartDate();

            // Then
            assertThat(actual).isEqualTo(startDate);
        }

        @Test
        void ReturnsStartDate_ReconstitutedTask() {
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
        void ReturnsEndDate_NewTask() {
            // Given
            var task = Task.create(userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

        @Test
        void ReturnsEndDate_ReconstitutedTask() {
            // Given
            var task = Task.reconstitute(id, userId, title, description, startDate, endDate);

            // When
            var actual = task.getEndDate();

            // Then
            assertThat(actual).isEqualTo(endDate);
        }

    }

}
