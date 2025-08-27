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

package com.bcn.asapp.uaa.infrastructure.authentication.spi;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.uaa.infrastructure.authentication.mapper.JwtAuthenticationMapper;

@Component
public class JwtAuthenticationRepositoryAdapter implements JwtAuthenticationRepository {

    private final JwtAuthenticationJdbcRepository jwtAuthenticationRepository;

    private final JwtAuthenticationMapper jwtAuthenticationMapper;

    public JwtAuthenticationRepositoryAdapter(JwtAuthenticationJdbcRepository jwtAuthenticationRepository, JwtAuthenticationMapper jwtAuthenticationMapper) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
        this.jwtAuthenticationMapper = jwtAuthenticationMapper;
    }

    @Override
    public Optional<JwtAuthentication> findByAccessToken(String accessToken) {
        return jwtAuthenticationRepository.findByAccessTokenToken(accessToken)
                                          .map(jwtAuthenticationMapper::toJwtAuthentication);
    }

    @Override
    public Optional<JwtAuthentication> findByRefreshToken(String refreshToken) {
        return jwtAuthenticationRepository.findByRefreshTokenToken(refreshToken)
                                          .map(jwtAuthenticationMapper::toJwtAuthentication);
    }

    @Override
    public Boolean existsByAccessToken(String accessToken) {
        return jwtAuthenticationRepository.existsByAccessTokenToken(accessToken);
    }

    @Override
    public JwtAuthentication save(JwtAuthentication jwtAuthentication) {
        var jwtAuthenticationToSave = jwtAuthenticationMapper.toJwtAuthenticationEntity(jwtAuthentication);
        var jwtAuthenticationSaved = jwtAuthenticationRepository.save(jwtAuthenticationToSave);
        return jwtAuthenticationMapper.toJwtAuthentication(jwtAuthenticationSaved);
    }

    @Override
    public void deleteById(JwtAuthenticationId id) {
        jwtAuthenticationRepository.deleteById(id.id());
    }

}
