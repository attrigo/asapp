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

package com.bcn.asapp.authentication.infrastructure.authentication.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtEntity;

/**
 * MapStruct mapper for mapping between JWT-related objects.
 * <p>
 * Handles mappings between domain {@link Jwt} entities and database {@link JdbcJwtEntity} entities.
 * <p>
 * Uses component mappers for complex value object transformations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring", uses = { EncodedTokenMapper.class, SubjectMapper.class, JwtTypeMapper.class, IssuedMapper.class, ExpirationMapper.class })
public interface JwtMapper {

    /**
     * Maps a domain {@link Jwt} to a database {@link JdbcJwtEntity}.
     *
     * @param jwt the {@link Jwt} domain entity
     * @return the {@link JdbcJwtEntity} database entity
     */
    @Mapping(target = "token", source = "encodedToken")
    JdbcJwtEntity toJdbcJwtEntity(Jwt jwt);

    /**
     * Extracts the encoded token value from a domain {@link Jwt}.
     *
     * @param jwt the {@link Jwt} domain entity
     * @return the encoded token value, or {@code null} if jwt is {@code null}
     */
    default String toEncodedTokenAsString(Jwt jwt) {
        return jwt != null ? jwt.encodedTokenValue() : null;
    }

    /**
     * Maps a database {@link JdbcJwtEntity} to a domain {@link Jwt}.
     *
     * @param jdbcJwtEntity the {@link JdbcJwtEntity} database entity
     * @return the {@link Jwt} domain entity
     */
    @Mapping(target = "encodedToken", source = "token")
    Jwt toJwt(JdbcJwtEntity jdbcJwtEntity);

}
