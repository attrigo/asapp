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

import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_ROOT_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.dtos.project.ProjectDTO;
import com.bcn.asapp.projects.config.JacksonMapperConfiguration;

@WebMvcTest(ProjectRestController.class)
@Import(JacksonMapperConfiguration.class)
class ProjectControllerIT {

    public static final String PROJECTS_BY_ID_PATH = PROJECTS_ROOT_PATH + "/";

    @Value("${spring.mvc.format.date-time}")
    private String dateTimeFormat;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectServiceMock;

    private UUID fakeProjectId;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private LocalDateTime fakeProjectStartDate;

    private String fakeProjectStartDateFormatted;

    @BeforeEach
    void beforeEach() {
        this.fakeProjectId = UUID.randomUUID();
        this.fakeProjectStartDate = LocalDateTime.now()
                                                 .truncatedTo(ChronoUnit.MILLIS);
        this.fakeProjectTitle = "IT Title";
        this.fakeProjectDescription = "IT Description";
        this.fakeProjectStartDateFormatted = DateTimeFormatter.ofPattern(dateTimeFormat)
                                                              .format(fakeProjectStartDate);
    }

    // getProjectById
    @Test
    @DisplayName("GIVEN id is empty WHEN get a project by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_GetProjectById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var requestBuilder = get(PROJECTS_BY_ID_PATH);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Not Found")))
               .andExpect(jsonPath("$.status", is(404)))
               .andExpect(jsonPath("$.detail", is("No static resource v1/projects.")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH)));
    }

    @Test
    @DisplayName("GIVEN id is not a valid UUID WHEN get a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_GetProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToFound = 1L;

        var requestBuilder = get(PROJECTS_BY_ID_PATH + idToFound);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToFound)));
    }

    @Test
    @DisplayName("GIVEN id does not exists WHEN get a project by id THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_GetProjectById_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
        // Given
        given(projectServiceMock.findById(any(UUID.class))).willReturn(Optional.empty());

        // When & Then
        var idToFind = fakeProjectId;

        var requestBuilder = get(PROJECTS_BY_ID_PATH + idToFind);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("GIVEN id exists WHEN get a project by id THEN returns HTTP response with status OK And the body with the project found")
    void IdExists_GetProjectById_ReturnsStatusOkAndBodyWithProjectFound() throws Exception {
        // Given
        var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectServiceMock.findById(any(UUID.class))).willReturn(Optional.of(fakeProject));

        // When & Then
        var idToFind = fakeProjectId;

        var requestBuilder = get(PROJECTS_BY_ID_PATH + idToFind);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
               .andExpect(jsonPath("$.title", is(fakeProjectTitle)))
               .andExpect(jsonPath("$.description", is(fakeProjectDescription)))
               .andExpect(jsonPath("$.startDateTime", is(fakeProjectStartDateFormatted)));
    }

    // getAllProjects
    @Test
    @DisplayName("GIVEN there are not projects WHEN get all projects THEN returns HTTP response with status OK And an empty body")
    void ThereAreNotProjects_GetAllProjects_ReturnsStatusOkAndEmptyBody() throws Exception {
        // Given
        given(projectServiceMock.findAll()).willReturn(Collections.emptyList());

        // When & Then
        var requestBuilder = get(PROJECTS_ROOT_PATH);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GIVEN there are projects WHEN get all projects THEN returns HTTP response with status OK And the body with all projects found")
    void ThereAreProjects_GetAllProjects_ReturnsStatusOkAndBodyWithAllProjectsFound() throws Exception {
        var fakeProject1Id = UUID.randomUUID();
        var fakeProject2Id = UUID.randomUUID();
        var fakeProject3Id = UUID.randomUUID();

        // Given
        var fakeProject1 = new ProjectDTO(fakeProject1Id, fakeProjectTitle + " 1", fakeProjectDescription + " 1", fakeProjectStartDate);
        var fakeProject2 = new ProjectDTO(fakeProject2Id, fakeProjectTitle + " 2", fakeProjectDescription + " 2", fakeProjectStartDate);
        var fakeProject3 = new ProjectDTO(fakeProject3Id, fakeProjectTitle + " 3", fakeProjectDescription + " 3", fakeProjectStartDate);
        var fakeProjects = Arrays.asList(fakeProject1, fakeProject2, fakeProject3);
        given(projectServiceMock.findAll()).willReturn(fakeProjects);

        // When & Then
        var requestBuilder = get(PROJECTS_ROOT_PATH);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$[0].id", is(fakeProject1Id.toString())))
               .andExpect(jsonPath("$[0].title", is(fakeProjectTitle + " 1")))
               .andExpect(jsonPath("$[0].description", is(fakeProjectDescription + " 1")))
               .andExpect(jsonPath("$[0].startDateTime", is(fakeProjectStartDateFormatted)))
               .andExpect(jsonPath("$[1].id", is(fakeProject2Id.toString())))
               .andExpect(jsonPath("$[1].title", is(fakeProjectTitle + " 2")))
               .andExpect(jsonPath("$[1].description", is(fakeProjectDescription + " 2")))
               .andExpect(jsonPath("$[1].startDateTime", is(fakeProjectStartDateFormatted)))
               .andExpect(jsonPath("$[2].id", is(fakeProject3Id.toString())))
               .andExpect(jsonPath("$[2].title", is(fakeProjectTitle + " 3")))
               .andExpect(jsonPath("$[2].description", is(fakeProjectDescription + " 3")))
               .andExpect(jsonPath("$[2].startDateTime", is(fakeProjectStartDateFormatted)));
    }

    // CreateProject
    @Test
    @DisplayName("GIVEN project is not a valid Json WHEN create a project THEN returns HTTP response with status Unsupported Media Type And the body with the problem details")
    void ProjectIsNotJson_CreateProject_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var projectToCreate = "";

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.TEXT_PLAIN)
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
    @DisplayName("GIVEN project is not present WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectIsNotPresent_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var projectToCreate = "";

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.APPLICATION_JSON)
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
    @DisplayName("GIVEN project has not mandatory fields WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectHasNotMandatoryFields_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var projectToCreate = new ProjectDTO(null, null, fakeProjectDescription, fakeProjectStartDate);
        var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.APPLICATION_JSON)
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
        var projectToCreate = new ProjectDTO(null, "", fakeProjectDescription, fakeProjectStartDate);
        var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.APPLICATION_JSON)
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
    @DisplayName("GIVEN project's start date has invalid format WHEN create a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectStartDateHasInvalidFormat_CreateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var projectToCreate = """
                {
                "title": "IT Title",
                "description": "IT Description",
                "startDateTime": "2011-11-11T11:11:11"
                }
                """;
        var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.APPLICATION_JSON)
                                                     .content(projectToCreateAsJson);
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
    @DisplayName("GIVEN project is valid WHEN create a project THEN returns HTTP response with status CREATED And the body with the project created")
    void ProjectIsValid_CreateProject_ReturnsStatusCreatedAndBodyWithProjectCreated() throws Exception {
        // Given
        var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        given(projectServiceMock.create(any(ProjectDTO.class))).willReturn(fakeProject);

        // When & Then
        var projectToCreate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToCreateAsJson = objectMapper.writeValueAsString(projectToCreate);

        var requestBuilder = post(PROJECTS_ROOT_PATH).contentType(MediaType.APPLICATION_JSON)
                                                     .content(projectToCreateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isCreated())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
               .andExpect(jsonPath("$.title", is(fakeProjectTitle)))
               .andExpect(jsonPath("$.description", is(fakeProjectDescription)))
               .andExpect(jsonPath("$.startDateTime", is(fakeProjectStartDateFormatted)));
    }

    // UpdateProject
    @Test
    @DisplayName("GIVEN id is empty WHEN update a project THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_UpdateProject_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH).contentType(MediaType.APPLICATION_JSON)
                                                     .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Not Found")))
               .andExpect(jsonPath("$.status", is(404)))
               .andExpect(jsonPath("$.detail", is("No static resource v1/projects.")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH)));
    }

    @Test
    @DisplayName("GIVEN id is not a valid UUID WHEN update a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_UpdateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = 1L;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)));
    }

    @Test
    @DisplayName("GIVEN project is not a valid Json WHEN update a project THEN returns HTTP response with status Unsupported Media Type And content with the problem details")
    void ProjectIsNotJson_UpdateProject_ReturnsStatusUnsupportedMediaTypeAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = "";

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.TEXT_PLAIN)
                                                                  .content(projectToUpdate);
        mockMvc.perform(requestBuilder)
               .andExpect(status().is4xxClientError())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Unsupported Media Type")))
               .andExpect(jsonPath("$.status", is(415)))
               .andExpect(jsonPath("$.detail", is("Content-Type 'text/plain' is not supported.")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)));
    }

    @Test
    @DisplayName("GIVEN project is not present WHEN update a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectIsNotPresent_UpdateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = "";

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdate);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", is("Failed to read request")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)));
    }

    @Test
    @DisplayName("GIVEN project has not mandatory fields WHEN update a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectHasNotMandatoryFields_UpdateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, null, fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)))
               .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
               .andExpect(jsonPath("$.errors[0].field", is("title")))
               .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
    }

    @Test
    @DisplayName("GIVEN project mandatory fields are empty WHEN update a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectMandatoryFieldsAreEmpty_UpdateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, "", fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", containsString("The title of the project is mandatory")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)))
               .andExpect(jsonPath("$.errors[0].entity", is("projectDTO")))
               .andExpect(jsonPath("$.errors[0].field", is("title")))
               .andExpect(jsonPath("$.errors[0].message", is("The title of the project is mandatory")));
    }

    @Test
    @DisplayName("GIVEN project's start date has invalid format WHEN update a project THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void ProjectStartDateHasInvalidFormat_UpdateProject_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = """
                {
                "title": "IT Title",
                "description": "IT Description",
                "startDateTime": "2011-11-11T11:11:11"
                }
                """;
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", containsString("Failed to read request")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToUpdate)));
    }

    @Test
    @DisplayName("GIVEN id does not exists WHEN update a project THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_UpdateProject_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
        // Given
        given(projectServiceMock.updateById(any(UUID.class), any(ProjectDTO.class))).willReturn(Optional.empty());

        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("GIVEN id exists WHEN update a project THEN returns HTTP response with status OK And the body with the project updated")
    void IdExists_UpdateProject_ReturnsStatusOkAndBodyWithProjectUpdated() throws Exception {
        var anotherFakeProjectStartDate = LocalDateTime.now()
                                                       .truncatedTo(ChronoUnit.MILLIS);
        var anotherFakeProjectStartDateFormatted = DateTimeFormatter.ofPattern(dateTimeFormat)
                                                                    .format(anotherFakeProjectStartDate);

        // Given
        var fakeProject = new ProjectDTO(fakeProjectId, fakeProjectTitle + " 2", fakeProjectDescription + " 2", anotherFakeProjectStartDate);
        given(projectServiceMock.updateById(any(UUID.class), any(ProjectDTO.class))).willReturn(Optional.of(fakeProject));

        // When & Then
        var idToUpdate = fakeProjectId;
        var projectToUpdate = new ProjectDTO(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToUpdateAsJson = objectMapper.writeValueAsString(projectToUpdate);

        var requestBuilder = put(PROJECTS_BY_ID_PATH + idToUpdate).contentType(MediaType.APPLICATION_JSON)
                                                                  .content(projectToUpdateAsJson);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.id", is(fakeProjectId.toString())))
               .andExpect(jsonPath("$.title", is(fakeProjectTitle + " 2")))
               .andExpect(jsonPath("$.description", is(fakeProjectDescription + " 2")))
               .andExpect(jsonPath("$.startDateTime", is(anotherFakeProjectStartDateFormatted)));
    }

    // DeleteProjectById
    @Test
    @DisplayName("GIVEN id is empty WHEN delete a project by id THEN returns HTTP response with status NOT_FOUND And the body with the problem details")
    void IdIsEmpty_DeleteProjectById_ReturnsStatusNotFoundAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var requestBuilder = delete(PROJECTS_BY_ID_PATH);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Not Found")))
               .andExpect(jsonPath("$.status", is(404)))
               .andExpect(jsonPath("$.detail", is("No static resource v1/projects.")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH)));
    }

    @Test
    @DisplayName("GIVEN id is not a valid UUID WHEN delete a project by id THEN returns HTTP response with status BAD_REQUEST And the body with the problem details")
    void IdIsNotUUID_DeleteProjectById_ReturnsStatusBadRequestAndBodyWithProblemDetails() throws Exception {
        // When & Then
        var idToDelete = 1L;

        var requestBuilder = delete(PROJECTS_BY_ID_PATH + idToDelete);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isBadRequest())
               .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
               .andExpect(jsonPath("$.type", is("about:blank")))
               .andExpect(jsonPath("$.title", is("Bad Request")))
               .andExpect(jsonPath("$.status", is(400)))
               .andExpect(jsonPath("$.detail", is("Failed to convert 'id' with value: '1'")))
               .andExpect(jsonPath("$.instance", is(PROJECTS_BY_ID_PATH + idToDelete)));
    }

    @Test
    @DisplayName("GIVEN id does not exists WHEN delete a project by id THEN returns HTTP response with status NOT_FOUND And the body without content")
    void IdNotExists_DeleteProjectById_ReturnsStatusNotFoundAndBodyWithoutContent() throws Exception {
        // Given
        given(projectServiceMock.deleteById(any(UUID.class))).willReturn(false);

        // When
        var idToDelete = fakeProjectId;

        var requestBuilder = delete(PROJECTS_BY_ID_PATH + idToDelete);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNotFound())
               .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    @DisplayName("GIVEN id exists WHEN delete a project by id THEN returns HTTP response with status NO_CONTENT And the body without content")
    void IdExists_DeleteProjectById_ReturnsStatusNoContentAndBodyWithoutContent() throws Exception {
        // Given
        given(projectServiceMock.deleteById(any(UUID.class))).willReturn(true);

        // When
        var idToDelete = fakeProjectId;

        var requestBuilder = delete(PROJECTS_BY_ID_PATH + idToDelete);
        mockMvc.perform(requestBuilder)
               .andExpect(status().isNoContent())
               .andExpect(jsonPath("$").doesNotExist());
    }

}
