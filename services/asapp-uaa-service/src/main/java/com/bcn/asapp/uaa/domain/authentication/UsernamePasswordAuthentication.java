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

package com.bcn.asapp.uaa.domain.authentication;

import java.util.Collection;

import com.bcn.asapp.uaa.domain.user.Role;

public record UsernamePasswordAuthentication(
        String username,
        String password,
        Collection<Role> authorities
) {

    public UsernamePasswordAuthentication {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username could be null or empty");
        }
    }

    public static UsernamePasswordAuthentication unAuthenticated(String username, String password) {
        return new UsernamePasswordAuthentication(username, password, null);
    }

    public static UsernamePasswordAuthentication authenticated(String username, String password, Collection<Role> authorities) {
        if (authorities == null || authorities.isEmpty()) {
            throw new IllegalArgumentException("Authorities could be null or empty");
        }
        return new UsernamePasswordAuthentication(username, password, authorities);
    }

}
