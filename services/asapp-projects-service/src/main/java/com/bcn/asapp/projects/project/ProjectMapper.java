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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.dto.project.ProjectDTO;
import com.bcn.asapp.dto.task.TaskDTO;

/**
 * MapStruct mapper for projects.
 * <p>
 * Brings multiple ways to map between {@link Project} and {@link ProjectDTO}.
 * <p>
 * This class is an interface that only contains the operations signature, the final implementation is generated during compilation time by MapStruct tool.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@Mapper(componentModel = "spring")
public interface ProjectMapper {

    /**
     * Maps all fields from a {@link Project} to {@link ProjectDTO}.
     *
     * @param project the source {@link Project}.
     * @return the {@link ProjectDTO} containing all fields mapped from {@link Project}.
     */
    ProjectDTO toProjectDTO(Project project);

    /**
     * Maps all fields from a {@link Project} to {@link ProjectDTO} and then adds the {@link TaskDTO} to the field tasks.
     *
     * @param project the source {@link Project}.
     * @param tasks   the source {@link TaskDTO}.
     * @return the {@link ProjectDTO} containing all fields mapped from {@link Project} and the list of {@link TaskDTO}.
     */
    ProjectDTO toProjectDTO(Project project, List<TaskDTO> tasks);

    /**
     * Maps all fields from a {@link ProjectDTO} to {@link Project}.
     *
     * @param projectDTO the source {@link ProjectDTO}.
     * @return the {@link Project} containing all fields mapped from {@link ProjectDTO}.
     */
    Project toProject(ProjectDTO projectDTO);

    /**
     * Maps all fields from a {@link ProjectDTO} to {@link Project} except the id which is mapped from parameter id.
     *
     * @param projectDTO the source {@link ProjectDTO}.
     * @param id         the source id.
     * @return the {@link Project} containing the fields mapped from {@link ProjectDTO} and the parameter id.
     */
    @Mapping(target = "id", source = "id")
    Project toProject(ProjectDTO projectDTO, UUID id);

    /**
     * Maps all fields from a {@link ProjectDTO} to {@link Project} except the id.
     *
     * @param projectDTO the source {@link ProjectDTO}.
     * @return the {@link Project} containing the fields mapped from {@link ProjectDTO}.
     */
    @Mapping(target = "id", ignore = true)
    Project toProjectIgnoreId(ProjectDTO projectDTO);

}
