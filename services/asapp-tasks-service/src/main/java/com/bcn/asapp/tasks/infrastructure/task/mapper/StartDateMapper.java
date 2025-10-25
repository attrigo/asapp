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

import com.bcn.asapp.tasks.domain.task.StartDate;

/**
 * MapStruct mapper for converting between {@link StartDate} and {@link Instant}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface StartDateMapper {

    /**
     * Maps a {@link Instant} start date value to an {@link StartDate} value object.
     *
     * @param startDate the start date instant value
     * @return the {@link StartDate}
     */
    StartDate toStartDate(Instant startDate);

    /**
     * Maps a {@link StartDate} value object to a {@link Instant} start date value.
     *
     * @param startDate date the {@link StartDate}
     * @return the start date instant value, or {@code null} if start date is {@code null}
     */
    default Instant toInstant(StartDate startDate) {
        return startDate != null ? startDate.value() : null;
    }

}
