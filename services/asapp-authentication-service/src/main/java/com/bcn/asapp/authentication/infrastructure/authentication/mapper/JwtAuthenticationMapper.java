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

import com.bcn.asapp.authentication.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.out.entity.JwtAuthenticationEntity;
import com.bcn.asapp.authentication.infrastructure.user.mapper.UserIdMapper;

/**
 * MapStruct mapper for mapping between JWT authentication-related objects.
 * <p>
 * Handles mappings between REST requests, commands, domain entities, database entities, and responses.
 * <p>
 * Uses custom object factories and component mappers for complex value object transformations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring", uses = { JwtAuthenticationObjectFactory.class, JwtAuthenticationIdMapper.class, UserIdMapper.class, JwtMapper.class })
public interface JwtAuthenticationMapper {

    /**
     * Maps a {@link AuthenticateRequest} to an {@link AuthenticateCommand}.
     *
     * @param request the {@link AuthenticateRequest}
     * @return the {@link AuthenticateCommand}
     */
    AuthenticateCommand toAuthenticateCommand(AuthenticateRequest request);

    /**
     * Maps a domain {@link JwtAuthentication} to a database {@link JwtAuthenticationEntity}.
     *
     * @param jwtAuthentication the {@link JwtAuthentication} domain entity
     * @return the {@link JwtAuthenticationEntity} database entity
     */
    JwtAuthenticationEntity toJwtAuthenticationEntity(JwtAuthentication jwtAuthentication);

    /**
     * Maps a database {@link JwtAuthenticationEntity} to a domain {@link JwtAuthentication}.
     * <p>
     * Uses {@link JwtAuthenticationObjectFactory} to construct the domain entity with proper validation.
     *
     * @param jwtAuthenticationEntity the {@link JwtAuthenticationEntity} database entity
     * @return the {@link JwtAuthentication} domain entity
     */
    JwtAuthentication toJwtAuthentication(JwtAuthenticationEntity jwtAuthenticationEntity);

    /**
     * Maps a domain {@link JwtAuthentication} to an {@link AuthenticateResponse}.
     *
     * @param jwtAuthentication the {@link JwtAuthentication} domain entity
     * @return the {@link AuthenticateResponse}
     */
    @Mapping(target = "accessToken", source = "jwtPair.accessToken")
    @Mapping(target = "refreshToken", source = "jwtPair.refreshToken")
    AuthenticateResponse toAuthenticateResponse(JwtAuthentication jwtAuthentication);

    /**
     * Maps a domain {@link JwtAuthentication} to a {@link RefreshAuthenticationResponse}.
     *
     * @param jwtAuthentication the {@link JwtAuthentication} domain entity
     * @return the {@link RefreshAuthenticationResponse}
     */
    @Mapping(target = "accessToken", source = "jwtPair.accessToken")
    @Mapping(target = "refreshToken", source = "jwtPair.refreshToken")
    RefreshAuthenticationResponse toRefreshAuthenticationResponse(JwtAuthentication jwtAuthentication);

}
