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
package com.bcn.asapp.projects.project.internal;

import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_CREATE_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_DELETE_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_ALL_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_BY_ID_FULL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_ROOT_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_UPDATE_BY_ID_FULL_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.dto.project.ProjectDTO;
import com.bcn.asapp.dto.task.TaskDTO;
import com.bcn.asapp.projects.config.security.JwtAuthenticationEntryPoint;
import com.bcn.asapp.projects.config.security.JwtTokenProvider;
import com.bcn.asapp.projects.config.security.SecurityConfiguration;
import com.bcn.asapp.projects.project.ProjectService;

@Import(value = { SecurityConfiguration.class, JwtTokenProvider.class, JwtAuthenticationEntryPoint.class })
@WebMvcTest(ProjectRestController.class)
@WithMockUser
class ProjectControllerIT {

    public static final String PROJECTS_ROOT_PATH_WITH_FINAL_SLASH = PROJECTS_ROOT_PATH + "/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProjectService projectServiceMock;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private Instant fakeProjectStartDate;

    private List<TaskDTO> fakeProjectTasks;

    @BeforeEach
    void beforeEach() {
        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectStartDate = Instant.now()
                                           .truncatedTo(ChronoUnit.MILLIS);
        this.fakeProjectTitle = "IT Title";
        this.fakeProjectDescription = "IT Description";
        var fakeTaskStartDate = Instant.now()
                                       .truncatedTo(ChronoUnit.MILLIS);
        var fakeTask1 = new TaskDTO(UUID.randomUUID(), "IT Task Title 1", "IT Task Description 1", fakeTaskStartDate, fakeProjectId);
        var fakeTask2 = new TaskDTO(UUID.randomUUID(), "IT Task Title 2", "IT Task Description 2", fakeTaskStartDate, fakeProjectId);
        this.fakeProjectTasks = List.of(fakeTask1, fakeTask2);
    }

    @Nested
    class GetProjectById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN get project by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_GetProjectById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var idToFind = fakeProjectId;

