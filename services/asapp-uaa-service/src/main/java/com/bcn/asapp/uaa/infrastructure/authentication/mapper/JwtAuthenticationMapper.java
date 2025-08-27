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
import org.mapstruct.Mapping;

import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.AuthenticateResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.api.resource.RefreshAuthenticationResponse;
import com.bcn.asapp.uaa.infrastructure.authentication.entity.JwtAuthenticationEntity;
import com.bcn.asapp.uaa.infrastructure.user.mapper.UserIdMapper;

@Mapper(componentModel = "spring", uses = { JwtAuthenticationObjectFactory.class, JwtAuthenticationIdMapper.class, UserIdMapper.class, JwtMapper.class })
public interface JwtAuthenticationMapper {

    // JwtAuthentication -> JwtAuthenticationEntity
    JwtAuthenticationEntity toJwtAuthenticationEntity(JwtAuthentication jwtAuthentication);

    // JwtAuthenticationEntity -> JwtAuthentication
    JwtAuthentication toJwtAuthentication(JwtAuthenticationEntity jwtAuthenticationEntity);

    // JwtAuthentication -> Response
    @Mapping(target = "accessToken", source = "accessToken.token")
    @Mapping(target = "refreshToken", source = "refreshToken.token")
    AuthenticateResponse toAuthenticateResponse(JwtAuthentication jwtAuthentication);

    @Mapping(target = "accessToken", source = "accessToken.token")
    @Mapping(target = "refreshToken", source = "refreshToken.token")
    RefreshAuthenticationResponse toRefreshAuthenticationResponse(JwtAuthentication jwtAuthentication);

}
