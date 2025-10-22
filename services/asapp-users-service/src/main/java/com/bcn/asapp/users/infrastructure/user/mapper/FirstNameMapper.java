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

import com.bcn.asapp.users.domain.user.FirstName;

/**
 * MapStruct mapper for converting between {@link FirstName} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface FirstNameMapper {

    /**
     * Maps a {@link String} first name value to a {@link FirstName} value object.
     *
     * @param firstName the first name value
     * @return the {@link FirstName}
     */
    FirstName toFirstName(String firstName);

    /**
     * Maps a {@link FirstName} value object to a {@link String} first name value.
     *
     * @param firstName the {@link FirstName}
     * @return the first name value, or {@code null} if first name is {@code null}
     */
    default String toString(FirstName firstName) {
        return firstName != null ? firstName.value() : null;
    }

}
