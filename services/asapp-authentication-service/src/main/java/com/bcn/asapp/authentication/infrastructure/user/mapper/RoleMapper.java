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

import static org.mapstruct.MappingConstants.ANY_REMAINING;
import static org.mapstruct.MappingConstants.THROW_EXCEPTION;

import org.mapstruct.Mapper;
import org.mapstruct.ValueMapping;

import com.bcn.asapp.authentication.domain.user.Role;

/**
 * MapStruct mapper for converting between {@link Role} enum and {@link String}.
 * <p>
 * Throws an exception for unmapped role values to ensure type safety.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface RoleMapper {

    /**
     * Maps a string to a role enum.
     *
     * @param role the role value
     * @return the {@link Role}
     * @throws IllegalArgumentException if the role value does not match any enum value
     */
    @ValueMapping(source = ANY_REMAINING, target = THROW_EXCEPTION)
    Role toRole(String role);

}
