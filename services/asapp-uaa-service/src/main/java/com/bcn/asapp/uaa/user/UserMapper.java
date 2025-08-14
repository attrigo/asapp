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

package com.bcn.asapp.uaa.user;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.dto.user.UserDTO;

/**
 * Mapper interface for converting between {@link User} entities and {@link UserDTO} data transfer objects.
 * <p>
 * This interface leverages <a href="https://mapstruct.org/">MapStruct</a> for generating type-safe and performant mapping implementations at compile time.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a {@link User} entity to its corresponding {@link UserDTO}.
     *
     * @param user the {@link User} entity to convert
     * @return the mapped {@link UserDTO}
     */
    UserDTO toUserDTO(User user);

    /**
     * Maps a {@link UserDTO} to a {@link User} entity, explicitly setting the {@code id} and {@code password}.
     *
     * @param userDTO  the {@link User} entity to convert
     * @param id       the id to set on the resulting {@link User} entity
     * @param password the password to set on the resulting {@link User} entity
     * @return the mapped {@link User}
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "password", source = "password")
    User toUser(UserDTO userDTO, UUID id, String password);

    /**
     * Maps a {@link UserDTO} to a {@link User} entity while ignoring the {@code id} field.
     *
     * @param userDTO  the {@link UserDTO} to convert
     * @param password the password to set on the resulting {@link User} entity
     * @return the resulting user entity without id
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "password")
    User toUserIgnoreId(UserDTO userDTO, String password);

}
