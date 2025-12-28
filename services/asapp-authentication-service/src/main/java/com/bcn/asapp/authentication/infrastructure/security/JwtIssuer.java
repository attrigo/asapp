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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.JwtType;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.Role;

/**
 * Infrastructure component for issuing signed JWTs.
 * <p>
 * Implements {@link TokenIssuer} port, providing the infrastructure capability to generate JWTs using the JJWT library.
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
 * @since 0.2.0
 * @see Jwts
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a>
 * @author attrigo
 */
@Component
public class JwtIssuer implements TokenIssuer {

    private static final Logger logger = LoggerFactory.getLogger(JwtIssuer.class);

    private final SecretKey secretKey;

    private final Long accessTokenExpirationTime;

    private final Long refreshTokenExpirationTime;

    /**
     * Constructs a new {@code JwtIssuer} with the configured secret key and expiration times.
     *
     * @param jwtSecret                  the base64-encoded JWT secret from configuration
     * @param accessTokenExpirationTime  the access token expiration time in milliseconds
     * @param refreshTokenExpirationTime the refresh token expiration time in milliseconds
     */
    public JwtIssuer(@Value("${asapp.security.jwt-secret}") String jwtSecret,
            @Value("${asapp.security.access-token-expiration-time}") Long accessTokenExpirationTime,
            @Value("${asapp.security.refresh-token-expiration-time}") Long refreshTokenExpirationTime) {

        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    /**
     * Issues a JWT pair (access and refresh tokens) for an authenticated user.
     *
     * @param userAuthentication the {@link UserAuthentication} containing user data
     * @return the generated {@link JwtPair} containing both access and refresh tokens
     */
    @Override
    public JwtPair issueTokenPair(UserAuthentication userAuthentication) {
        logger.trace("[JWT_ISSUER] Issuing token pair for username={}", userAuthentication.username());

        var subject = Subject.of(userAuthentication.username()
                                                   .value());
        var role = userAuthentication.role();

        return issueTokenPair(subject, role);
    }

    /**
     * Issues a JWT pair (access and refresh tokens) for a subject and role.
     *
     * @param subject the {@link Subject} identifier
     * @param role    the {@link Role}
     * @return the generated {@link JwtPair} containing both access and refresh tokens
     */
    @Override
    public JwtPair issueTokenPair(Subject subject, Role role) {
        logger.trace("[JWT_ISSUER] Issuing token pair for subject={} and role={}", subject, role);
        var accessToken = issueToken(ACCESS_TOKEN, subject, role, accessTokenExpirationTime, ACCESS_TOKEN_USE);
        var refreshToken = issueToken(REFRESH_TOKEN, subject, role, refreshTokenExpirationTime, REFRESH_TOKEN_USE);
        return JwtPair.of(accessToken, refreshToken);
    }

    /**
     * Issues a JWT with the specified parameters.
     *
     * @param type                 the {@link JwtType}
     * @param subject              the {@link Subject} identifier
     * @param role                 the {@link Role}
     * @param expirationTimeMillis the expiration time in milliseconds
     * @param tokenUseClaim        the token_use claim value
     * @return the generated {@link Jwt}
     */
    private Jwt issueToken(JwtType type, Subject subject, Role role, Long expirationTimeMillis, String tokenUseClaim) {
        var claims = JwtClaims.of(ROLE, role.name(), TOKEN_USE, tokenUseClaim);
        var issued = Issued.now();
        var expiration = Expiration.of(issued, expirationTimeMillis);

        var issuedToken = issueToken(type, subject, claims, issued, expiration);
        var encodedToken = EncodedToken.of(issuedToken);

        return Jwt.of(encodedToken, type, subject, claims, issued, expiration);
    }

    /**
     * Creates and signs a JWT string.
     *
     * @param tokenType  the {@link JwtType}
     * @param subject    the {@link Subject} identifier
     * @param claims     the {@link JwtClaims}
     * @param issuedAt   the {@link Issued} timestamp
     * @param expiration the {@link Expiration} timestamp
     * @return the signed JWT string
     */
    private String issueToken(JwtType tokenType, Subject subject, JwtClaims claims, Issued issuedAt, Expiration expiration) {
        logger.trace("[JWT_ISSUER] Building and signing {} token with expiration={}", tokenType, expiration.value());
        return Jwts.builder()
                   .header()
                   .type(tokenType.type())
                   .and()
                   .subject(subject.value())
                   .claims(claims.value())
                   .issuedAt(issuedAt.asDate())
                   .expiration(expiration.asDate())
                   .signWith(secretKey)
                   .compact();
    }

}
