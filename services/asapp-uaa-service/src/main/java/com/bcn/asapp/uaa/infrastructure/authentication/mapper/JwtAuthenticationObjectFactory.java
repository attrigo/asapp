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

import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.domain.authentication.Jwt;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.uaa.domain.user.UserId;
import com.bcn.asapp.uaa.infrastructure.authentication.entity.JwtAuthenticationEntity;
import com.bcn.asapp.uaa.infrastructure.user.mapper.UserIdMapper;

@Component
public class JwtAuthenticationObjectFactory {

    private final JwtAuthenticationIdMapper idMapper;

    private final UserIdMapper userIdMapper;

    private final JwtMapper jwtMapper;

    public JwtAuthenticationObjectFactory(JwtAuthenticationIdMapper idMapper, UserIdMapper userIdMapper, JwtMapper jwtMapper) {
        this.idMapper = idMapper;
        this.userIdMapper = userIdMapper;
        this.jwtMapper = jwtMapper;
    }

    @ObjectFactory
    public JwtAuthentication create(JwtAuthenticationEntity entity) {
        JwtAuthenticationId id = idMapper.toJwtAuthenticationId(entity.id());
        UserId userId = userIdMapper.toUserId(entity.userId());
        Jwt accessToken = jwtMapper.toJwt(entity.accessToken());
        Jwt refreshToken = jwtMapper.toJwt(entity.refreshToken());

        if (id == null) {
            return JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);
        } else {
            return JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);
        }
    }

}
