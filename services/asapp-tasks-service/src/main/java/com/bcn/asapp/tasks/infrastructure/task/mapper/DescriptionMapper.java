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

import org.mapstruct.Mapper;

import com.bcn.asapp.tasks.domain.task.Description;

/**
 * MapStruct mapper for converting between {@link Description} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface DescriptionMapper {

    /**
     * Maps a {@link String} description value to a {@link Description} value object.
     *
     * @param description the description value
     * @return the {@link Description}
     */
    Description toDescription(String description);

    /**
     * Maps a {@link Description} value object to a {@link String} description value.
     *
     * @param description the {@link Description}
     * @return the description value, or {@code null} if description is {@code null}
     */
    default String toString(Description description) {
        return description != null ? description.value() : null;
    }

}
