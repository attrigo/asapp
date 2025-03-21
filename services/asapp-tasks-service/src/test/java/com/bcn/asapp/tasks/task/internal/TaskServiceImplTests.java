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
package com.bcn.asapp.tasks.task.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.tasks.task.Task;
import com.bcn.asapp.tasks.task.TaskMapperImpl;
import com.bcn.asapp.tasks.task.TaskRepository;

@ExtendWith(SpringExtension.class)
class TaskServiceImplTests {

    @Mock
    private TaskRepository taskRepositoryMock;

    @Spy
    private TaskMapperImpl taskMapperSpy;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UUID fakeTaskId;

    private String fakeTaskTitle;

    private String fakeTaskDescription;

    private Instant fakeTaskStartDate;

    private UUID fakeProjectId;

    @BeforeEach
    void beforeEach() {
        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskTitle = "UT Title";
        this.fakeTaskDescription = "UT Description";
        this.fakeTaskStartDate = Instant.now()
                                        .truncatedTo(ChronoUnit.MILLIS);
        this.fakeProjectId = UUID.randomUUID();
    }

    @Nested
    class FindById {

        @Test
        @DisplayName("GIVEN task id does not exists WHEN find a task by id THEN does not find the task And returns empty")
        void TaskIdNotExists_FindById_DoesNotFindTaskAndReturnsEmpty() {
            // Given
            given(taskRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When
            var idToFind = fakeTaskId;

            var actualTask = taskService.findById(idToFind);

            // Then
            assertFalse(actualTask.isPresent());

            then(taskRepositoryMock).should(times(1))
                                    .findById(idToFind);
        }

        @Test
        @DisplayName("GIVEN task id exists WHEN find a task by id THEN finds the task And returns the task found")
        void TaskIdExists_FindById_FindsTaskAndReturnsTaskFound() {
            // Given
            var fakeTask = new Task(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeTask));

            // When
            var idToFind = fakeTaskId;

            var actualTask = taskService.findById(idToFind);

            // Then
            var expectedTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            assertTrue(actualTask.isPresent());
            assertEquals(expectedTask, actualTask.get());

            then(taskRepositoryMock).should(times(1))
                                    .findById(idToFind);
        }

    }

    @Nested
    class FindAll {

        @Test
        @DisplayName("GIVEN there are not tasks WHEN find all tasks THEN does not find any tasks And returns empty list")
        void ThereAreNotTasks_FindAll_DoesNotFindTasksAndReturnsEmptyList() {
            // Given
            given(taskRepositoryMock.findAll()).willReturn(Collections.emptyList());

            // When
            var actualTasks = taskService.findAll();

            // Then
            assertTrue(actualTasks.isEmpty());

            then(taskRepositoryMock).should(times(1))
                                    .findAll();
        }

