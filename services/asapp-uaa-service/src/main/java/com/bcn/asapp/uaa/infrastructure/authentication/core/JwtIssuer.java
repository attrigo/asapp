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

package com.bcn.asapp.uaa.infrastructure.authentication.core;

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_REFRESH_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.domain.authentication.Jwt;
import com.bcn.asapp.uaa.domain.authentication.JwtType;
import com.bcn.asapp.uaa.domain.user.Role;
import com.bcn.asapp.uaa.domain.user.User;

/**
 * Issuer responsible for generating signed JSON Web Tokens (JWTs) for authentication.
 * <p>
 * It can generate two types of tokens:
 * <ul>
 * <li>Access tokens - Short-lived tokens used for accessing protected resources</li>
 * <li>Refresh tokens - Longer-lived tokens used for obtaining new access tokens without re-authentication</li>
 * </ul>
 * <p>
 * Supports creation of both <em>access tokens</em> and <em>refresh tokens</em> with configurable expiration times and secret key, suitable for use in OAuth2 or
 * custom authentication flows.
 * <p>
 * Tokens are signed using HMAC-SHA algorithms with a Base64-decoded secret key.
 * <p>
 * Generated tokens include standard headers like {@code typ} (token type: "at+jwt" or "rt+jwt") and claims such as {@code sub} (subject), {@code iat} (issued
 * at), {@code exp} (expiration), and custom claims for {@code role} and {@code token_use} ("access" or "refresh").
 *
 * @author ttrigo
 * @see Jwts
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a>
 * @since 0.2.0
 */
@Component
// TODO: Check if possible to reduce duplicated code
// TODO: Can replace Map<String, Object> by var
public class JwtIssuer {

    /**
     * Secret key used for signing JWTs. Derived from a Base64-encoded string.
     */
    private final SecretKey secretKey;

    /**
     * Access token expiration time in milliseconds.
     */
    private final Long accessTokenExpirationTime;

    /**
     * Refresh token expiration time in milliseconds.
     */
    private final Long refreshTokenExpirationTime;

    /**
     * Constructs a new {@code JwtIssuer} with specified JWT secret and expiration settings.
     *
     * @param jwtSecret                  the Base64-encoded secret key for signing JWTs
     * @param accessTokenExpirationTime  expiration time for access tokens in milliseconds
     * @param refreshTokenExpirationTime expiration time for refresh tokens in milliseconds
     */
    public JwtIssuer(@Value("${asapp.security.jwt-secret}") String jwtSecret,
            @Value("${asapp.security.access-token-expiration-time}") Long accessTokenExpirationTime,
            @Value("${asapp.security.refresh-token-expiration-time}") Long refreshTokenExpirationTime) {

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public Jwt issueAccessToken(User user) {
        return issueAccessToken(user.getUsername(), user.getRole());
    }

    public Jwt issueAccessToken(String subject, Role role) {
        Map<String, Object> claims = Map.of(ROLE_CLAIM_NAME, role.name(), TOKEN_USE_CLAIM_NAME, TOKEN_USE_ACCESS_CLAIM_VALUE);
        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(accessTokenExpirationTime);

        var token = issueToken(subject, ACCESS_TOKEN, claims, issuedAt, expiresAt);

        return new Jwt(token, ACCESS_TOKEN, subject, claims, issuedAt, expiresAt);
    }

    public Jwt issueRefreshToken(User user) {
        return issueRefreshToken(user.getUsername(), user.getRole());
    }

    public Jwt issueRefreshToken(String subject, Role role) {
        Map<String, Object> claims = Map.of(ROLE_CLAIM_NAME, role.name(), TOKEN_USE_CLAIM_NAME, TOKEN_USE_REFRESH_CLAIM_VALUE);
        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(refreshTokenExpirationTime);

        var token = issueToken(subject, REFRESH_TOKEN, claims, issuedAt, expiresAt);

        return new Jwt(token, REFRESH_TOKEN, subject, claims, issuedAt, expiresAt);
    }

    private String issueToken(String subject, JwtType tokenType, Map<String, Object> claims, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                   .header()
                   .type(tokenType.getType())
                   .and()
                   .subject(subject)
                   .claims(claims)
                   .issuedAt(Date.from(issuedAt))
                   .expiration(Date.from(expiresAt))
                   .signWith(secretKey)
                   .compact();

    }

}
