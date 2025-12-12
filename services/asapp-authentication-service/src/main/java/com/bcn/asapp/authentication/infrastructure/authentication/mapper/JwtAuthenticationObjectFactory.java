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

import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.user.mapper.UserIdMapper;

/**
 * MapStruct object factory for mapping between {@link JwtAuthentication} domain entities and {@link JdbcJwtAuthenticationEntity} database entities.
 * <p>
 * Ensures that domain and database entities are created through their proper factory methods with complete validation, maintaining domain integrity during
 * mapping.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class JwtAuthenticationObjectFactory {

    private final JwtAuthenticationIdMapper idMapper;

    private final UserIdMapper userIdMapper;

    private final JwtMapper jwtMapper;

    /**
     * Constructs a new {@code JwtAuthenticationObjectFactory} with required mappers.
     *
     * @param idMapper     the mapper for JWT authentication IDs
     * @param userIdMapper the mapper for user IDs
     * @param jwtMapper    the mapper for JWT tokens
     */
    public JwtAuthenticationObjectFactory(JwtAuthenticationIdMapper idMapper, UserIdMapper userIdMapper, JwtMapper jwtMapper) {
        this.idMapper = idMapper;
        this.userIdMapper = userIdMapper;
        this.jwtMapper = jwtMapper;
    }

    /**
     * Creates a database {@link JdbcJwtAuthenticationEntity} entity from a domain {@link JwtAuthentication}.
     * <p>
     * Maps domain value objects to their primitive representations for persistence.
     *
     * @param source the {@link JwtAuthentication} domain entity
     * @return the {@link JdbcJwtAuthenticationEntity} database entity
     */
    @ObjectFactory
    public JdbcJwtAuthenticationEntity toJdbcJwtAuthenticationEntity(JwtAuthentication source) {
        var id = idMapper.toUUID(source.getId());
        var userId = userIdMapper.toUUID(source.getUserId());
        var accessToken = jwtMapper.toJdbcJwtEntity(source.accessToken());
        var refreshToken = jwtMapper.toJdbcJwtEntity(source.refreshToken());

        return new JdbcJwtAuthenticationEntity(id, userId, accessToken, refreshToken);
    }

    /**
     * Creates a domain {@link JwtAuthentication} from a database {@link JdbcJwtAuthenticationEntity} entity.
     * <p>
     * Maps entity fields to value objects and constructs either an authenticated or unauthenticated JWT authentication using the domain's factory methods.
     *
     * @param source the {@link JdbcJwtAuthenticationEntity} database entity
     * @return the {@link JwtAuthentication} domain entity
     */
    @ObjectFactory
    public JwtAuthentication toJwtAuthentication(JdbcJwtAuthenticationEntity source) {
        var userId = userIdMapper.toUserId(source.userId());
        var accessToken = jwtMapper.toJwt(source.accessToken());
        var refreshToken = jwtMapper.toJwt(source.refreshToken());
        var jwtPair = JwtPair.of(accessToken, refreshToken);

        if (source.id() == null) {
            return JwtAuthentication.unAuthenticated(userId, jwtPair);
        } else {
            var id = idMapper.toJwtAuthenticationId(source.id());
            return JwtAuthentication.authenticated(id, userId, jwtPair);
        }
    }

}
