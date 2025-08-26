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

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;

import com.bcn.asapp.uaa.domain.user.UserId;

public final class JwtAuthentication {

    // TODO: Rethink if really needed
    private final JwtAuthenticationId id;

    private final UserId userId;

    private Jwt accessToken;

    private Jwt refreshToken;

    private JwtAuthentication(JwtAuthenticationId id, UserId userId, Jwt accessToken, Jwt refreshToken) {
        this.id = id;
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        this.userId = userId;
        setAccessToken(accessToken);
        setRefreshToken(refreshToken);
    }

    public static JwtAuthentication unAuthenticated(UserId userId, Jwt accessToken, Jwt refreshToken) {
        return new JwtAuthentication(null, userId, accessToken, refreshToken);
    }

    public static JwtAuthentication authenticated(JwtAuthenticationId id, UserId userId, Jwt accessToken, Jwt refreshToken) {
        return new JwtAuthentication(id, userId, accessToken, refreshToken);
    }

    public JwtAuthenticationId getId() {
        return this.id;
    }

    public UserId getUserId() {
        return this.userId;
    }

    public Jwt getAccessToken() {
        return this.accessToken;
    }

    // TODO: Check if can be written simpler
    public void setAccessToken(Jwt accessToken) {
        if (accessToken != null && this.refreshToken != null) {
            accessToken.getClaim(ROLE_CLAIM_NAME, String.class)
                       .orElseThrow(() -> new IllegalArgumentException("Access token must contain Role claim"));
            this.accessToken = accessToken;

        } else if (accessToken == null && this.refreshToken == null) {
            this.accessToken = null;
        }
    }

    public Jwt getRefreshToken() {
        return this.refreshToken;
    }

    // TODO: Check if can be written simpler
    public void setRefreshToken(Jwt refreshToken) {
        if (refreshToken != null && this.accessToken != null) {
            refreshToken.getClaim(ROLE_CLAIM_NAME, String.class)
                        .orElseThrow(() -> new IllegalArgumentException("Refresh token must contain Role claim"));
            this.refreshToken = refreshToken;

        } else if (refreshToken == null && this.accessToken == null) {
            this.refreshToken = null;
        }
    }

}
