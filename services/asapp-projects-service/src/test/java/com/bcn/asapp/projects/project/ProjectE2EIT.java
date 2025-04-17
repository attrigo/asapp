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

import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_UPDATE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_PROJECT_ID_FULL_PATH;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.matchers.Times.once;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.dto.project.ProjectDTO;
import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.projects.AsappProjectsServiceApplication;
import com.bcn.asapp.projects.testconfig.SecurityTestConfiguration;
import com.bcn.asapp.projects.testutil.JwtTestGenerator;

@AutoConfigureWebTestClient(timeout = "30000")
@Testcontainers(disabledWithoutDocker = true, parallel = true)
@Import(SecurityTestConfiguration.class)
@SpringBootTest(classes = AsappProjectsServiceApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
class ProjectE2EIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Container
    static MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
        registry.add("asapp.tasks-service.base-url", mockServerContainer::getEndpoint);
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTestGenerator jwtTestGenerator;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private Instant fakeProjectStartDate;

    @BeforeEach
    void beforeEach() {
        projectRepository.deleteAll();

        var bearerToken = "Bearer " + jwtTestGenerator.generateJwt();
        webTestClient = webTestClient.mutate()
                                     .defaultHeader(HttpHeaders.AUTHORIZATION, bearerToken)
                                     .build();

        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectTitle = "E2E IT Title";
        this.fakeProjectDescription = "E2E IT Description";
        this.fakeProjectStartDate = Instant.now()
                                           .truncatedTo(ChronoUnit.MILLIS);
    }

    @Nested
    class GetProjectById {

        @Test
        @DisplayName("GIVEN project id does not exists WHEN get a project by id THEN does not get the project And returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_GetProjectById_DoesNotGetTheProjectAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToFind = fakeProjectId;

            webTestClient.get()
                         .uri(PROJECTS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN project id exists without tasks WHEN get a project by id THEN gets the project And returns HTTP response with status OK And the body with the project with empty list of tasks")
        void ProjectIdExistsWithoutTasks_GetProjectById_GetsProjectAndReturnsStatusOKAndBodyWithProjectWithEmptyListTasks() {
            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeFound = projectRepository.save(fakeProject);
            assertNotNull(projectToBeFound);

            mockRequestToGetTasksByProjectIdWithOkResponse(projectToBeFound.id(), Collections.emptyList());

            // When & Then
            var idToFind = projectToBeFound.id();

            var expectedProject = new ProjectDTO(idToFind, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, Collections.emptyList());

            webTestClient.get()
                         .uri(PROJECTS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(ProjectDTO.class)
                         .isEqualTo(expectedProject);
        }

        @Test
        @DisplayName("GIVEN project id exists with tasks And tasks service is not available WHEN get a project by id THEN gets the project And returns HTTP response with status OK And the body with the project without tasks")
        void ProjectIdExistsWithTasksAndTasksServiceIsNotAvailable_GetProjectById_GetsProjectAndReturnsStatusOKAndBodyWithProjectWithoutTasks() {
            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeFound = projectRepository.save(fakeProject);
            assertNotNull(projectToBeFound);

            mockRequestToGetTasksByProjectIdWithServerErrorResponse(projectToBeFound.id());

            // When & Then
            var idToFind = projectToBeFound.id();

            var expectedProject = new ProjectDTO(idToFind, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

            webTestClient.get()
                         .uri(PROJECTS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(ProjectDTO.class)
                         .isEqualTo(expectedProject);
        }

        @Test
        @DisplayName("GIVEN project id exists with tasks WHEN get a project by id THEN gets the project And returns HTTP response with status OK And the body with the project with tasks")
        void ProjectIdExistsWithTasks_GetProjectById_GetsProjectAndReturnsStatusOKAndBodyWithProjectWithTasks() {
            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeFound = projectRepository.save(fakeProject);
            assertNotNull(projectToBeFound);

            var fakeProjectTasks = buildFakeProjectTasks(projectToBeFound.id());
            mockRequestToGetTasksByProjectIdWithOkResponse(projectToBeFound.id(), fakeProjectTasks);

            // When & Then
            var idToFind = projectToBeFound.id();

            var expectedProject = new ProjectDTO(idToFind, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);

            webTestClient.get()
                         .uri(PROJECTS_GET_BY_ID_FULL_PATH, idToFind)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBody(ProjectDTO.class)
                         .isEqualTo(expectedProject);
        }

    }

    @Nested
    class GetAllProjects {

        @Test
        @DisplayName("GIVEN there are not projects WHEN get all projects THEN does not find any projects And returns HTTP response with status OK And an empty body")
        void ThereAreNotProjects_GetAllProjects_DoesNotFindProjectsAndReturnsStatusOKAndEmptyBody() {
            // When & Then
            webTestClient.get()
                         .uri(PROJECTS_GET_ALL_FULL_PATH)
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
        @DisplayName("GIVEN there are projects without tasks WHEN get all projects THEN gets all projects And returns HTTP response with status OK And the body with the projects with empty list of tasks")
        void ThereAreProjectsWithoutTasks_GetAllProjects_GetsAllProjectsAndReturnsStatusOKAndBodyWithProjectsWithEmptyListTasks() {
            var fakeProjectTitle1 = fakeProjectTitle + " 1";
            var fakeProjectTitle2 = fakeProjectTitle + " 2";
            var fakeProjectTitle3 = fakeProjectTitle + " 3";
            var fakeProjectDesc1 = fakeProjectDescription + " 1";
            var fakeProjectDesc2 = fakeProjectDescription + " 2";
            var fakeProjectDesc3 = fakeProjectDescription + " 3";

            // Given
            var fakeProject1 = new Project(null, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate);
            var fakeProject2 = new Project(null, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate);
            var fakeProject3 = new Project(null, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate);
            var projectsToBeFound = projectRepository.saveAll(List.of(fakeProject1, fakeProject2, fakeProject3));
            var projectIdsToBeFound = projectsToBeFound.stream()
                                                       .map(Project::id)
                                                       .collect(Collectors.toList());
            assertNotNull(projectIdsToBeFound);
            assertEquals(3L, projectIdsToBeFound.size());

            var projectIdToBeFound1 = projectIdsToBeFound.getFirst();
            var projectIdToBeFound2 = projectIdsToBeFound.get(1);
            var projectIdToBeFound3 = projectIdsToBeFound.getLast();

            mockRequestToGetTasksByProjectIdWithServerErrorResponse(projectIdToBeFound1);
            mockRequestToGetTasksByProjectIdWithServerErrorResponse(projectIdToBeFound2);
            mockRequestToGetTasksByProjectIdWithServerErrorResponse(projectIdToBeFound3);

            // When & Then
            var expectedProject1 = new ProjectDTO(projectIdToBeFound1, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate, null);
            var expectedProject2 = new ProjectDTO(projectIdToBeFound2, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate, null);
            var expectedProject3 = new ProjectDTO(projectIdToBeFound3, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate, null);
            var expectedProjects = List.of(expectedProject1, expectedProject2, expectedProject3);

            webTestClient.get()
                         .uri(PROJECTS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBodyList(ProjectDTO.class)
                         .hasSize(3)
                         .isEqualTo(expectedProjects);
        }

        @Test
        @DisplayName("GIVEN there are projects with tasks And tasks service is not available WHEN get all projects THEN gets all projects And returns HTTP response with status OK And the body with the projects without tasks")
        void ThereAreProjectsWithTasksAndTasksServiceIsNotAvailable_GetAllProjects_GetsAllProjectsAndReturnsStatusOKAndBodyWithProjectsWithoutTasks() {
            var fakeProjectTitle1 = fakeProjectTitle + " 1";
            var fakeProjectTitle2 = fakeProjectTitle + " 2";
            var fakeProjectTitle3 = fakeProjectTitle + " 3";
            var fakeProjectDesc1 = fakeProjectDescription + " 1";
            var fakeProjectDesc2 = fakeProjectDescription + " 2";
            var fakeProjectDesc3 = fakeProjectDescription + " 3";

            // Given
            var fakeProject1 = new Project(null, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate);
            var fakeProject2 = new Project(null, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate);
            var fakeProject3 = new Project(null, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate);
            var projectsToBeFound = projectRepository.saveAll(List.of(fakeProject1, fakeProject2, fakeProject3));
            var projectIdsToBeFound = projectsToBeFound.stream()
                                                       .map(Project::id)
                                                       .collect(Collectors.toList());
            assertNotNull(projectIdsToBeFound);
            assertEquals(3L, projectIdsToBeFound.size());

            var projectIdToBeFound1 = projectIdsToBeFound.getFirst();
            var projectIdToBeFound2 = projectIdsToBeFound.get(1);
            var projectIdToBeFound3 = projectIdsToBeFound.getLast();

            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound1, Collections.emptyList());
            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound2, Collections.emptyList());
            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound3, Collections.emptyList());

            // When & Then
            var expectedProject1 = new ProjectDTO(projectIdToBeFound1, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate, Collections.emptyList());
            var expectedProject2 = new ProjectDTO(projectIdToBeFound2, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate, Collections.emptyList());
            var expectedProject3 = new ProjectDTO(projectIdToBeFound3, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate, Collections.emptyList());
            var expectedProjects = List.of(expectedProject1, expectedProject2, expectedProject3);

            webTestClient.get()
                         .uri(PROJECTS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBodyList(ProjectDTO.class)
                         .hasSize(3)
                         .isEqualTo(expectedProjects);
        }

        @Test
        @DisplayName("GIVEN there are projects with tasks WHEN get all projects THEN gets all projects And returns HTTP response with status OK And the body with the projects with tasks")
        void ThereAreProjectsWithTasks_GetAllProjects_GetsAllProjectsAndReturnsStatusOKAndBodyWithProjectsWithTasks() {
            var fakeProjectTitle1 = fakeProjectTitle + " 1";
            var fakeProjectTitle2 = fakeProjectTitle + " 2";
            var fakeProjectTitle3 = fakeProjectTitle + " 3";
            var fakeProjectDesc1 = fakeProjectDescription + " 1";
            var fakeProjectDesc2 = fakeProjectDescription + " 2";
            var fakeProjectDesc3 = fakeProjectDescription + " 3";

            // Given
            var fakeProject1 = new Project(null, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate);
            var fakeProject2 = new Project(null, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate);
            var fakeProject3 = new Project(null, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate);
            var projectsToBeFound = projectRepository.saveAll(List.of(fakeProject1, fakeProject2, fakeProject3));
            var projectIdsToBeFound = projectsToBeFound.stream()
                                                       .map(Project::id)
                                                       .collect(Collectors.toList());
            assertNotNull(projectIdsToBeFound);
            assertEquals(3L, projectIdsToBeFound.size());

            var projectIdToBeFound1 = projectIdsToBeFound.getFirst();
            var projectIdToBeFound2 = projectIdsToBeFound.get(1);
            var projectIdToBeFound3 = projectIdsToBeFound.getLast();

            var fakeProjectTasks1 = buildFakeProjectTasks(projectIdToBeFound1);
            var fakeProjectTasks2 = buildFakeProjectTasks(projectIdToBeFound2);
            var fakeProjectTasks3 = buildFakeProjectTasks(projectIdToBeFound3);

            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound1, fakeProjectTasks1);
            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound2, fakeProjectTasks2);
            mockRequestToGetTasksByProjectIdWithOkResponse(projectIdToBeFound3, fakeProjectTasks3);

            // When & Then
            var expectedProject1 = new ProjectDTO(projectIdToBeFound1, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate, fakeProjectTasks1);
            var expectedProject2 = new ProjectDTO(projectIdToBeFound2, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate, fakeProjectTasks2);
            var expectedProject3 = new ProjectDTO(projectIdToBeFound3, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate, fakeProjectTasks3);
            var expectedProjects = List.of(expectedProject1, expectedProject2, expectedProject3);

            webTestClient.get()
                         .uri(PROJECTS_GET_ALL_FULL_PATH)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isOk()
                         .expectHeader()
                         .contentType(MediaType.APPLICATION_JSON)
                         .expectBodyList(ProjectDTO.class)
                         .hasSize(3)
                         .isEqualTo(expectedProjects);
        }

    }

    @Nested
    class CreateProject {

        @Test
        @DisplayName("GIVEN project has id field WHEN create a project THEN creates the project ignoring the given project id And returns HTTP response with status CREATED And the body with the project created")
        void ProjectHasIdField_CreateProject_CreatesProjectIgnoringIdAndReturnsStatusCreatedAndBodyWithProjectCreated() {
            var anotherFakeProjectId = UUID.randomUUID();

            // When & Then
            var projectToCreate = new ProjectDTO(anotherFakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

            var response = webTestClient.post()
                                        .uri(PROJECTS_CREATE_FULL_PATH)
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
                                        .value(project -> assertThat(project.tasks(), nullValue()))
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

        @Test
        @DisplayName("GIVEN project has tasks field WHEN create a project THEN creates the project ignoring the given project tasks And returns HTTP response with status CREATED And the body with the project created")
        void ProjectHasTasksField_CreateProject_CreatesProjectIgnoringTasksAndReturnsStatusCreatedAndBodyWithProjectCreated() {
            var fakeProjectTasks = buildFakeProjectTasks(null);

            // When & Then
            var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);

            var response = webTestClient.post()
                                        .uri(PROJECTS_CREATE_FULL_PATH)
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
                                        .value(project -> assertThat(project.tasks(), nullValue()))
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

        @Test
        @DisplayName("GIVEN project fields are valid WHEN create a project THEN creates the project And returns HTTP response with status CREATED And the body with the project created")
        void ProjectFieldsAreValid_CreateProject_CreatesProjectAndReturnsStatusCreatedAndBodyWithProjectCreated() {
            // When & Then
            var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

            var response = webTestClient.post()
                                        .uri(PROJECTS_CREATE_FULL_PATH)
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
                                        .value(project -> assertThat(project.tasks(), nullValue()))
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

    }

    @Nested
    class UpdateProjectById {

        @Test
        @DisplayName("GIVEN project id does not exists WHEN update a project by id THEN does not update the project And returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_UpdateProjectById_DoesNotUpdateTheProjectAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);

            webTestClient.put()
                         .uri(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
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
        @DisplayName("GIVEN project id exists And new project data has id field WHEN update a project by id THEN updates all project fields except the id And returns HTTP response with status OK And the body with the project updated")
        void ProjectIdExistsAndNewProjectDataHasIdField_UpdateProjectById_UpdatesAllProjectFieldsExceptIdAndReturnsStatusOkAndBodyWithProjectUpdated() {
            var anotherFakeProjectTitle = fakeProjectTitle + " 2";
            var anotherFakeProjectDesc = fakeProjectDescription + " 2";
            var anotherFakeProjectStartDate = Instant.now()
                                                     .truncatedTo(ChronoUnit.MILLIS);

            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeUpdated = projectRepository.save(fakeProject);
            assertNotNull(projectToBeUpdated);

            // When & Then
            var idToUpdate = projectToBeUpdated.id();
            var projectToUpdate = new ProjectDTO(UUID.randomUUID(), anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate, null);

            var expectedProjectDTO = new ProjectDTO(idToUpdate, anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate, null);

            var response = webTestClient.put()
                                        .uri(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(projectToUpdate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(ProjectDTO.class)
                                        .isEqualTo(expectedProjectDTO)
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

        @Test
        @DisplayName("GIVEN project id exists And new project data has tasks field WHEN update a project by id THEN updates all project fields except the tasks And returns HTTP response with status OK And the body with the project updated")
        void ProjectIdExistsAndNewProjectDataHasTasksField_UpdateProject_UpdatesAllProjectFieldsExceptTasksAndReturnsStatusOkAndBodyWithProjectUpdated() {
            var anotherFakeProjectTitle = fakeProjectTitle + " 2";
            var anotherFakeProjectDesc = fakeProjectDescription + " 2";
            var anotherFakeProjectStartDate = Instant.now()
                                                     .truncatedTo(ChronoUnit.MILLIS);
            var anotherFakeProjectTasks = buildFakeProjectTasks(null);

            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeUpdated = projectRepository.save(fakeProject);
            assertNotNull(projectToBeUpdated);

            // When & Then
            var idToUpdate = projectToBeUpdated.id();
            var projectToUpdate = new ProjectDTO(UUID.randomUUID(), anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate,
                    anotherFakeProjectTasks);

            var expectedProjectDTO = new ProjectDTO(idToUpdate, anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate, null);

            var response = webTestClient.put()
                                        .uri(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(projectToUpdate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(ProjectDTO.class)
                                        .isEqualTo(expectedProjectDTO)
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

        @Test
        @DisplayName("GIVEN project id exists And new project data fields are valid WHEN update a project THEN updates all project fields And returns HTTP response with status OK And the body with the project updated")
        void ProjectIdExistsAndNewProjectDataFieldsAreValid_UpdateProject_UpdatesAllProjectFieldsAndReturnsStatusOkAndBodyWithProjectUpdated() {
            var anotherFakeProjectTitle = fakeProjectTitle + " 2";
            var anotherFakeProjectDesc = fakeProjectDescription + " 2";
            var anotherFakeProjectStartDate = Instant.now()
                                                     .truncatedTo(ChronoUnit.MILLIS);

            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeUpdated = projectRepository.save(fakeProject);
            assertNotNull(projectToBeUpdated);

            // When & Then
            var idToUpdate = projectToBeUpdated.id();
            var projectToUpdate = new ProjectDTO(null, anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate, null);

            var expectedProjectDTO = new ProjectDTO(idToUpdate, anotherFakeProjectTitle, anotherFakeProjectDesc, anotherFakeProjectStartDate, null);

            var response = webTestClient.put()
                                        .uri(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate)
                                        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                        .bodyValue(projectToUpdate)
                                        .exchange()
                                        .expectStatus()
                                        .isOk()
                                        .expectHeader()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .expectBody(ProjectDTO.class)
                                        .isEqualTo(expectedProjectDTO)
                                        .returnResult()
                                        .getResponseBody();

            assertNotNull(response);
            var expectedProject = new Project(response.id(), response.title(), response.description(), response.startDateTime());
            var actualProject = projectRepository.findById(response.id());
            assertTrue(actualProject.isPresent());
            assertEquals(expectedProject, actualProject.get());
        }

    }

    @Nested
    class DeleteProjectById {

        @Test
        @DisplayName("GIVEN project id does not exists WHEN delete a project by id THEN does not delete the project And returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_DeleteProjectById_DoesNotDeleteProjectAndReturnsStatusNotFoundAndWithoutBody() {
            // When & Then
            var idToDelete = UUID.randomUUID();

            webTestClient.delete()
                         .uri(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete)
                         .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                         .exchange()
                         .expectStatus()
                         .isNotFound()
                         .expectBody()
                         .isEmpty();
        }

        @Test
        @DisplayName("GIVEN project id exists WHEN delete a project by id THEN deletes the project And returns HTTP response with status NO_CONTENT And without body")
        void ProjectIdExists_DeleteProjectById_DeletesProjectAndReturnsStatusNoContentAndWithoutBody() {
            // Given
            var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
            var projectToBeDeleted = projectRepository.save(fakeProject);
            assertNotNull(projectToBeDeleted);

            // When & Then
            var idToDelete = projectToBeDeleted.id();

            webTestClient.delete()
                         .uri(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete)
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

    private List<TaskDTO> buildFakeProjectTasks(UUID projectId) {
        var fakeTaskStartDate = Instant.now()
                                       .truncatedTo(ChronoUnit.MILLIS);
        var fakeProjectTask1 = new TaskDTO(UUID.randomUUID(), "E2E IT Task Title 1", "E2E IT Task Description 1", fakeTaskStartDate, projectId);
        var fakeProjectTask2 = new TaskDTO(UUID.randomUUID(), "E2E IT Task Title 2", "E2E IT Task Description 2", fakeTaskStartDate, projectId);
        return List.of(fakeProjectTask1, fakeProjectTask2);
    }

    private void mockRequestToGetTasksByProjectIdWithOkResponse(UUID projectId, List<TaskDTO> fakeTasks) {
        String fakeProjectTasksAsJson = null;

        try {
            fakeProjectTasksAsJson = objectMapper.writeValueAsString(fakeTasks);
        } catch (JsonProcessingException ignored) {}

        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_PROJECT_ID_FULL_PATH)
                               .withPathParameter("id", projectId.toString());
        var response = response().withStatusCode(200)
                                 .withBody(fakeProjectTasksAsJson)
                                 .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON);
        mockServerClient.when(request, once())
                        .respond(response);
    }

    private void mockRequestToGetTasksByProjectIdWithServerErrorResponse(UUID projectId) {
        var request = request().withMethod(HttpMethod.GET.name())
                               .withPath(TASKS_GET_BY_PROJECT_ID_FULL_PATH)
                               .withPathParameter("id", projectId.toString());
        var response = response().withStatusCode(500)
                                 .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON);
        mockServerClient.when(request, once())
                        .respond(response);
    }

}
