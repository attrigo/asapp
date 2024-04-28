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
package com.bcn.asapp.tasks.task;

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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.dtos.task.TaskDTO;

@ExtendWith(SpringExtension.class)
class TaskServiceImpTests {

    @Mock
    private TaskRepository taskRepositoryMock;

    @Spy
    private TaskMapperImpl taskMapperSpy;

    @InjectMocks
    private TaskServiceImpl taskService;

    private UUID fakeTaskId;

    private String fakeTaskTitle;

    private String fakeTaskDescription;

    private LocalDateTime fakeTaskStartDate;

    @BeforeEach
    void beforeEach() {
        given(taskMapperSpy.toTaskDTO(any(Task.class))).willCallRealMethod();
        given(taskMapperSpy.toTask(any(TaskDTO.class))).willCallRealMethod();
        given(taskMapperSpy.toTaskIgnoreId(any(TaskDTO.class))).willCallRealMethod();

        this.fakeTaskId = UUID.randomUUID();
        this.fakeTaskTitle = "Test Title";
        this.fakeTaskDescription = "Test Description";
        this.fakeTaskStartDate = LocalDateTime.now();
    }

    // findById
    @Test
    @DisplayName("GIVEN id does not exists WHEN find task by id THEN finds the task with the given id And returns empty")
    void IdNotExists_FindById_FindsTaskAndReturnsEmpty() {
        // Given
        given(taskRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

        // When
        var idToFind = fakeTaskId;

        var actual = taskService.findById(idToFind);

        // Then
        assertFalse(actual.isPresent());

        then(taskRepositoryMock).should(times(1))
                                .findById(idToFind);
    }

    @Test
    @DisplayName("GIVEN id exists WHEN find task by id THEN finds the task with the given id And returns the task found")
    void IdExists_FindById_FindsTaskAndReturnsTaskFound() {
        // Given
        var fakeTask = new Task(fakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        given(taskRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeTask));

        // When
        var idToFind = fakeTaskId;

        var actual = taskService.findById(idToFind);

        // Then
        var expected = TaskDTO.builder()
                              .id(fakeTaskId)
                              .title(fakeTaskTitle)
                              .description(fakeTaskDescription)
                              .startDateTime(fakeTaskStartDate)
                              .build();

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        then(taskRepositoryMock).should(times(1))
                                .findById(idToFind);
    }

    // findAll
    @Test
    @DisplayName("GIVEN there are not tasks WHEN find all tasks THEN does not find tasks And returns empty list")
    void ThereAreNotTasks_FindAll_DoesNotFindTasksAndReturnsEmptyList() {
        // Given
        given(taskRepositoryMock.findAll()).willReturn(Collections.emptyList());

        // When
        var actual = taskService.findAll();

        // Then
        assertTrue(actual.isEmpty());

        then(taskRepositoryMock).should(times(1))
                                .findAll();
    }

    @Test
    @DisplayName("GIVEN there are tasks WHEN find all tasks THEN finds tasks And returns the tasks found")
    void ThereAreTasks_FindAll_FindsTasksAndReturnsTasksFound() {
        var fakeTask1Id = UUID.randomUUID();
        var fakeTask2Id = UUID.randomUUID();
        var fakeTask3Id = UUID.randomUUID();

        var fakeTaskStartDate1 = LocalDateTime.now();
        var fakeTaskStartDate2 = LocalDateTime.now();
        var fakeTaskStartDate3 = LocalDateTime.now();

        // Given
        var fakeTask1 = new Task(fakeTask1Id, fakeTaskTitle + " 1", fakeTaskDescription + " 1", fakeTaskStartDate1);
        var fakeTask2 = new Task(fakeTask2Id, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate2);
        var fakeTask3 = new Task(fakeTask3Id, fakeTaskTitle + " 3", fakeTaskDescription + " 3", fakeTaskStartDate3);
        var fakeTasks = Arrays.asList(fakeTask1, fakeTask2, fakeTask3);
        given(taskRepositoryMock.findAll()).willReturn(fakeTasks);

        // When
        var actual = taskService.findAll();

        // Then
        var expectedTask1 = TaskDTO.builder()
                                   .id(fakeTask1Id)
                                   .title(fakeTaskTitle + " 1")
                                   .description(fakeTaskDescription + " 1")
                                   .startDateTime(fakeTaskStartDate1)
                                   .build();
        var expectedTask2 = TaskDTO.builder()
                                   .id(fakeTask2Id)
                                   .title(fakeTaskTitle + " 2")
                                   .description(fakeTaskDescription + " 2")
                                   .startDateTime(fakeTaskStartDate2)
                                   .build();
        var expectedTask3 = TaskDTO.builder()
                                   .id(fakeTask3Id)
                                   .title(fakeTaskTitle + " 3")
                                   .description(fakeTaskDescription + " 3")
                                   .startDateTime(fakeTaskStartDate3)
                                   .build();
        var expected = Arrays.asList(expectedTask1, expectedTask2, expectedTask3);

        assertIterableEquals(expected, actual);

        then(taskRepositoryMock).should(times(1))
                                .findAll();
    }

    // Create
    @Test
    @DisplayName("GIVEN task id is not null WHEN create a task THEN creates the task ignoring the given task id And returns the task created with a new id")
    void TaskIdIsNotNull_Create_CreatesTaskIgnoringGivenTaskIdAndReturnsTaskCreated() {
        var newFakeTaskId = UUID.randomUUID();

        // Given
        var fakeTask = new Task(newFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

        // When
        var taskToCreate = TaskDTO.builder()
                                  .id(fakeTaskId)
                                  .title(fakeTaskTitle)
                                  .description(fakeTaskDescription)
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.create(taskToCreate);

        // Then
        var expected = TaskDTO.builder()
                              .id(newFakeTaskId)
                              .title(fakeTaskTitle)
                              .description(fakeTaskDescription)
                              .startDateTime(fakeTaskStartDate)
                              .build();

        assertEquals(expected, actual);

        ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
        then(taskRepositoryMock).should(times(1))
                                .save(taskArgumentCaptor.capture());
        Task taskArgument = taskArgumentCaptor.getValue();
        assertNull(taskArgument.id());
    }

    @Test
    @DisplayName("GIVEN task id is null WHEN create a task THEN creates the task And returns the task created with a new id")
    void TaskIdIsNull_Create_CreatesTaskAndReturnsTaskCreated() {
        var newFakeTaskId = UUID.randomUUID();

        // Given
        var fakeTask = new Task(newFakeTaskId, fakeTaskTitle, fakeTaskDescription, fakeTaskStartDate);
        given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

        // When
        var taskToCreate = TaskDTO.builder()
                                  .id(null)
                                  .title(fakeTaskTitle)
                                  .description(fakeTaskDescription)
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.create(taskToCreate);

        // Then
        var expected = TaskDTO.builder()
                              .id(newFakeTaskId)
                              .title(fakeTaskTitle)
                              .description(fakeTaskDescription)
                              .startDateTime(fakeTaskStartDate)
                              .build();

        assertEquals(expected, actual);

        ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
        then(taskRepositoryMock).should(times(1))
                                .save(taskArgumentCaptor.capture());
        Task taskArgument = taskArgumentCaptor.getValue();
        assertNull(taskArgument.id());
    }

    // Update
    @Test
    @DisplayName("GIVEN id does not exists And task id is not null WHEN update a task by id THEN does not update the task And returns empty")
    void IdNotExistsAndTaskIdIsNotNull_UpdateById_DoesNotUpdateTaskAndReturnsEmpty() {
        // Given
        given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeTaskId;
        var taskToUpdate = TaskDTO.builder()
                                  .id(UUID.randomUUID())
                                  .title(fakeTaskTitle)
                                  .description(fakeTaskDescription)
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.updateById(idToUpdate, taskToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(taskRepositoryMock).should(never())
                                .save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN id does not exists And task id is null WHEN update a task by id THEN does not update the task And returns empty")
    void IdNotExistsAndTaskIdIsNull_UpdateById_DoesNotUpdateTaskAndReturnsEmpty() {
        // Given
        given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeTaskId;
        var taskToUpdate = TaskDTO.builder()
                                  .id(null)
                                  .title(fakeTaskTitle)
                                  .description(fakeTaskDescription)
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.updateById(idToUpdate, taskToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(taskRepositoryMock).should(never())
                                .save(any(Task.class));
    }

    @Test
    @DisplayName("GIVEN id exists And task id is not null WHEN update a task by id THEN updates all fields of the task except the id And Returns the task updated with the new values")
    void IdExistsAndTaskIdIsNotNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsTaskUpdated() {
        // Given
        given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeTask = new Task(fakeTaskId, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);
        given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

        // When
        var idToUpdate = fakeTaskId;
        var taskToUpdate = TaskDTO.builder()
                                  .id(UUID.randomUUID())
                                  .title(fakeTaskTitle + " 2")
                                  .description(fakeTaskDescription + " 2")
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.updateById(idToUpdate, taskToUpdate);

        // Then
        var expected = TaskDTO.builder()
                              .id(fakeTaskId)
                              .title(fakeTaskTitle + " 2")
                              .description(fakeTaskDescription + " 2")
                              .startDateTime(fakeTaskStartDate)
                              .build();

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
        then(taskRepositoryMock).should(times(1))
                                .save(taskArgumentCaptor.capture());
        Task taskArgument = taskArgumentCaptor.getValue();
        assertEquals(fakeTaskId, taskArgument.id());
    }

    @Test
    @DisplayName("GIVEN id exists And task id is null WHEN update a task by id THEN updates all fields of the task except the id And Returns the task updated with the new values")
    void IdExistsAndTaskIdIsNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsTaskUpdated() {
        // Given
        given(taskRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeTask = new Task(fakeTaskId, fakeTaskTitle + " 2", fakeTaskDescription + " 2", fakeTaskStartDate);
        given(taskRepositoryMock.save(any(Task.class))).willReturn(fakeTask);

        // When
        var idToUpdate = fakeTaskId;
        var taskToUpdate = TaskDTO.builder()
                                  .id(null)
                                  .title(fakeTaskTitle + " 2")
                                  .description(fakeTaskDescription + " 2")
                                  .startDateTime(fakeTaskStartDate)
                                  .build();

        var actual = taskService.updateById(idToUpdate, taskToUpdate);

        // Then
        var expected = TaskDTO.builder()
                              .id(fakeTaskId)
                              .title(fakeTaskTitle + " 2")
                              .description(fakeTaskDescription + " 2")
                              .startDateTime(fakeTaskStartDate)
                              .build();

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Task> taskArgumentCaptor = ArgumentCaptor.forClass(Task.class);
        then(taskRepositoryMock).should(times(1))
                                .save(taskArgumentCaptor.capture());
        Task taskArgument = taskArgumentCaptor.getValue();
        assertEquals(fakeTaskId, taskArgument.id());
    }

    // Delete
    @Test
    @DisplayName("GIVEN id does not exists WHEN delete a task by id THEN does not delete any task And returns false")
    void IdNotExists_DeleteById_DoesNotDeleteAnyTaskAndReturnsFalse() {
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
    @DisplayName("GIVEN id exists WHEN delete a task by id THEN deletes the task with the given id And returns true")
    void IdExists_DeleteById_DeletesTaskWithGivenIdAndReturnsTrue() {
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
