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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bcn.asapp.clients.client.task.TaskClient;
import com.bcn.asapp.dto.project.ProjectDTO;
import com.bcn.asapp.projects.project.Project;
import com.bcn.asapp.projects.project.ProjectMapper;
import com.bcn.asapp.projects.project.ProjectRepository;
import com.bcn.asapp.projects.project.ProjectService;

/**
 * Standard implementation of {@link ProjectService}.
 *
 * @author ttrigo
 * @since 0.1.0
 */

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    private final ProjectMapper projectMapper;

    private final TaskClient taskClient;

    /**
     * Main constructor.
     *
     * @param projectMapper     the mapper to map between {@link ProjectDTO} and {@link Project} and.
     * @param projectRepository the repository to access project's data.
     * @param taskClient        the HTTP client to call tasks service.
     */
    public ProjectServiceImpl(ProjectMapper projectMapper, ProjectRepository projectRepository, TaskClient taskClient) {
        this.projectMapper = projectMapper;
        this.projectRepository = projectRepository;
        this.taskClient = taskClient;
    }

    @Override
    public Optional<ProjectDTO> findById(UUID id) {
        return this.projectRepository.findById(id)
                                     .map(project -> {
                                         var tasks = taskClient.getTasksByProjectId(project.id());
                                         return projectMapper.toProjectDTO(project, tasks);
                                     });
    }

    @Override
    public List<ProjectDTO> findAll() {
        return projectRepository.findAll()
                                .stream()
                                .map(project -> {
                                    var tasks = taskClient.getTasksByProjectId(project.id());
                                    return projectMapper.toProjectDTO(project, tasks);
                                })
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
