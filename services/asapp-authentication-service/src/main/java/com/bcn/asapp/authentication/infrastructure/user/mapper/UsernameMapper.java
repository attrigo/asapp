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

package com.bcn.asapp.authentication.infrastructure.user.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.authentication.domain.user.Username;

/**
 * MapStruct mapper for converting between {@link Username} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface UsernameMapper {

    /**
     * Maps a {@link String} username value to a {@link Username} value object.
     *
     * @param username the username value
     * @return the {@link Username}
     */
    Username toUsername(String username);

    /**
     * Maps a {@link Username} value object to a {@link String} username value.
     *
     * @param username the {@link Username}
     * @return the username value, or {@code null} if username is {@code null}
     */
    default String toString(Username username) {
        return username != null ? username.value() : null;
    }

}
