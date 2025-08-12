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
package com.bcn.asapp.uaa.security.authentication.issuer;

import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_REFRESH_CLAIM_VALUE;

import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.user.Role;

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

    /**
     * Generates a new access token for the given authentication.
     * <p>
     * The token includes a header for the JWT type ("at+jwt") and claims for the username, user role, token use indicator ("access"), issuance time, and
     * expiration time.
     * <p>
     * Token IDs and user IDs are unset as they are expected to be populated when persisted.
     *
     * @param authentication the authenticated user details; must not be {@literal null} and must contain a valid username
     * @return a {@link AccessToken} containing the generated JWT and metadata
     * @throws IllegalArgumentException if authentication or its username is invalid
     */
    public AccessToken generateAccessToken(Authentication authentication) {
        validateAuthentication(authentication);

        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(accessTokenExpirationTime);

        var jwt = generateJwt(authentication, ACCESS_TOKEN_TYPE, issuedAt, expiresAt);

        return new AccessToken(null, null, jwt, issuedAt, expiresAt);
    }

    /**
     * Generates a new refresh token for the given authentication.
     * <p>
     * The token includes a header for the JWT type ("rt+jwt") and claims for the username, user role, token use indicator ("refresh"), issuance time, and
     * expiration time.
     * <p>
     * Token IDs and user IDs are unset as they are expected to be populated when persisted.
     *
     * @param authentication the authenticated user details; must not be {@literal null} and must contain a valid username
     * @return a {@link RefreshToken} containing the generated JWT and metadata
     * @throws IllegalArgumentException if authentication or its username is invalid
     */
    public RefreshToken generateRefreshToken(Authentication authentication) {
        validateAuthentication(authentication);

        var issuedAt = Instant.now();
        var expiresAt = Instant.now()
                               .plusMillis(refreshTokenExpirationTime);

        var jwt = generateJwt(authentication, REFRESH_TOKEN_TYPE, issuedAt, expiresAt);

        return new RefreshToken(null, null, jwt, issuedAt, expiresAt);
    }

    /**
     * Validates the provided {@link Authentication] object ensuring it is not {@literal null} and contains a valid username.
     *
     * @param authentication the authentication object to validate
     * @throws IllegalArgumentException if authentication is {@literal null} or has an empty username
     */
    private void validateAuthentication(Authentication authentication) {
        Assert.notNull(authentication, "Authentication must not be null");
        Assert.hasText(authentication.getName(), "Authentication username must not be null or empty");
    }

    /**
     * Generates and signs a JWT string with the given parameters.
     * <p>
     * The JWT header will have a {@code typ} field set to either {@code "at+jwt"} or {@code "rt+jwt"} to indicate access or refresh tokens, respectively. The
     * JWT claims will include:
     * <ul>
     * <li>{@code sub} - username</li>
     * <li>{@code role} - user's role</li>
     * <li>{@code token_use} - "access" or "refresh" indicating the token purpose</li>
     * <li>{@code iat} - issued at timestamp</li>
     * <li>{@code exp} - expiration timestamp</li>
     * </ul>
     * The JWT is signed with the configured secret key using an HMAC-SHA algorithm.
     *
     * @param authentication the authenticated user details
     * @param tokenType      the type of token to generate ("at+jwt" for access tokens, "rt+jwt" for refresh tokens)
     * @param issuedAt       the token issuance timestamp
     * @param expiresAt      the token expiration timestamp
     * @return a signed JWT string
     */
    private String generateJwt(Authentication authentication, String tokenType, Instant issuedAt, Instant expiresAt) {
        var username = authentication.getName();
        var role = resolveRole(authentication);

        return Jwts.builder()
                   .header()
                   .type(tokenType)
                   .and()
                   .subject(username)
                   .claim(ROLE_CLAIM_NAME, role.name())
                   .claim(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_TYPE.equals(tokenType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(Date.from(issuedAt))
                   .expiration(Date.from(expiresAt))
                   .signWith(secretKey)
                   .compact();
    }

    /**
     * Resolves the {@link Role} from the authentication's authorities.
     *
     * @param authentication the authenticated user details
     * @return the resolved {@link Role}
     * @throws IllegalArgumentException if no authorities are found or authority mapping fails
     */
    private Role resolveRole(Authentication authentication) {
        return authentication.getAuthorities()
                             .stream()
                             .findFirst()
                             .map(GrantedAuthority::getAuthority)
                             .map(this::mapAuthorityToRole)
                             .orElseThrow(() -> new IllegalArgumentException("Authentication authorities must not be empty"));
    }

    /**
     * Maps a granted authority string to a corresponding {@link Role} enum value.
     *
     * @param authority the granted authority string
     * @return the corresponding {@link Role}
     * @throws IllegalArgumentException if the authority does not match any known {@link Role}
     */
    private Role mapAuthorityToRole(String authority) {
        try {
            return Role.valueOf(authority);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Authentication authority is not valid");
        }
    }

}