            var requestBuilder = get(PROJECTS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id is empty WHEN get a project by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void ProjectIdIsEmpty_GetProjectById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = get(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN project id is not a valid UUID WHEN get a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectIdIsNotUUID_GetProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToFound = 1L;

            var requestBuilder = get(PROJECTS_GET_BY_ID_FULL_PATH, idToFound);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToFound)));
        }

        @Test
        @DisplayName("GIVEN project id does not exists WHEN get a project by id THEN returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_GetProjectById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(projectServiceMock.findById(any(UUID.class))).willReturn(Optional.empty());

            // When & Then
            var idToFind = fakeProjectId;

            var requestBuilder = get(PROJECTS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id exists WHEN get a project by id THEN returns HTTP response with status OK And the body with the project found")
        void ProjectIdExists_GetProjectById_ReturnsStatusOkAndBodyWithProjectFound() throws Exception {
            // Given
            var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, fakeProjectTasks);
            given(projectServiceMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

            // When & Then
            var idToFind = fakeProjectId;

            var requestBuilder = get(PROJECTS_GET_BY_ID_FULL_PATH, idToFind);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeProjectTitle)))
                   .andExpect(jsonPath("$.description", is(fakeProjectDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$.tasks", hasSize(2)));
        }

    }

    @Nested
    class GetAllProjects {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN get all projects THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_GetAllProjects_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var requestBuilder = get(PROJECTS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN there are not projects WHEN get all projects THEN returns HTTP response with status OK And an empty body")
        void ThereAreNotProjects_GetAllProjects_ReturnsStatusOkAndEmptyBody() throws Exception {
            // Given
            given(projectServiceMock.findAll()).willReturn(Collections.emptyList());

            // When & Then
            var requestBuilder = get(PROJECTS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("GIVEN there are projects WHEN get all projects THEN returns HTTP response with status OK And the body with all projects found")
        void ThereAreProjects_GetAllProjects_ReturnsStatusOkAndBodyWithProjectsFound() throws Exception {
            var fakeProjectId1 = UUID.randomUUID();
            var fakeProjectId2 = UUID.randomUUID();
            var fakeProjectId3 = UUID.randomUUID();
            var fakeProjectTitle1 = fakeProjectTitle + " 1";
            var fakeProjectTitle2 = fakeProjectTitle + " 2";
            var fakeProjectTitle3 = fakeProjectTitle + " 3";
            var fakeProjectDesc1 = fakeProjectDescription + " 1";
            var fakeProjectDesc2 = fakeProjectDescription + " 2";
            var fakeProjectDesc3 = fakeProjectDescription + " 3";

            // Given
            var fakeProject1 = new ProjectDTO(fakeProjectId1, fakeProjectTitle1, fakeProjectDesc1, fakeProjectStartDate, fakeProjectTasks);
            var fakeProject2 = new ProjectDTO(fakeProjectId2, fakeProjectTitle2, fakeProjectDesc2, fakeProjectStartDate, fakeProjectTasks);
            var fakeProject3 = new ProjectDTO(fakeProjectId3, fakeProjectTitle3, fakeProjectDesc3, fakeProjectStartDate, fakeProjectTasks);
            var fakeProjects = List.of(fakeProject1, fakeProject2, fakeProject3);
            given(projectServiceMock.findAll()).willReturn(fakeProjects);

            // When & Then
            var requestBuilder = get(PROJECTS_GET_ALL_FULL_PATH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$[0].id", is(fakeProjectId1.toString())))
                   .andExpect(jsonPath("$[0].title", is(fakeProjectTitle1)))
                   .andExpect(jsonPath("$[0].description", is(fakeProjectDesc1)))
                   .andExpect(jsonPath("$[0].startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$[0].tasks", hasSize(2)))
                   .andExpect(jsonPath("$[1].id", is(fakeProjectId2.toString())))
                   .andExpect(jsonPath("$[1].title", is(fakeProjectTitle2)))
                   .andExpect(jsonPath("$[1].description", is(fakeProjectDesc2)))
                   .andExpect(jsonPath("$[1].startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$[1].tasks", hasSize(2)))
                   .andExpect(jsonPath("$[2].id", is(fakeProjectId3.toString())))
                   .andExpect(jsonPath("$[2].title", is(fakeProjectTitle3)))
                   .andExpect(jsonPath("$[2].description", is(fakeProjectDesc3)))
                   .andExpect(jsonPath("$[2].startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$[2].tasks", hasSize(2)));
        }

    }

    @Nested
    class CreateProject {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN create a project THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_CreateProject_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project fields are not a valid Json WHEN create a project THEN returns HTTP response with status Unsupported Media Type And the body with the problem details")
        void ProjectFieldsAreNotJson_CreateProject_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToCreate = "";

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.TEXT_PLAIN)
                                                                .content(projectToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH)));
        }

        @Test
        @DisplayName("GIVEN project fields are not present WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectFieldsAreNotPresent_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToCreate = "";

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH)));
        }

        @Test
        @DisplayName("GIVEN project mandatory fields are not present WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectMandatoryFieldsAreNotPresent_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToCreate = new ProjectDTO(null, null, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH)))
                   .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
        }

        @Test
        @DisplayName("GIVEN project mandatory fields are empty WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectMandatoryFieldsAreEmpty_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToCreate = new ProjectDTO(null, "", fakeProjectDescription, fakeProjectStartDate, null);
            var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH)))
                   .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
        }

        @Test
        @DisplayName("GIVEN project start date field has invalid format WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectStartDateFieldHasInvalidFormat_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToCreate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11"
                    }
                    """;

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH)));
        }

        @Test
        @DisplayName("GIVEN project fields are valid WHEN create a project THEN returns HTTP response with status CREATED And the body with the project created")
        void ProjectFieldsAreValid_CreateProject_ReturnsStatusCreatedAndBodyWithProjectCreated() throws Exception {
            // Given
            var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            given(projectServiceMock.create(any(ProjectDTO.class))).willReturn(fakeProject);

            // When & Then
            var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

            var requestBuilder = post(PROJECTS_CREATE_FULL_PATH).contentType(MediaType.APPLICATION_JSON)
                                                                .content(projectToCreateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isCreated())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeProjectTitle)))
                   .andExpect(jsonPath("$.description", is(fakeProjectDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$.tasks").doesNotExist());
        }

    }

    @Nested
    class UpdateProjectById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN update a project by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_UpdateProjectById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id is empty WHEN update a project by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void ProjectIdIsEmpty_UpdateProjectById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH).contentType(MediaType.APPLICATION_JSON)
                                                                         .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN project id is not a valid UUID WHEN update a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectIdIsNotUUID_UpdateProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = 1L;
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN project fields are not a valid Json WHEN update a project by id THEN returns HTTP response with status Unsupported Media Type And content with the problem details")
        void NewProjectDataFieldsAreNotJson_UpdateProjectById_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = "";

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.TEXT_PLAIN)
                                                                                 .content(projectToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().is4xxClientError())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
                   .andExpect(jsonPath("$.status", is(415)))
                   .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new project data fields are not present WHEN update a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewProjectDataFieldsAreNotPresent_UpdateProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = "";

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN new project data mandatory fields are not present WHEN update a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewProjectDataMandatoryFieldsAreNotPresent_UpdateProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, null, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)))
                   .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
        }

        @Test
        @DisplayName("GIVEN new project data mandatory fields are empty WHEN update a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewProjectDataMandatoryFieldsAreEmpty_UpdateProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, "", fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)))
                   .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
                   .andExpect(jsonPath("$.errors[0].field", is("title")))
                   .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
        }

        @Test
        @DisplayName("GIVEN new project data start date field has invalid format WHEN update a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void NewProjectDataStartDateFieldHasInvalidFormat_UpdateProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = """
                    {
                    "title": "IT Title",
                    "description": "IT Description",
                    "startDateTime": "2011-11-11T11:11:11"
                    }
                    """;

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdate);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", containsString("Failed to read request")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToUpdate)));
        }

        @Test
        @DisplayName("GIVEN project id does not exists WHEN update a project by id THEN returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_UpdateProjectById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(projectServiceMock.updateById(any(UUID.class), any(ProjectDTO.class))).willReturn(Optional.empty());

            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id exists WHEN update a project by id THEN returns HTTP response with status OK And the body with the project updated")
        void ProjectIdExists_UpdateProjectById_ReturnsStatusOkAndBodyWithProjectUpdated() throws Exception {
            // Given
            var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            given(projectServiceMock.updateById(any(UUID.class), any(ProjectDTO.class))).willReturn(Optional.of(fakeProject));

            // When & Then
            var idToUpdate = fakeProjectId;
            var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate, null);
            var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

            var requestBuilder = put(PROJECTS_UPDATE_BY_ID_FULL_PATH, idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                                 .content(projectToUpdateAsJson);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                   .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
                   .andExpect(jsonPath("$.title", is(fakeProjectTitle)))
                   .andExpect(jsonPath("$.description", is(fakeProjectDescription)))
                   .andExpect(jsonPath("$.startDateTime", is(fakeProjectStartDate.toString())))
                   .andExpect(jsonPath("$.tasks").doesNotExist());
        }

    }

    @Nested
    class DeleteProjectById {

        @Test
        @WithAnonymousUser
        @DisplayName("GIVEN JWT is not present WHEN delete a project by id THEN returns HTTP response with status Unauthorized And an empty body")
        void JwtIsNotPresent_UpdateProjectById_ReturnsStatusUnauthorizedAndEmptyBody() throws Exception {
            // When & Then
            var idToDelete = fakeProjectId;

            var requestBuilder = delete(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isUnauthorized())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id is empty WHEN delete a project by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
        void ProjectIdIsEmpty_DeleteProjectById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var requestBuilder = delete(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Not Found")))
                   .andExpect(jsonPath("$.status", is(404)))
                   .andExpect(jsonPath("$.detail", startsWith("No static resource")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH)));
        }

        @Test
        @DisplayName("GIVEN project id is not a valid UUID WHEN delete a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
        void ProjectIdIsNotUUID_DeleteProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
            // When & Then
            var idToDelete = 1L;

            var requestBuilder = delete(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isBadRequest())
                   .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                   .andExpect(jsonPath("$.type", is("about:blank")))
                   .andExpect(jsonPath("$.title", is("Bad Request")))
                   .andExpect(jsonPath("$.status", is(400)))
                   .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
                   .andExpect(jsonPath("$.instance", is(PROJECTS_ROOT_PATH_WITH_FINAL_SLASH + idToDelete)));
        }

        @Test
        @DisplayName("GIVEN project id does not exists WHEN delete a project by id THEN returns HTTP response with status NOT_FOUND And without body")
        void ProjectIdNotExists_DeleteProjectById_ReturnsStatusNotFoundAndWithoutBody() throws Exception {
            // Given
            given(projectServiceMock.deleteById(any(UUID.class))).willReturn(false);

            // When
            var idToDelete = fakeProjectId;

            var requestBuilder = delete(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNotFound())
                   .andExpect(jsonPath("$").doesNotExist());
        }

        @Test
        @DisplayName("GIVEN project id exists WHEN delete a project by id THEN returns HTTP response with status NO_CONTENT And without body")
        void ProjectIdExists_DeleteProjectById_ReturnsStatusNoContentAndWithoutBody() throws Exception {
            // Given
            given(projectServiceMock.deleteById(any(UUID.class))).willReturn(true);

            // When
            var idToDelete = fakeProjectId;

            var requestBuilder = delete(PROJECTS_DELETE_BY_ID_FULL_PATH, idToDelete);
            mockMvc.perform(requestBuilder)
                   .andExpect(status().isNoContent())
                   .andExpect(jsonPath("$").doesNotExist());
        }

    }

}
