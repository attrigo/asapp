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

import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.infrastructure.authentication.DecodedJwt.TOKEN_USE_REFRESH_CLAIM_VALUE;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.domain.authentication.AccessToken;
import com.bcn.asapp.uaa.domain.authentication.RefreshToken;
import com.bcn.asapp.uaa.domain.user.User;

/**
 * Provider responsible for generating signed JSON Web Tokens (JWTs) for authentication.
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
 * @see Authentication
 * @see Jwts
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a>
 * @since 0.2.0
 */
@Component
public class JwtProvider {

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
     * Constructs a new {@code JwtProvider} with specified JWT secret and expiration settings.
     *
     * @param jwtSecret                  the Base64-encoded secret key for signing JWTs
     * @param accessTokenExpirationTime  expiration time for access tokens in milliseconds
     * @param refreshTokenExpirationTime expiration time for refresh tokens in milliseconds
     */
    public JwtProvider(@Value("${asapp.security.jwt-secret}") String jwtSecret,
            @Value("${asapp.security.access-token-expiration-time}") Long accessTokenExpirationTime,
            @Value("${asapp.security.refresh-token-expiration-time}") Long refreshTokenExpirationTime) {

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public AccessToken provideAccessToken(User user) {
        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(accessTokenExpirationTime);

        var jwt = generateJwt(user, ACCESS_TOKEN_TYPE, issuedAt, expiresAt);

        return new AccessToken(jwt, issuedAt, expiresAt);
    }

    public RefreshToken provideRefreshToken(User user) {
        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(refreshTokenExpirationTime);

        var jwt = generateJwt(user, REFRESH_TOKEN_TYPE, issuedAt, expiresAt);

        return new RefreshToken(jwt, issuedAt, expiresAt);
    }

    private String generateJwt(User user, String tokenType, Instant issuedAt, Instant expiresAt) {
        var role = user.getRole()
                       .name();

        return Jwts.builder()
                   .header()
                   .type(tokenType)
                   .and()
                   .subject(user.getUsername())
                   .claim(ROLE_CLAIM_NAME, role)
                   .claim(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_TYPE.equals(tokenType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(Date.from(issuedAt))
                   .expiration(Date.from(expiresAt))
                   .signWith(secretKey)
                   .compact();
    }

}
