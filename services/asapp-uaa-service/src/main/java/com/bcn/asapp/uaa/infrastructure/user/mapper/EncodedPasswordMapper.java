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

package com.bcn.asapp.uaa.infrastructure.user.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.uaa.domain.user.EncodedPassword;

/**
 * MapStruct mapper for converting {@link EncodedPassword} to {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface EncodedPasswordMapper {

    /**
     * Maps a {@link EncodedPassword} value object to a {@link String} encoded password value.
     *
     * @param encodedPassword the {@link EncodedPassword}
     * @return the encoded password value, or {@code null} if encoded password is {@code null}
     */
    default String toString(EncodedPassword encodedPassword) {
        return encodedPassword != null ? encodedPassword.value() : null;
    }

}
