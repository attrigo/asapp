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

import java.time.Instant;

import org.mapstruct.Mapper;

import com.bcn.asapp.tasks.domain.task.EndDate;

/**
 * MapStruct mapper for converting between {@link EndDate} and {@link Instant}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface EndDateMapper {

    /**
     * Maps a {@link Instant} end date value to an {@link EndDate} value object.
     *
     * @param endDate the end date instant value
     * @return the {@link EndDate}
     */
    EndDate toEndDate(Instant endDate);

    /**
     * Maps a {@link EndDate} value object to a {@link Instant} end date value.
     *
     * @param endDate date the {@link EndDate}
     * @return the end date instant value, or {@code null} if end date is {@code null}
     */
    default Instant toInstant(EndDate endDate) {
        return endDate != null ? endDate.value() : null;
    }

}