        @Test
        @DisplayName("GIVEN there are tasks WHEN find all tasks THEN finds tasks And returns the tasks found")
        void ThereAreTasks_FindAll_FindsTasksAndReturnsTasksFound() {
            var fakeTaskId1 = UUID.randomUUID();
            var fakeTaskId2 = UUID.randomUUID();
            var fakeTaskId3 = UUID.randomUUID();
            var fakeTaskTitle1 = fakeTaskTitle + " 1";
            var fakeTaskTitle2 = fakeTaskTitle + " 2";
            var fakeTaskTitle3 = fakeTaskTitle + " 3";
            var fakeTaskDesc1 = fakeTaskDescription + " 1";
            var fakeTaskDesc2 = fakeTaskDescription + " 2";
            var fakeTaskDesc3 = fakeTaskDescription + " 3";

            // Given
            var fakeTask1 = new Task(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var fakeTask2 = new Task(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var fakeTask3 = new Task(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var fakeTasks = List.of(fakeTask1, fakeTask2, fakeTask3);
            given(taskRepositoryMock.findAll()).willReturn(fakeTasks);

            // When
            var actualTasks = taskService.findAll();

            // Then
            var expectedTask1 = new TaskDTO(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var expectedTask2 = new TaskDTO(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var expectedTask3 = new TaskDTO(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var expectedTasks = List.of(expectedTask1, expectedTask2, expectedTask3);

            assertIterableEquals(expectedTasks, actualTasks);

            then(taskRepositoryMock).should(times(1))
                                    .findAll();
        }

    }

    @Nested
    class FindByProjectId {

        @Test
        @DisplayName("GIVEN there are not tasks with project id WHEN find tasks by project id THEN does not find any tasks And returns empty list")
        void ThereAreNotTasksWithProjectId_FindByProjectId_DoesNotFindTasksAndReturnsEmptyList() {
            // Given
            given(taskRepositoryMock.findByProjectId(any(UUID.class))).willReturn(Collections.emptyList());

            // When
            var idToFind = fakeTaskId;

            var actualTasks = taskService.findByProjectId(idToFind);

            // Then
            assertTrue(actualTasks.isEmpty());

            then(taskRepositoryMock).should(times(1))
                                    .findByProjectId(idToFind);
        }

        @Test
        @DisplayName("GIVEN there are tasks with project id WHEN find tasks by project id THEN finds the tasks And returns the tasks found")
        void ThereAreTasksWithProjectId_FindByProjectId_FindsTasksAndReturnsTasksFound() {
            var fakeTaskId1 = UUID.randomUUID();
            var fakeTaskId2 = UUID.randomUUID();
            var fakeTaskId3 = UUID.randomUUID();
            var fakeTaskTitle1 = fakeTaskTitle + " 1";
            var fakeTaskTitle2 = fakeTaskTitle + " 2";
            var fakeTaskTitle3 = fakeTaskTitle + " 3";
            var fakeTaskDesc1 = fakeTaskDescription + " 1";
            var fakeTaskDesc2 = fakeTaskDescription + " 2";
            var fakeTaskDesc3 = fakeTaskDescription + " 3";

            // Given
            var fakeTask1 = new Task(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var fakeTask2 = new Task(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var fakeTask3 = new Task(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var fakeTasks = List.of(fakeTask1, fakeTask2, fakeTask3);
            given(taskRepositoryMock.findByProjectId(any(UUID.class))).willReturn(fakeTasks);

            // When
            var idToFind = fakeProjectId;

            var actualTasks = taskService.findByProjectId(idToFind);

            // Then
            var expectedTask1 = new TaskDTO(fakeTaskId1, fakeTaskTitle1, fakeTaskDesc1, fakeTaskStartDate, fakeProjectId);
            var expectedTask2 = new TaskDTO(fakeTaskId2, fakeTaskTitle2, fakeTaskDesc2, fakeTaskStartDate, fakeProjectId);
            var expectedTask3 = new TaskDTO(fakeTaskId3, fakeTaskTitle3, fakeTaskDesc3, fakeTaskStartDate, fakeProjectId);
            var expectedTasks = List.of(expectedTask1, expectedTask2, expectedTask3);

            assertIterableEquals(expectedTasks, actualTasks);

            then(taskRepositoryMock).should(times(1))
                                    .findByProjectId(idToFind);
        }

    }

    @Nested
    class Create {

        @Test
        @DisplayName("GIVEN task id field is not null WHEN create a task THEN creates the task ignoring the given task id And returns the task created with a new id")
        void TaskIdFieldIsNotNull_Create_CreatesTaskIgnoringTaskIdAndReturnsTaskCreated() {
            var anotherFakeTaskId = UUID.randomUUID();

            // Given
            var fakeTask = new Task(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

            // When
            var taskToCreate = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.create(taskToCreate);

            // Then
            var expectedTask = new TaskDTO(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            assertEquals(expectedTask, actualTask);

            ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
            then(taskRepositoryMock).should(times(1))
                                    .save(taskArgumentCaptor.capture());
            Task taskArgument = taskArgumentCaptor.getValue();
            assertNull(taskArgument.id());
        }

        @Test
        @DisplayName("GIVEN task id field is null WHEN create a task THEN creates the task And returns the task created with a new id")
        void TaskIdFieldIsNull_Create_CreatesTaskAndReturnsTaskCreated() {
            var anotherFakeTaskId = UUID.randomUUID();

            // Given
            var fakeTask = new Task(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

            // When
            var taskToCreate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.create(taskToCreate);

            // Then
            var expectedTask = new TaskDTO(anotherFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            assertEquals(expectedTask, actualTask);

            ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
            then(taskRepositoryMock).should(times(1))
                                    .save(taskArgumentCaptor.capture());
            Task taskArgument = taskArgumentCaptor.getValue();
            assertNull(taskArgument.id());
        }

    }

    @Nested
    class UpdateById {

        @Test
        @DisplayName("GIVEN task id does not exists And new task data id field is not null WHEN update a task by id THEN does not update the task And returns empty")
        void TaskIdNotExistsAndNewTaskDataIdFieldIsNotNull_UpdateById_DoesNotUpdateTaskAndReturnsEmpty() {
            // Given
            given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(false);

            // When
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(UUID.randomUUID(), fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.updateById(idToUpdate, taskToUpdate);

            // Then
            assertFalse(actualTask.isPresent());

            then(taskRepositoryMock).should(never())
                                    .save(any(Task.class));
        }

        @Test
        @DisplayName("GIVEN task id does not exists And new task data id field is null WHEN update a task by id THEN does not update the task And returns empty")
        void TaskIdNotExistsAndNewTaskDataIdFieldIsNull_UpdateById_DoesNotUpdateTaskAndReturnsEmpty() {
            // Given
            given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(false);

            // When
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.updateById(idToUpdate, taskToUpdate);

            // Then
            assertFalse(actualTask.isPresent());

            then(taskRepositoryMock).should(never())
                                    .save(any(Task.class));
        }

        @Test
        @DisplayName("GIVEN task id exists And new task data id field is not null WHEN update a task by id THEN updates all fields of the task except the id And returns the task updated with the new data")
        void TaskIdExistsAndNewTaskDataIdFieldIsNotNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsTaskUpdated() {
            // Given
            given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(true);

            var fakeTask = new Task(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

            // When
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(UUID.randomUUID(), fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.updateById(idToUpdate, taskToUpdate);

            // Then
            var expectedTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            assertTrue(actualTask.isPresent());
            assertEquals(expectedTask, actualTask.get());

            ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
            then(taskRepositoryMock).should(times(1))
                                    .save(taskArgumentCaptor.capture());
            Task taskArgument = taskArgumentCaptor.getValue();
            assertEquals(fakeTaskId, taskArgument.id());
        }

        @Test
        @DisplayName("GIVEN task id exists And new task data id field is null WHEN update a task by id THEN updates all fields of the task except the id And returns the task updated with the new data")
        void TaskIdExistsAndNewTaskDataIdFieldIsNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsTaskUpdated() {
            // Given
            given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(true);

            var fakeTask = new Task(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);
            given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

            // When
            var idToUpdate = fakeTaskId;
            var taskToUpdate = new TaskDTO(null, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            var actualTask = taskService.updateById(idToUpdate, taskToUpdate);

            // Then
            var expectedTask = new TaskDTO(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate, fakeProjectId);

            assertTrue(actualTask.isPresent());
            assertEquals(expectedTask, actualTask.get());

            ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
            then(taskRepositoryMock).should(times(1))
                                    .save(taskArgumentCaptor.capture());
            Task taskArgument = taskArgumentCaptor.getValue();
            assertEquals(fakeTaskId, taskArgument.id());
        }

    }

    @Nested
    class DeleteById {

        @Test
        @DisplayName("GIVEN task id does not exists WHEN delete a task by id THEN does not delete the task And returns false")
        void TaskIdNotExists_DeleteById_DoesNotDeleteTaskAndReturnsFalse() {
            // Given
            given(taskRepositoryMock.deleteTaskById(any(UUID.class))).willReturn(0L);

            // When
            var idToDelete = fakeTaskId;

            var actual = taskService.deleteById(idToDelete);

            // Then
            assertFalse(actual);

            then(taskRepositoryMock).should(times(1))
                                    .deleteTaskById(idToDelete);
        }

        @Test
        @DisplayName("GIVEN task id exists WHEN delete a task by id THEN deletes the task And returns true")
        void TaskIdExists_DeleteById_DeletesTaskAndReturnsTrue() {
            // Given
            given(taskRepositoryMock.deleteTaskById(any(UUID.class))).willReturn(1L);

            // When
            var idToDelete = fakeTaskId;

            var actual = taskService.deleteById(idToDelete);

            // Then
            assertTrue(actual);

            then(taskRepositoryMock).should(times(1))
                                    .deleteTaskById(idToDelete);
        }

    }

}
