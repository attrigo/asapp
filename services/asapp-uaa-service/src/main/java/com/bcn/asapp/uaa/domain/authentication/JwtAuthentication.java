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

import com.bcn.asapp.uaa.domain.user.UserId;

public class JwtAuthentication {

    private JwtAuthenticationId id;

    private UserId userId;

    private AccessToken accessToken;

    private RefreshToken refreshToken;

    private JwtAuthentication(UserId userId, AccessToken accessToken, RefreshToken refreshToken) {
        if (userId == null) {
            throw new IllegalArgumentException("User must not be null");
        }
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static JwtAuthentication unAuthenticated(UserId userId) {
        return new JwtAuthentication(userId, null, null);
    }

    public static JwtAuthentication authenticated(UserId userId, AccessToken accessToken, RefreshToken refreshToken) {
        return new JwtAuthentication(userId, accessToken, refreshToken);
    }

    public JwtAuthenticationId getId() {
        return id;
    }

    public void setId(JwtAuthenticationId id) {
        this.id = id;
    }

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public AccessToken getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }

}
