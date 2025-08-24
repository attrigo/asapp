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

package com.bcn.asapp.uaa.infrastructure.authentication;

import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationNotFoundException extends AuthenticationException {

    /**
     * Constructs a new {@code JwtAuthenticationNotFoundException} with the specified detail message.
     *
     * @param message the detail message providing additional information about the exception
     */
    public JwtAuthenticationNotFoundException(String message) {
        super(message);
    }

}
