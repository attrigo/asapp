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

package com.bcn.asapp.users.infrastructure.user.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.users.domain.user.LastName;

/**
 * MapStruct mapper for converting between {@link LastName} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface LastNameMapper {

    /**
     * Maps a {@link String} last name value to a {@link LastName} value object.
     *
     * @param lastName the last name value
     * @return the {@link LastName}
     */
    LastName toLastName(String lastName);

    /**
     * Maps a {@link LastName} value object to a {@link String} last name value.
     *
     * @param lastName the {@link LastName}
     * @return the last name value, or {@code null} if last name is {@code null}
     */
    default String toString(LastName lastName) {
        return lastName != null ? lastName.value() : null;
    }

}
