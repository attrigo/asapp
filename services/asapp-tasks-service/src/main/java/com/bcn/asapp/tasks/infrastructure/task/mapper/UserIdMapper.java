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

import com.bcn.asapp.tasks.domain.task.UserId;

/**
 * MapStruct mapper for converting between {@link UserId} and {@link UUID}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface UserIdMapper {

    /**
     * Maps a {@link UUID} to a {@link UserId} value object.
     *
     * @param id the {@link UUID}
     * @return the {@link UserId}
     */
    UserId toUserId(UUID id);

    /**
     * Maps a {@link UserId} value object to a {@link UUID}.
     *
     * @param userId the {@link UserId}
     * @return the {@link UUID}, or {@code null} if userId is {@code null}
     */
    default UUID toUUID(UserId userId) {
        return userId != null ? userId.value() : null;
    }

}
