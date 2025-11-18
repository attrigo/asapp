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

package com.bcn.asapp.tasks.infrastructure.task.mapper;

import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.tasks.domain.task.Description;
import com.bcn.asapp.tasks.domain.task.EndDate;
import com.bcn.asapp.tasks.domain.task.StartDate;
import com.bcn.asapp.tasks.domain.task.Task;
import com.bcn.asapp.tasks.domain.task.TaskId;
import com.bcn.asapp.tasks.domain.task.Title;
import com.bcn.asapp.tasks.domain.task.UserId;
import com.bcn.asapp.tasks.infrastructure.task.persistence.JdbcTaskEntity;

/**
 * MapStruct object factory for mapping between {@link Task} domain entities and {@link JdbcTaskEntity} database entities.
 * <p>
 * Ensures that domain entities are created through their proper factory methods with complete validation, maintaining domain integrity during mapping.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class TaskObjectFactory {

    private final TaskIdMapper taskIdMapper;

    private final UserIdMapper userIdMapper;

    private final TitleMapper titleMapper;

    private final DescriptionMapper descriptionMapper;

    private final StartDateMapper startDateMapper;

    private final EndDateMapper endDateMapper;

    /**
     * Constructs a new {@code TaskObjectFactory} with required mappers.
     *
     * @param taskIdMapper      the mapper for task IDs
     * @param userIdMapper      the mapper for user IDs
     * @param titleMapper       the mapper for titles
     * @param descriptionMapper the mapper for descriptions
     * @param startDateMapper   the mapper start date
     * @param endDateMapper     the mapper for end dates
     */
    public TaskObjectFactory(TaskIdMapper taskIdMapper, UserIdMapper userIdMapper, TitleMapper titleMapper, DescriptionMapper descriptionMapper,
            StartDateMapper startDateMapper, EndDateMapper endDateMapper) {

        this.taskIdMapper = taskIdMapper;
        this.userIdMapper = userIdMapper;
        this.titleMapper = titleMapper;
        this.descriptionMapper = descriptionMapper;
        this.startDateMapper = startDateMapper;
        this.endDateMapper = endDateMapper;
    }

    /**
     * Creates a domain {@link Task} from a database {@link JdbcTaskEntity} entity.
     * <p>
     * Maps entity fields to value objects and reconstitutes a task using the domain's factory method.
     *
     * @param source the {@link JdbcTaskEntity} database entity
     * @return the {@link Task} domain entity
     */
    @ObjectFactory
    public Task toTask(JdbcTaskEntity source) {
        var taskId = TaskId.of(source.id());
        var userId = UserId.of(source.userId());
        var title = Title.of(source.title());
        var description = Description.ofNullable(source.description());
        var startDate = StartDate.ofNullable(source.startDate());
        var endDate = EndDate.ofNullable(source.endDate());

        return Task.reconstitute(taskId, userId, title, description, startDate, endDate);
    }

}
