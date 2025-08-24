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

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.authentication.spi.JwtAuthenticationRepository;
import com.bcn.asapp.uaa.application.authentication.spi.JwtRevoker;
import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

@Component
public class JwtRevokerAdapter implements JwtRevoker {

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    public JwtRevokerAdapter(JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    public void revokeAuthentication(JwtAuthentication authentication) {
        jwtAuthenticationRepository.deleteById(authentication.getId());
    }

}
