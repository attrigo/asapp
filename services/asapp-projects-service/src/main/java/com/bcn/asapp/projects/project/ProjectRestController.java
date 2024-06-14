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

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bcn.asapp.dto.project.ProjectDTO;

/**
 * Default implementation of {@link ProjectRestAPI}.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@RestController
public class ProjectRestController implements ProjectRestAPI {

    private static final Logger log = LoggerFactory.getLogger(ProjectRestController.class);

    private final ProjectService projectService;

    /**
     * Default constructor.
     *
     * @param projectService the service that brings project's business operations, must not be {@literal null}.
     */
    public ProjectRestController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public ResponseEntity<ProjectDTO> getProjectById(UUID id) {
        log.info("getProjectById");
        return projectService.findById(id)
                             .map(ResponseEntity::ok)
                             .orElseGet(() -> ResponseEntity.notFound()
                                                            .build());
    }

    @Override
    public List<ProjectDTO> getAllProjects() {
        log.info("getAllProjects");
        return projectService.findAll();
    }

    @Override
    public ProjectDTO createProject(ProjectDTO project) {
        log.info("createProject");
        return projectService.create(project);
    }

    @Override
    public ResponseEntity<ProjectDTO> updateProjectById(UUID id, ProjectDTO newProjectData) {
        log.info("updateProjectById");
        return projectService.updateById(id, newProjectData)
                             .map(ResponseEntity::ok)
                             .orElseGet(() -> ResponseEntity.notFound()
                                                            .build());
    }

    @Override
    public ResponseEntity<Void> deleteProjectById(UUID id) {
        log.info("deleteProjectById");
        boolean projectHasBeenDeleted = projectService.deleteById(id);
        return ResponseEntity.status(Boolean.TRUE.equals(projectHasBeenDeleted) ? HttpStatus.NO_CONTENT : HttpStatus.NOT_FOUND)
                             .build();
    }

}
