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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.dtos.project.ProjectDTO;
import com.bcn.asapp.projects.AsappProjectsServiceApplication;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AsappProjectsServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class ProjectE2EIT {

    public static final String PROJECTS_PATH = "/v1/projects";

    public static final String PROJECTS_PATH_BY_ID = "/v1/projects/{id}";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebTestClient webTestClient;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private LocalDateTime fakeProjectStartDate;

    @BeforeEach
    void beforeEach() {
        projectRepository.deleteAll();

        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectTitle = "IT Title";
        this.fakeProjectDescription = "IT Description";
        this.fakeProjectStartDate = LocalDateTime.now()
                                                 .truncatedTo(ChronoUnit.MILLIS);
    }

    // GetProjectById
    @Test
    @DisplayName("GIVEN id does not exists WHEN get a project by id THEN returns HTTP response with status NOT_FOUND And no body")
    void IdNotExists_GetProjectById_ReturnsStatusNotFoundAndNoBody() {
        // When & Then
        var idToFind = fakeProjectId;

        webTestClient.get()
                     .uri(PROJECTS_PATH_BY_ID, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN id exists WHEN get a project by id THEN gets the project with given id And returns HTTP response with status OK And the body with the project found")
    void IdExists_GetProjectById_GetsProjectAndReturnsStatusOKAndBodyWithProjectFound() {
        // Given
        var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToBeFound = projectRepository.save(fakeProject);
        assertNotNull(projectToBeFound);

        // When & Then
        var idToFind = projectToBeFound.id();

        webTestClient.get()
                     .uri(PROJECTS_PATH_BY_ID, idToFind)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBody(ProjectDTO.class)
                     .value(project -> assertThat(project.id(), equalTo(idToFind)))
                     .value(project -> assertThat(project.title(), equalTo(fakeProjectTitle)))
                     .value(project -> assertThat(project.description(), equalTo(fakeProjectDescription)))
                     .value(project -> assertThat(project.startDateTime(), equalTo(fakeProjectStartDate)));
    }

    // GetAllProjects
    @Test
    @DisplayName("GIVEN there are not projects WHEN get all projects THEN returns HTTP response with status OK And an empty body")
    void ThereAreNotProjects_GetAllProjects_ReturnsStatusOKAndEmptyBody() {
        // When & Then
        webTestClient.get()
                     .uri(PROJECTS_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(ProjectDTO.class)
                     .hasSize(0);
    }

    @Test
    @DisplayName("GIVEN there are projects WHEN get all projects THEN gets all projects And returns HTTP response with status OK And the body with the projects found")
    void ThereAreProjects_GetAllProjects_GetsAllProjectsAndReturnsStatusOKAndBodyWithProjectsFound() {
        // Given
        var fakeProject1 = new Project(null, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var fakeProject2 = new Project(null, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var fakeProject3 = new Project(null, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var projectsToBeFound = projectRepository.saveAll(Arrays.asList(fakeProject1, fakeProject2, fakeProject3));
        var projectIdsToBeFound = projectsToBeFound.stream()
                                                   .map(Project::id)
                                                   .collect(Collectors.toList());
        assertNotNull(projectIdsToBeFound);
        assertEquals(3L, projectIdsToBeFound.size());

        // When & Then
        var expectedProject1 = new ProjectDTO(projectIdsToBeFound.get(0), fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var expectedProject2 = new ProjectDTO(projectIdsToBeFound.get(1), fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var expectedProject3 = new ProjectDTO(projectIdsToBeFound.get(2), fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var expected = Arrays.asList(expectedProject1, expectedProject2, expectedProject3);

        webTestClient.get()
                     .uri(PROJECTS_PATH)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isOk()
                     .expectHeader()
                     .contentType(MediaType.APPLICATION_JSON)
                     .expectBodyList(ProjectDTO.class)
                     .hasSize(3)
                     .isEqualTo(expected);
    }

    // CreateProject
    @Test
    @DisplayName("GIVEN project has id WHEN create a project THEN creates the project ignoring the given project id And returns HTTP response with status CREATED And the body with the project created")
    void ProjectHasId_CreateProject_CreatesProjectIgnoringGivenProjectIdAndReturnsStatusCreatedAndBodyWithProjectCreated() {
        var anotherFakeProjectId = UUID.randomUUID();

        // When & Then
        var projectToCreate = new ProjectDTO(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var response = webTestClient.post()
                                    .uri(PROJECTS_PATH)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(projectToCreate)
                                    .exchange()
                                    .expectStatus()
                                    .isCreated()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(ProjectDTO.class)
                                    .value(project -> assertThat(project.id(), allOf(not(anotherFakeProjectId), notNullValue())))
                                    .value(project -> assertThat(project.title(), equalTo(fakeProjectTitle)))
                                    .value(project -> assertThat(project.description(), equalTo(fakeProjectDescription)))
                                    .value(project -> assertThat(project.startDateTime(), equalTo(fakeProjectStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Project(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = projectRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(expected.get(), actual);
    }

    @Test
    @DisplayName("GIVEN project fields are valid WHEN create a project THEN creates the project And returns HTTP response with status CREATED And the body with the project created")
    void ProjectFieldsAreValid_CreateProject_CreatesProjectAndReturnsStatusCreatedAndBodyWithProjectCreated() {
        // When & Then
        var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);

        var response = webTestClient.post()
                                    .uri(PROJECTS_PATH)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(projectToCreate)
                                    .exchange()
                                    .expectStatus()
                                    .isCreated()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(ProjectDTO.class)
                                    .value(project -> assertThat(project.id(), notNullValue()))
                                    .value(project -> assertThat(project.title(), equalTo(fakeProjectTitle)))
                                    .value(project -> assertThat(project.description(), equalTo(fakeProjectDescription)))
                                    .value(project -> assertThat(project.startDateTime(), equalTo(fakeProjectStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Project(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = projectRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(expected.get(), actual);
    }

    // UpdateProject
    @Test
    @DisplayName("GIVEN id does not exists WHEN update a project THEN returns HTTP response with status NOT_FOUND And an empty body")
    void IdNotExists_UpdateProject_ReturnsStatusNotFoundAndEmptyBody() {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);

        webTestClient.put()
                     .uri(PROJECTS_PATH_BY_ID, idToUpdate)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                     .bodyValue(projectToUpdate)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN id exists And project has id WHEN update a project THEN updates all fields (except project's id) of the project with the given id with the given project data And returns HTTP response with status OK And the body with the project updated")
    void IdExistsAndProjectHasId_UpdateProject_UpdatesAllProjectFieldsExceptIdAndReturnsStatusOkAndBodyWithProjectUpdated() {
        var anotherFakeProjectStartDate = LocalDateTime.now()
                                                       .truncatedTo(ChronoUnit.MILLIS);

        // Given
        var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToBeUpdated = projectRepository.save(fakeProject);
        assertNotNull(projectToBeUpdated);

        // When & Then
        var idToUpdate = projectToBeUpdated.id();
        var projectToUpdate = new ProjectDTO(UUID.randomUUID(), fakeProjectTitle + " 2", fakeProjectDescription + " 2", anotherFakeProjectStartDate);

        var response = webTestClient.put()
                                    .uri(PROJECTS_PATH_BY_ID, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(projectToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(ProjectDTO.class)
                                    .value(project -> assertThat(project.id(), equalTo(projectToBeUpdated.id())))
                                    .value(project -> assertThat(project.title(), equalTo(fakeProjectTitle + " 2")))
                                    .value(project -> assertThat(project.description(), equalTo(fakeProjectDescription + " 2")))
                                    .value(project -> assertThat(project.startDateTime(), equalTo(anotherFakeProjectStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Project(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = projectRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(actual, expected.get());
    }

    @Test
    @DisplayName("GIVEN id exists And project is valid WHEN update a project THEN updates all fields of the project with the given id with the given project data And returns HTTP response with status OK And the body with the project updated")
    void IdExistsAndProjectIsValid_UpdateProject_UpdatesAllProjectFieldsAndReturnsStatusOkAndBodyWithProjectUpdated() {
        var anotherFakeProjectStartDate = LocalDateTime.now()
                                                       .truncatedTo(ChronoUnit.MILLIS);

        // Given
        var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToBeUpdated = projectRepository.save(fakeProject);
        assertNotNull(projectToBeUpdated);

        // When & Then
        var idToUpdate = projectToBeUpdated.id();
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle + " 2", fakeProjectDescription + " 2", anotherFakeProjectStartDate);

        var response = webTestClient.put()
                                    .uri(PROJECTS_PATH_BY_ID, idToUpdate)
                                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .bodyValue(projectToUpdate)
                                    .exchange()
                                    .expectStatus()
                                    .isOk()
                                    .expectHeader()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .expectBody(ProjectDTO.class)
                                    .value(project -> assertThat(project.id(), equalTo(projectToBeUpdated.id())))
                                    .value(project -> assertThat(project.title(), equalTo(fakeProjectTitle + " 2")))
                                    .value(project -> assertThat(project.description(), equalTo(fakeProjectDescription + " 2")))
                                    .value(project -> assertThat(project.startDateTime(), equalTo(anotherFakeProjectStartDate)))
                                    .returnResult()
                                    .getResponseBody();

        assertNotNull(response);

        var actual = new Project(response.id(), response.title(), response.description(), response.startDateTime());
        var expected = projectRepository.findById(response.id());
        assertTrue(expected.isPresent());
        assertEquals(actual, expected.get());
    }

    // DeleteProjectById
    @Test
    @DisplayName("GIVEN id does not exists WHEN delete a project by id THEN does not delete any project And returns HTTP response with status NOT_FOUND And an empty body")
    void IdNotExists_DeleteProjectById_DoesNotDeleteProjectAndReturnsStatusNotFoundAndEmptyBody() {
        // When & Then
        var idToDelete = UUID.randomUUID();

        webTestClient.delete()
                     .uri(PROJECTS_PATH_BY_ID, idToDelete)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNotFound()
                     .expectBody()
                     .isEmpty();
    }

    @Test
    @DisplayName("GIVEN id exists WHEN delete a project by id THEN deletes the project with the given id And returns HTTP response with status NO_CONTENT And an empty body")
    void IdExists_DeleteProjectById_DeletesProjectAndReturnsStatusNoContentAndEmptyBody() {
        // Given
        var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToBeDeleted = projectRepository.save(fakeProject);
        assertNotNull(projectToBeDeleted);

        // When & Then
        var idToDelete = projectToBeDeleted.id();

        webTestClient.delete()
                     .uri(PROJECTS_PATH_BY_ID, idToDelete)
                     .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                     .exchange()
                     .expectStatus()
                     .isNoContent()
                     .expectBody()
                     .isEmpty();

        assertFalse(projectRepository.findById(projectToBeDeleted.id())
                                     .isPresent());
    }

}
