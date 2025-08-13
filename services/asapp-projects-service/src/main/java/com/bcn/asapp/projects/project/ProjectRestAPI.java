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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.bcn.asapp.dto.project.ProjectDTO;

/**
 * Defines the RESTful API for handling project operations.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@RequestMapping(PROJECTS_ROOT_PATH)
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Projects operations", description = "Defines the RESTful API for handling project operations")
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
    @GetMapping(value = PROJECTS_GET_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Gets a project by id", description = "Returns the project found, or empty if the given id has not been found")
    @ApiResponse(responseCode = "200", description = "Project has been found", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    ResponseEntity<ProjectDTO> getProjectById(@PathVariable("id") @Parameter(description = "Id of the project to get") UUID id);

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
    @GetMapping(value = PROJECTS_GET_ALL_PATH, produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Gets all projects", description = "Returns all projects, or an empty list if there aren't projects")
    @ApiResponse(responseCode = "200", description = "Projects found", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    List<ProjectDTO> getAllProjects();

    /**
     * Creates a project.
     * <p>
     * Creates the given project ignoring the id and tasks fields.
     * <p>
     * The resultant project always has a new id.
     * <p>
     * Response codes:
     * <ul>
     * <li>201-CREATED : Project has been created.</li>
     * </ul>
     *
     * @param project the project to create.
     * @return the created project.
     */
    @PostMapping(value = PROJECTS_CREATE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a project", description = "Creates the given project ignoring the id and the tasks fields, the resultant project always has a new id")
    @ApiResponse(responseCode = "201", description = "Project has been created", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    ProjectDTO createProject(@RequestBody @Valid ProjectDTO project);

    /**
     * Updates a project by id.
     * <p>
     * Updates all fields of the project except the id and tasks, with the given new data.
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
    @PutMapping(value = PROJECTS_UPDATE_BY_ID_PATH, consumes = "application/json", produces = "application/json")
    @Operation(summary = "Updates a project", description = "Updates all fields of the project except the id and tasks, with the given new data")
    @ApiResponse(responseCode = "200", description = "Project has been updated", content = { @Content(schema = @Schema(implementation = ProjectDTO.class)) })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    ResponseEntity<ProjectDTO> updateProjectById(@PathVariable("id") @Parameter(description = "Identifier of the project to update") UUID id,
            @RequestBody @Valid ProjectDTO newProjectData);

    /**
     * Deletes a project by id.
     * <p>
     * The related tasks are not deleted.
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
    @DeleteMapping(value = PROJECTS_DELETE_BY_ID_PATH, produces = "application/json")
    @Operation(summary = "Deletes a project by id", description = "Deletes a project by id, the related tasks are not deleted")
    @ApiResponse(responseCode = "204", description = "Project has been deleted", content = { @Content })
    @ApiResponse(responseCode = "404", description = "Project not found", content = { @Content })
    ResponseEntity<Void> deleteProjectById(@PathVariable("id") @Parameter(description = "Id of the project to delete") UUID id);

}
