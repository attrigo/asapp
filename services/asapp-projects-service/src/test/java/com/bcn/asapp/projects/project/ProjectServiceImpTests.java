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
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.dtos.project.ProjectDTO;

@ExtendWith(SpringExtension.class)
class ProjectServiceImpTests {

    @Mock
    private ProjectRepository projectRepositoryMock;

    @Spy
    private ProjectMapperImpl projectMapperSpy;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private LocalDateTime fakeProjectStartDate;

    @BeforeEach
    void beforeEach() {
        given(projectMapperSpy.toProjectDTO(any(Project.class))).willCallRealMethod();
        given(projectMapperSpy.toProject(any(ProjectDTO.class))).willCallRealMethod();
        given(projectMapperSpy.toProjectIgnoreId(any(ProjectDTO.class))).willCallRealMethod();

        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectTitle = "Test Title";
        this.fakeProjectDescription = "Test Description";
        this.fakeProjectStartDate = LocalDateTime.now();
    }

    // findById
    @Test
    @DisplayName("GIVEN id does not exists WHEN find project by id THEN finds the project with the given id And returns empty")
    void IdNotExists_FindById_FindsProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.empty());

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
    }

    @Test
    @DisplayName("GIVEN id exists WHEN find project by id THEN finds the project with the given id And returns the project found")
    void IdExists_FindById_FindsProjectAndReturnsProjectFound() {
        // Given
        var fakeProject = new Project(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

        // When
        var idToFind = fakeProjectId;

        var actual = projectService.findById(idToFind);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        then(projectRepositoryMock).should(times(1))
                                   .findById(idToFind);
    }

    // findAll
    @Test
    @DisplayName("GIVEN there are not projects WHEN find all projects THEN does not find projects And returns empty list")
    void ThereAreNotProjects_FindAll_DoesNotFindProjectsAndReturnsEmptyList() {
        // Given
        given(projectRepositoryMock.findAll()).willReturn(Collections.emptyList());

        // When
        var actual = projectService.findAll();

        // Then
        assertTrue(actual.isEmpty());

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
    }

    @Test
    @DisplayName("GIVEN there are projects WHEN find all projects THEN finds projects And returns the projects found")
    void ThereAreProjects_FindAll_FindsProjectsAndReturnsProjectsFound() {
        var fakeProject1Id = UUID.randomUUID();
        var fakeProject2Id = UUID.randomUUID();
        var fakeProject3Id = UUID.randomUUID();

        var fakeProjectStartDate1 = LocalDateTime.now();
        var fakeProjectStartDate2 = LocalDateTime.now();
        var fakeProjectStartDate3 = LocalDateTime.now();

        // Given
        var fakeProject1 = new Project(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate1);
        var fakeProject2 = new Project(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate2);
        var fakeProject3 = new Project(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate3);
        var fakeProjects = Arrays.asList(fakeProject1, fakeProject2, fakeProject3);
        given(projectRepositoryMock.findAll()).willReturn(fakeProjects);

        // When
        var actual = projectService.findAll();

        // Then
        var expectedProject1 = new ProjectDTO(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate1);
        var expectedProject2 = new ProjectDTO(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate2);
        var expectedProject3 = new ProjectDTO(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate3);
        var expected = Arrays.asList(expectedProject1, expectedProject2, expectedProject3);

        assertIterableEquals(expected, actual);

        then(projectRepositoryMock).should(times(1))
                                   .findAll();
    }

    // Create
    @Test
    @DisplayName("GIVEN project id is not null WHEN create a project THEN creates the project ignoring the given project id And returns the project created with a new id")
    void ProjectIdIsNotNull_Create_CreatesProjectIgnoringGivenProjectIdAndReturnsProjectCreated() {
        var newFakeProjectId = UUID.randomUUID();

        // Given
        var fakeProject = new Project(newFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var projectToCreate = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var actual = projectService.create(projectToCreate);

        // Then
        var expected = new ProjectDTO(newFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        assertEquals(expected, actual);

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertNull(projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN project id is null WHEN create a project THEN creates the project And returns the project created with a new id")
    void ProjectIdIsNull_Create_CreatesProjectAndReturnsProjectCreated() {
        var newFakeProjectId = UUID.randomUUID();

        // Given
        var fakeProject = new Project(newFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var actual = projectService.create(projectToCreate);

        // Then
        var expected = new ProjectDTO(newFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        assertEquals(expected, actual);

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertNull(projectArgument.id());
    }

    // Update
    @Test
    @DisplayName("GIVEN id does not exists And project id is not null WHEN update a project by id THEN does not update the project And returns empty")
    void IdNotExistsAndProjectIdIsNotNull_UpdateById_DoesNotUpdateProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(never())
                                   .save(any(Project.class));
    }

    @Test
    @DisplayName("GIVEN id does not exists And project id is null WHEN update a project by id THEN does not update the project And returns empty")
    void IdNotExistsAndProjectIdIsNull_UpdateById_DoesNotUpdateProjectAndReturnsEmpty() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(false);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        assertFalse(actual.isPresent());

        then(projectRepositoryMock).should(never())
                                   .save(any(Project.class));
    }

    @Test
    @DisplayName("GIVEN id exists And project id is not null WHEN update a project by id THEN updates all fields of the project except the id And Returns the project updated with the new values")
    void IdExistsAndProjectIdIsNotNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsProjectUpdated() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeProject = new Project(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);

        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());

        ArgumentCaptor<Project> projectArgumentCaptor = ArgumentCaptor.forClass(Project.class);
        then(projectRepositoryMock).should(times(1))
                                   .save(projectArgumentCaptor.capture());
        Project projectArgument = projectArgumentCaptor.getValue();
        assertEquals(fakeProjectId, projectArgument.id());
    }

    @Test
    @DisplayName("GIVEN id exists And project id is null WHEN update a project by id THEN updates all fields of the project except the id And Returns the project updated with the new values")
    void IdExistsAndProjectIdIsNull_UpdateById_UpdatesAllFieldsExceptIdAndReturnsProjectUpdated() {
        // Given
        given(projectRepositoryMock.existsById(any(UUID.class))).willReturn(true);

        var fakeProject = new Project(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        given(projectRepositoryMock.save(any(Project.class))).willReturn(fakeProject);

        // When
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);

        var actual = projectService.updateById(idToUpdate, projectToUpdate);

        // Then
        var expected = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);

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
    @DisplayName("GIVEN id does not exists WHEN delete a project by id THEN does not delete any project And returns false")
    void IdNotExists_DeleteById_DoesNotDeleteAnyProjectAndReturnsFalse() {
        // Given
        given(projectRepositoryMock.deleteProjectById(any(UUID.class))).willReturn(0L);

        // When
        var idToDelete = fakeProjectId;

        var actual = projectService.deleteById(idToDelete);

        // Then
        Assertions.assertFalse(actual);

        then(projectRepositoryMock).should(times(1))
                                   .deleteProjectById(idToDelete);
    }

    @Test
    @DisplayName("GIVEN id exists WHEN delete a project by id THEN deletes the project with the given id And returns true")
    void IdExists_DeleteById_DeletesProjectWithGivenIdAndReturnsTrue() {
        // Given
        given(projectRepositoryMock.deleteProjectById(any(UUID.class))).willReturn(1L);

        // When
        var idToDelete = fakeProjectId;

        var actual = projectService.deleteById(idToDelete);

        // Then
        Assertions.assertTrue(actual);

        then(projectRepositoryMock).should(times(1))
                                   .deleteProjectById(idToDelete);
    }

}
