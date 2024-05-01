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

import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_CREATE_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_DELETE_BY_ID_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_ALL_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_GET_BY_ID_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_ROOT_PATH;
import static com.bcn.asapp.url.project.ProjectRestAPIURL.PROJECTS_UPDATE_BY_ID_PATH;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.bcn.asapp.dto.project.ProjectDTO;

/**
 * Defines the endpoints to handle project requests and responses.
 * <p>
 * The web layer relies on Spring Web MVC to manage requests and responses.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@Tag(name = "Projects operations", description = "Defines the endpoints to handle project requests")
@RequestMapping(PROJECTS_ROOT_PATH)
public interface ProjectRestAPI {

    /**
     * Gets a project by id.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK : Project has been found.</li>
     * <li>404-NOT_FOUND : Project not found.</li>
     * </ul>
     *
     * @param id the id of the project to get.
     * @return a {@link ResponseEntity} wrapping the project found, or wrapping empty if the given id has not been found.
     */
    @Operation(summary = "Get a project by id", description = "Returns the project found, or empty if the id has not been found")
    @ApiResponse(responseCode = "200", description = "Project has been found", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    @GetMapping(value = PROJECTS_GET_BY_ID_PATH, produces = "application/json")
    ResponseEntity<ProjectDTO> getProjectById(@Parameter(description = "Id of the project to get") @PathVariable("id") UUID id);

    /**
     * Gets all projects.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK : Projects found.</li>
     * </ul>
     *
     * @return all projects found, or an empty list if there aren't projects.
     */
    @Operation(summary = "Get all projects", description = "Returns all projects, or an empty list if there aren't projects")
    @ApiResponse(responseCode = "200", description = "Projects found", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @GetMapping(value = PROJECTS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    List<ProjectDTO> getAllProjects();

    /**
     * Creates a project.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED : Project has been created.</li>
     * </ul>
     *
     * @param project the project to create.
     * @return the created project.
     */
    @Operation(summary = "Create a project", description = "Returns the created project")
    @ApiResponse(responseCode = "201", description = "Project has been created", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @PostMapping(value = PROJECTS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    ProjectDTO createProject(@Valid @RequestBody ProjectDTO project);

    /**
     * Updates a project by id.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK : Project has been updated.</li>
     * <li>404-NOT_FOUND : Project not found.</li>
     * </ul>
     *
     * @param id             the id of the project to update.
     * @param newProjectData the new project data.
     * @return a {@link ResponseEntity} wrapping the updated project, or empty if the given id has not been found.
     */
    @Operation(summary = "Update a project", description = "Returns the updated project, or empty if the given id has not been found")
    @ApiResponse(responseCode = "200", description = "Project has been updated", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    @PutMapping(value = PROJECTS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    ResponseEntity<ProjectDTO> updateProjectById(@Parameter(description = "Identifier of the project to update") @PathVariable("id") UUID id,
            @Valid @RequestBody ProjectDTO newProjectData);

    /**
     * Deletes a project by id.
     * <p>
     * Response codes:
     * <ul>
     * <li>204-NO_CONTENT : Project has been deleted.</li>
     * <li>404-NOT_FOUND : Project not found.</li>
     * </ul>
     *
     * @param id the id of the project to delete.
     * @return a {@link ResponseEntity} wrapping empty.
     */
    @Operation(summary = "Delete a project by id", description = "Returns empty")
    @ApiResponse(responseCode = "204", description = "Project has been deleted", content = { @Content })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    @DeleteMapping(value = PROJECTS_DELETE_BY_ID_PATH, produces = "application/json")
    ResponseEntity<Void> deleteProjectById(@Parameter(description = "Id of the project to delete") @PathVariable("id") UUID id);

}
