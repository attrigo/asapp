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
package com.bcn.asapp.projects.project;

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
import java.util.List;
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

import com.bcn.asapp.clients.client.task.TaskClient;
import com.bcn.asapp.dto.project.ProjectDTO;
import com.bcn.asapp.dto.task.TaskDTO;

@ExtendWith(SpringExtension.class)
class ProjectServiceImpTests {

    @Mock
    private ProjectRepository projectRepositoryMock;

    @Spy
    private ProjectMapperImpl projectMapperSpy;

    @Mock
    private TaskClient taskClientMock;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private LocalDateTime fakeProjectStartDate;

    private List<TaskDTO> fakeProjectTasks;

    @BeforeEach
    void beforeEach() {
        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectTitle = "UT Title";
        this.fakeProjectDescription = "UT Description";
        this.fakeProjectStartDate = LocalDateTime.now();
        var fakeTask1 = new TaskDTO(UUID.randomUUID(), "UT Task Title 1", "UT Task Description 1", LocalDateTime.now(), fakeProjectId);
        var fakeTask2 = new TaskDTO(UUID.randomUUID(), "UT Task Title 2", "UT Task Description 2", LocalDateTime.now(), fakeProjectId);
        fakeProjectTasks = List.of(fakeTask1, fakeTask2);
    }

