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

package com.bcn.asapp.uaa.domain.user;

import com.bcn.asapp.uaa.domain.authentication.JwtAuthentication;

/**
 * Represents a user entity in the UAA (User Account and Authentication) service.
 * <p>
 * Encapsulates the user's identity, credentials, and assigned role.
 *
 * @param id       the unique identifier of the user
 * @param username the username used for authentication and identification, must not be {@literal blank}
 * @param password the encrypted password used for authentication, must not be {@literal blank}
 * @param role     the {@link Role} assigned to the user, determining access permissions, must not be {@literal null}
 * @author ttrigo
 * @since 0.2.0
 */
public class User {

    private UserId id;

    private String username;

    private String password;

    private Role role;

    private JwtAuthentication authentication;

    public User(String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public UserId getId() {
        return id;
    }

    public void setId(UserId id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public JwtAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(JwtAuthentication authentication) {
        this.authentication = authentication;
    }

}
