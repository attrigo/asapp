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
 * Tests {@link TaskFactory} task creation from raw primitive values.
 * <p>
 * Coverage:
 * <li>Creates new task from primitives with null ID and correct field values</li>
 * <li>Creates new task accepting null for optional fields (description, start date, end date)</li>
 * <li>Reconstitutes task from primitives with ID and correct field values</li>
 * <li>Reconstitutes task accepting null for optional fields (description, start date, end date)</li>
 * <li>Rejects null user ID or null title during creation</li>
 * <li>Rejects null ID, null user ID, or null title during reconstitution</li>
 */
class TaskFactoryTests {

    @Nested
    class Create {

        @Test
        void ReturnsNewTask_ValidParameters() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedStartDate = StartDate.of(startDate);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.create(userId, title, description, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullDescription_NullDescription() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedStartDate = StartDate.of(startDate);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.create(userId, title, null, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isNull();
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullStartDate_NullStartDate() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.create(userId, title, description, null, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isNull();
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsNewTaskWithNullEndDate_NullEndDate() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedStartDate = StartDate.of(startDate);

            // When
            var actual = TaskFactory.create(userId, title, description, startDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // When
            var actual = catchThrowable(() -> TaskFactory.create(null, "Title", "Description", Instant.now(), Instant.now()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> TaskFactory.create(userId, null, "Description", Instant.now(), Instant.now()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

    }

    @Nested
    class Reconstitute {

        @Test
        void ReturnsReconstitutedTask_ValidParameters() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedId = TaskId.of(id);
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedStartDate = StartDate.of(startDate);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.reconstitute(id, userId, title, description, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(expectedId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullDescription_NullDescription() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedId = TaskId.of(id);
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedStartDate = StartDate.of(startDate);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.reconstitute(id, userId, title, null, startDate, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(expectedId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isNull();
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullStartDate_NullStartDate() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var endDate = Instant.parse("2025-02-02T12:00:00Z");
            var expectedId = TaskId.of(id);
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedEndDate = EndDate.of(endDate);

            // When
            var actual = TaskFactory.reconstitute(id, userId, title, description, null, endDate);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(expectedId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isNull();
                softly.assertThat(actual.getEndDate()).as("end date").isEqualTo(expectedEndDate);
                // @formatter:on
            });
        }

        @Test
        void ReturnsReconstitutedTaskWithNullEndDate_NullEndDate() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");
            var title = "Title";
            var description = "Description";
            var startDate = Instant.parse("2025-01-01T11:00:00Z");
            var expectedId = TaskId.of(id);
            var expectedUserId = UserId.of(userId);
            var expectedTitle = Title.of(title);
            var expectedDescription = Description.of(description);
            var expectedStartDate = StartDate.of(startDate);

            // When
            var actual = TaskFactory.reconstitute(id, userId, title, description, startDate, null);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(expectedId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(expectedUserId);
                softly.assertThat(actual.getTitle()).as("title").isEqualTo(expectedTitle);
                softly.assertThat(actual.getDescription()).as("description").isEqualTo(expectedDescription);
                softly.assertThat(actual.getStartDate()).as("start date").isEqualTo(expectedStartDate);
                softly.assertThat(actual.getEndDate()).as("end date").isNull();
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> TaskFactory.reconstitute(null, userId, "Title", "Description", Instant.now(), Instant.now()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Task ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

            // When
            var actual = catchThrowable(() -> TaskFactory.reconstitute(id, null, "Title", "Description", Instant.now(), Instant.now()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullTitle() {
            // Given
            var id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
            var userId = UUID.fromString("09726a94-df21-48ad-864a-f3612499ff3d");

            // When
            var actual = catchThrowable(() -> TaskFactory.reconstitute(id, userId, null, "Description", Instant.now(), Instant.now()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Title must not be null or empty");
        }

    }

}
