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

import java.util.UUID;

import org.mapstruct.Mapper;

import com.bcn.asapp.tasks.domain.task.TaskId;

/**
 * MapStruct mapper for converting between {@link TaskId} and {@link UUID}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface TaskIdMapper {

    /**
     * Maps a {@link UUID} to a {@link TaskId} value object.
     *
     * @param id the {@link UUID}
     * @return the {@link TaskId}
     */
    TaskId toTaskId(UUID id);

    /**
     * Maps a {@link TaskId} value object to a {@link UUID}.
     *
     * @param taskId the {@link TaskId}
     * @return the {@link UUID}, or {@code null} if taskId is {@code null}
     */
    default UUID toUUID(TaskId taskId) {
        return taskId != null ? taskId.value() : null;
    }

}