    // findById
    @Test
    @DisplayName("GIVEN project id does not exists WHEN find a project by id THEN does not find the project And returns empty")
    void ProjectIdNotExists_FindById_DoesNotFindProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
        then(taskClientMock).should(never())
                            .getTasksByProjectId(any(UUID.class));
    }

    @Test
    @DisplayName("GIVEN project id exists without tasks WHEN find a project by id THEN finds the project And returns the project with empty list of tasks")
    void ProjectIdExistsWithoutTasks_FindById_FindsProjectAndReturnsProjectWithEmptyListTasks() {
        // Given
        var fakeProject = new Project(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(Collections.emptyList());

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, Collections.emptyList());

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProjectId);
    }

    @Test
    @DisplayName("GIVEN project id exists with tasks And tasks service is not available WHEN find a project by id THEN finds the project And returns the project with null tasks")
    void ProjectIdExistsWithTasksAndTasksServiceIsNotAvailable_FindById_FindsProjectAndReturnsProjectWithNullTasks() {
        // Given
        var fakeProject = new Project(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(null);

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProjectId);
    }

    @Test
    @DisplayName("GIVEN project id exists with tasks WHEN find a project by id THEN finds the project And returns the project with tasks")
    void ProjectIdExistsWithTasks_FindById_FindsProjectAndReturnsProjectWithTasks() {
        // Given
        var fakeProject = new Project(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(fakeProjectTasks);

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProjectId);
    }

    // findAll
    @Test
    @DisplayName("GIVEN there are not projects WHEN find all projects THEN does not find any projects And returns empty list")
    void ThereAreNotProjects_FindAll_DoesNotFindProjectsAndReturnsEmptyList() {
        // Given
        given(projectRepositoryMock.findAll()).willReturn(Collections.emptyList());

        // When
        var actual = projectService.findAll();

        // Then
        assertTrue(actual.isEmpty());

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
        then(taskClientMock).should(never())
                            .getTasksByProjectId(any(UUID.class));
    }

    @Test
    @DisplayName("GIVEN there are projects without tasks WHEN find all projects THEN finds projects And returns the projects with empty list of tasks")
    void ThereAreProjectsWithoutTasks_FindAll_FindsProjectsAndReturnsProjectsWithEmptyListTasks() {
        var fakeProject1Id = UUID.randomUUID();
        var fakeProject2Id = UUID.randomUUID();
        var fakeProject3Id = UUID.randomUUID();

        // Given
        var fakeProject1 = new Project(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var fakeProject2 = new Project(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var fakeProject3 = new Project(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var fakeProjects = Arrays.asList(fakeProject1, fakeProject2, fakeProject3);
        given(projectRepositoryMock.findAll()).willReturn(fakeProjects);

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(Collections.emptyList());

        // When
        var actual = projectService.findAll();

        // Then
        var expectedProject1 = new ProjectDTO(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate,
                Collections.emptyList());
        var expectedProject2 = new ProjectDTO(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate,
                Collections.emptyList());
        var expectedProject3 = new ProjectDTO(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate,
                Collections.emptyList());
        var expected = Arrays.asList(expectedProject1, expectedProject2, expectedProject3);

        assertIterableEquals(expected, actual);

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject1Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject2Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject3Id);
    }

    @Test
    @DisplayName("GIVEN there are projects with tasks And tasks service is not available WHEN find all projects THEN finds projects And returns the projects with null tasks")
    void ThereAreProjectsWithTasksAndTasksServiceIsNotAvailable_FindAll_FindsProjectsAndReturnsProjectsWithNullTasks() {
        var fakeProject1Id = UUID.randomUUID();
        var fakeProject2Id = UUID.randomUUID();
        var fakeProject3Id = UUID.randomUUID();

        // Given
        var fakeProject1 = new Project(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var fakeProject2 = new Project(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var fakeProject3 = new Project(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var fakeProjects = Arrays.asList(fakeProject1, fakeProject2, fakeProject3);
        given(projectRepositoryMock.findAll()).willReturn(fakeProjects);

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(null);

        // When
        var actual = projectService.findAll();

        // Then
        var expectedProject1 = new ProjectDTO(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate, null);
        var expectedProject2 = new ProjectDTO(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);
        var expectedProject3 = new ProjectDTO(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate, null);
        var expected = Arrays.asList(expectedProject1, expectedProject2, expectedProject3);

        assertIterableEquals(expected, actual);

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject1Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject2Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject3Id);
    }

    @Test
    @DisplayName("GIVEN there are projects with tasks WHEN find all projects THEN finds projects And returns the projects with tasks")
    void ThereAreProjectsWithTasks_FindAll_FindsProjectsAndReturnsProjectsWithTasks() {
        var fakeProject1Id = UUID.randomUUID();
        var fakeProject2Id = UUID.randomUUID();
        var fakeProject3Id = UUID.randomUUID();

        // Given
        var fakeProject1 = new Project(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var fakeProject2 = new Project(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var fakeProject3 = new Project(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var fakeProjects = Arrays.asList(fakeProject1, fakeProject2, fakeProject3);
        given(projectRepositoryMock.findAll()).willReturn(fakeProjects);

        given(taskClientMock.getTasksByProjectId(any(UUID.class))).willReturn(fakeProjectTasks);

        // When
        var actual = projectService.findAll();

        // Then
        var expectedProject1 = new ProjectDTO(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate, fakeProjectTasks);
        var expectedProject2 = new ProjectDTO(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, fakeProjectTasks);
        var expectedProject3 = new ProjectDTO(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate, fakeProjectTasks);
        var expected = Arrays.asList(expectedProject1, expectedProject2, expectedProject3);

        assertIterableEquals(expected, actual);

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject1Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject2Id);
        then(taskClientMock).should(times(1))
                            .getTasksByProjectId(fakeProject3Id);
    }

    // Create
    @Test
    @DisplayName("GIVEN project id field is not null WHEN create a project THEN creates the project ignoring the given project id And returns the project created with a new id")
    void ProjectIdFieldIsNotNull_Create_CreatesProjectIgnoringIdAndReturnsProjectCreated() {
        var anotherFakeProjectId = UUID.randomUUID();

        // Given
        var fakeProject = new Project(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var projectToCreate = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        var actual = projectService.create(projectToCreate);

        // Then
        var expected = new ProjectDTO(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        assertEquals(expected, actual);

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertNull(projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN project tasks field is not null WHEN create a project THEN creates the project ignoring the given project tasks And returns the project created")
    void ProjectTasksFieldIsNotNull_Create_CreatesProjectIgnoringTasksAndReturnsProjectCreated() {
        var anotherFakeProjectId = UUID.randomUUID();

        // Given
        var fakeProject = new Project(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var projectToCreate = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);

        var actual = projectService.create(projectToCreate);

        // Then
        var expected = new ProjectDTO(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        assertEquals(expected, actual);

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertNull(projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN project id field is null WHEN create a project THEN creates the project And returns the project created with a new id")
    void ProjectIdFieldIsNull_Create_CreatesProjectAndReturnsProjectCreated() {
        var anotherFakeProjectId = UUID.randomUUID();

        // Given
        var fakeProject = new Project(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        var actual = projectService.create(projectToCreate);

        // Then
        var expected = new ProjectDTO(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        assertEquals(expected, actual);

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertNull(projectArgument.id());
    }

    // Update
    @Test
    @DisplayName("GIVEN project id does not exists And new project data id field is not null WHEN update a project by id THEN does not update the project And returns empty")
    void ProjectIdNotExistsAndNewProjectDataIdFieldIsNotNull_UpdateById_DoesNotUpdateProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(never())
                                   .save(any(Project.class));
    }

    @Test
    @DisplayName("GIVEN project id does not exists And new project data tasks field is not null WHEN update a project by id THEN does not update the project And returns empty")
    void ProjectIdNotExistsAndNewProjectDataTasksFieldIsNotNull_UpdateById_DoesNotUpdateProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(never())
                                   .save(any(Project.class));
    }

    @Test
    @DisplayName("GIVEN project id does not exists And new project data id field is null WHEN update a project by id THEN does not update the project And returns empty")
    void ProjectIdNotExistsAndNewProjectDataIdFieldIsNull_UpdateById_DoesNotUpdateProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(never())
                                   .save(any(Project.class));
    }

    @Test
    @DisplayName("GIVEN project id exists And new project data id field is not null WHEN update a project by id THEN updates all fields of the project except the id And returns the project updated with the new data")
    void ProjectIdExistsAndNewProjectDataIdFieldIsNotNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsProjectUpdated() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeProject = new Project(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertEquals(fakeProjectId, projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN project id exists And project tasks field is not null WHEN update a project by id THEN updates all fields of the project except the tasks And returns the project updated with the new data")
    void ProjectIdExistsAndProjectTasksFieldIsNotNull_UpdateById_UpdatesAllFieldsExceptTasksAndReturnsProjectUpdated() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeProject = new Project(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, fakeProjectTasks);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertEquals(fakeProjectId, projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN project id exists And new project data id field is null WHEN update a project by id THEN updates all fields of the project except the id And returns the project updated with the new data")
    void ProjectIdExistsAndNewProjectDataIdFieldIsNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsProjectUpdated() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeProject = new Project(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate, null);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertEquals(fakeProjectId, projectArgument.id());
    }

    // Delete
    @Test
    @DisplayName("GIVEN project id does not exists WHEN delete a project by id THEN does not delete the project And returns false")
    void ProjectIdNotExists_DeleteById_DoesNotDeleteProjectAndReturnsFalse() {
        // Given
        given(projectRepositoryMock.deleteProjectById(any(UUID.class))).willReturn(0L);

        // When
        var idToDelete = fakeProjectId;

        var actual = projectService.deleteById(idToDelete);

        // Then
        assertFalse(actual);

        then(projectRepositoryMock).should(times(1))
                                   .deleteProjectById(idToDelete);
    }

    @Test
    @DisplayName("GIVEN project id exists WHEN delete a project by id THEN deletes the project And returns true")
    void ProjectIdExists_DeleteById_DeletesProjectAndReturnsTrue() {
        // Given
        given(projectRepositoryMock.deleteProjectById(any(UUID.class))).willReturn(1L);

        // When
        var idToDelete = fakeProjectId;

        var actual = projectService.deleteById(idToDelete);

        // Then
        assertTrue(actual);

        then(projectRepositoryMock).should(times(1))
                                   .deleteProjectById(idToDelete);
    }

}
