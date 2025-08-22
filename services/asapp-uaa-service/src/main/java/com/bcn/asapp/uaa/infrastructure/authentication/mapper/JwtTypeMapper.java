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

package com.bcn.asapp.uaa.infrastructure.authentication.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.uaa.domain.authentication.JwtType;

/**
 * MapStruct mapper for converting between {@link JwtType} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface JwtTypeMapper {

    /**
     * Maps a {@link String} JWT type value to a {@link JwtType} value object.
     *
     * @param jwtType the JWT type value
     * @return the {@link JwtType}, or {@code null} if jwtType is {@code null}
     */
    default JwtType toJwtType(String jwtType) {
        return jwtType != null ? JwtType.ofType(jwtType) : null;
    }

    /**
     * Maps a {@link JwtType} value object to a {@link String} JWT type value.
     *
     * @param jwtType the {@link JwtType}
     * @return the JWT type value, or {@code null} if jwtType is {@code null}
     */
    default String toString(JwtType jwtType) {
        return jwtType != null ? jwtType.type() : null;
    }

}
