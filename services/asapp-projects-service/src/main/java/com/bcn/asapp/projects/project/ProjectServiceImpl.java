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

import org.springframework.stereotype.Service;

import com.bcn.asapp.dto.project.ProjectDTO;

/**
 * Default implementation of {@link ProjectService}.
 *
 * @author ttrigo
 * @since 0.1.0
 */

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    private final ProjectMapper projectMapper;

    /**
     * Default constructor.
     *
     * @param projectMapper     the project's mapper, must not be {@literal null}.
     * @param projectRepository the repository to access projects data, must not be {@literal null}.
     */
    public ProjectServiceImpl(ProjectMapper projectMapper, ProjectRepository projectRepository) {
        this.projectMapper = projectMapper;
        this.projectRepository = projectRepository;
    }

    @Override
    public Optional<ProjectDTO> findById(UUID id) {
        return this.projectRepository.findById(id)
                                     .map(projectMapper::toProjectDTO);
    }

    @Override
    public List<ProjectDTO> findAll() {
        return projectRepository.findAll()
                                .stream()
                                .map(projectMapper::toProjectDTO)
                                .toList();
    }

    @Override
    public ProjectDTO create(ProjectDTO project) {
        var projectWithoutId = projectMapper.toProjectIgnoreId(project);
        var projectCreated = projectRepository.save(projectWithoutId);

        return projectMapper.toProjectDTO(projectCreated);
    }

    @Override
    public Optional<ProjectDTO> updateById(UUID id, ProjectDTO newProjectData) {
        var projectExists = projectRepository.existsById(id);

        if (!projectExists) {
            return Optional.empty();
        }

        var project = projectMapper.toProject(newProjectData, id);
        var projectUpdated = projectRepository.save(project);

        var projectDTOUpdated = projectMapper.toProjectDTO(projectUpdated);
        return Optional.of(projectDTOUpdated);
    }

    @Override
    public Boolean deleteById(UUID id) {
        return projectRepository.deleteProjectById(id) > 0;
    }

}
