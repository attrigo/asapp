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

import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.application.authentication.spi.JwtVerifier;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;
import com.bcn.asapp.uaa.infrastructure.authentication.core.InvalidJwtException;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtAuthenticationNotFoundException;
import com.bcn.asapp.uaa.infrastructure.authentication.core.JwtDecoder;
import com.bcn.asapp.uaa.infrastructure.authentication.core.UnexpectedJwtTypeException;

@Component
public class JwtVerifierAdapter implements JwtVerifier {

    private final JwtDecoder jwtDecoder;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public JwtVerifierAdapter(JwtDecoder jwtDecoder, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    @Override
    public final JwtAuthentication verifyAccessToken(String accessToken) {
        try {
            var jwt = jwtDecoder.decode(accessToken);

            if (!jwt.isAccessToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not a %s", jwt.token(), ACCESS_TOKEN));
            }

            // TODO: Should check if access token belongs to the user in the system (JWT could username could not be modified)
            return jwtAuthenticationRepository.findByAccessToken(jwt.token())
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      "Jwt authentication not found by access token " + jwt.token()));
        } catch (Exception e) {
            throw new InvalidJwtException("Access token is not valid", e);
        }
    }

    @Override
    public final JwtAuthentication verifyRefreshToken(String refreshToken) {
        try {
            var jwt = jwtDecoder.decode(refreshToken);

            if (!jwt.isRefreshToken()) {
                throw new UnexpectedJwtTypeException(String.format("JWT %s is not a %s", jwt.token(), REFRESH_TOKEN));
            }

            // TODO: Should check if refresh token belongs to the user in the system (JWT could username could not be modified)
            return jwtAuthenticationRepository.findByRefreshToken(jwt.token())
                                              .orElseThrow(() -> new JwtAuthenticationNotFoundException(
                                                      "Jwt authentication not found by refresh token " + jwt.token()));
        } catch (Exception e) {
            throw new InvalidJwtException("Refresh token is not valid", e);
        }
    }

}
