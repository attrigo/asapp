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

import java.util.Objects;

/**
 * Represents a task entity.
 * <p>
 * This aggregate root encapsulates task identity and common task information.
 * <p>
 * Tasks can exist in two states: new (transient, without ID) and reconstituted (persistent, with ID).
 * <p>
 * Equality is based on ID; new instances are not considered equal to any other instance.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class Task {

    private final TaskId id;

    private UserId userId;

    private Title title;

    private Description description;

    private StartDate startDate;

    private EndDate endDate;

    /**
     * Constructs a new {@code Task} instance and validates its integrity.
     *
     * @param userId      the task's user unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private Task(UserId userId, Title title, Description description, StartDate startDate, EndDate endDate) {
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.id = null;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Constructs a reconstituted {@code Task} instance and validates its integrity.
     *
     * @param id          the task's unique identifier
     * @param userId      the task's user unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private Task(TaskId id, UserId userId, Title title, Description description, StartDate startDate, EndDate endDate) {
        validateIdIsNotNull(id);
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Factory method to create a task without a persistent ID.
     * <p>
     * Typically used when registering a new task before persistence.
     *
     * @param userId      the task's user unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @return a new {@code Task} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static Task create(UserId userId, Title title, Description description, StartDate startDate, EndDate endDate) {
        return new Task(userId, title, description, startDate, endDate);
    }

    /**
     * Factory method to reconstitute a task with a persistent ID.
     * <p>
     * Typically used when reconstituting a task from the database.
     *
     * @param id          the task's unique identifier
     * @param userId      the task's user unique identifier
     * @param title       the task's title
     * @param description the task's description
     * @param startDate   the task's start date
     * @param endDate     the task's end date
     * @return a reconstituted {@code Task} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public static Task reconstitute(TaskId id, UserId userId, Title title, Description description, StartDate startDate, EndDate endDate) {
        return new Task(id, userId, title, description, startDate, endDate);
    }

    /**
     * Updates the task's information.
     *
     * @param title       the new title
     * @param description the new description
     * @param startDate   the new start date
     * @param endDate     the new end date
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public void update(UserId userId, Title title, Description description, StartDate startDate, EndDate endDate) {
        validateUserIdIsNotNull(userId);
        validateTitleIsNotNull(title);
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Determines equality based on task state.
     * <p>
     * Two {@code Task} instances are equal only if both have non-null IDs that match.
     * <p>
     * Non-persisted instances are never equal to any other instance.
     *
     * @param object the object to compare
     * @return {@code true} if equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Task other = (Task) object;
        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    /**
     * Generates hash code based on task ID.
     * <p>
     * Uses ID for reconstituted instances and identity hash code for new instances.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.id != null ? Objects.hashCode(this.id) : System.identityHashCode(this);
    }

    /**
     * Returns the task's unique identifier.
     *
     * @return the {@link TaskId}, or {@code null} for new tasks
     */
    public TaskId getId() {
        return this.id;
    }

    /**
     * Returns the task's user unique identifier.
     *
     * @return the {@link UserId}
     */
    public UserId getUserId() {
        return this.userId;
    }

    /**
     * Returns the task's title.
     *
     * @return the {@link Title}
     */
    public Title getTitle() {
        return this.title;
    }

    /**
     * Returns the task's description.
     *
     * @return the {@link Description}
     */
    public Description getDescription() {
        return this.description;
    }

    /**
     * Returns the task's start date.
     *
     * @return the {@link StartDate}
     */
    public StartDate getStartDate() {
        return this.startDate;
    }

    /**
     * Returns the task's end date.
     *
     * @return the {@link EndDate}
     */
    public EndDate getEndDate() {
        return this.endDate;
    }

    /**
     * Validates that the task ID is not {@code null}.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is {@code null}
     */
    private static void validateIdIsNotNull(TaskId id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    /**
     * Validates that the user ID is not {@code null}.
     *
     * @param userId the user ID to validate
     * @throws IllegalArgumentException if the user ID is {@code null}
     */
    private static void validateUserIdIsNotNull(UserId userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }

    /**
     * Validates that the title is not {@code null}.
     *
     * @param title the title to validate
     * @throws IllegalArgumentException if the title is {@code null}
     */
    private static void validateTitleIsNotNull(Title title) {
        if (title == null) {
            throw new IllegalArgumentException("Title must not be null");
        }
    }

}
