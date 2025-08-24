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

public class User {

    private UserId id;

    // TODO: Should be possible to change the username (may generate un-synchronized tokens)
    private String username;

    private String password;

    private Role role;

    public User(UserId id, String username, String password, Role role) {
        // TODO: Is a good practice to use setters in constructor
        setId(id);
        setUsername(username);
        setPassword(password);
        this.role = role;
    }

    public User(String username, String password, Role role) {
        setUsername(username);
        setPassword(password);
        this.role = role;
    }

    public void setId(UserId id) {
        if (id == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (this.id == null) {
            this.id = id;
        }
    }

    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

}
