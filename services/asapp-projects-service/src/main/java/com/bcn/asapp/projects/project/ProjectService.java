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
import java.util.Optional;
import java.util.UUID;

import com.bcn.asapp.dto.project.ProjectDTO;

/**
 * Defines the project business operations.
 *
 * @author ttrigo
 * @since 0.1.0
 */
public interface ProjectService {

    /**
     * Finds a project by the given id.
     * <p>
     * Gets the tasks of the project from the tasks service.
     *
     * @param id the id of the project to be found, must not be {@literal null}.
     * @return {@link Optional} wrapping the project found, or an empty {@link Optional} if the given id has not been found.
     */
    Optional<ProjectDTO> findById(UUID id);

    /**
     * Finds all projects.
     * <p>
     * Gets the tasks of the project from the tasks service.
     *
     * @return a list with all projects, or an empty list if none found.
     */
    List<ProjectDTO> findAll();

    /**
     * Creates the given project.
     * <p>
     * Always creates the project with a new id, therefore in cases where the id of the given project is present it is ignored.
     * <p>
     * The given project's tasks are not created.
     *
     * @param project the project to be created, must be a valid project.
     * @return the created project.
     */
    ProjectDTO create(ProjectDTO project);

    /**
     * Updates the project by the given id with the given new data.
     * <p>
     * The id of the project is not updated, so in cases where the given new project has id it is ignored.
     * <p>
     * The given project's tasks are not updated.
     *
     * @param id             the id of the project to be updated, must not be {@literal null}.
     * @param newProjectData the new project data, must be a valid project.
     * @return {@link Optional} wrapping the project updated with the new data, or an empty {@link Optional} if the given id has not been found.
     */
    Optional<ProjectDTO> updateById(UUID id, ProjectDTO newProjectData);

    /**
     * Deletes a project by the given id.
     * <p>
     * The related tasks are not deleted.
     *
     * @param id the id of the project to be deleted, must not be {@literal null}.
     * @return true if the given id exists, otherwise false.
     */
    Boolean deleteById(UUID id);

}
