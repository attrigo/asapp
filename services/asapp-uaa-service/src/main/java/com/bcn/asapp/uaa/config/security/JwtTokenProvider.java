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
package com.bcn.asapp.uaa.config.security;

import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.auth.Role;

/**
 * JWT utility component that provides operations to work with JSON Web Tokens.
 *
 * @author ttrigo
 * @since 0.2.0
 * @see Keys
 * @see SecretKey
 * @see Jwts
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    private final Long jwtExpirationDate;

    /**
     * Main constructor.
     *
     * @param jwtSecret         the jwt secret used to sign the JWT (from application properties).
     * @param jwtExpirationDate the jwt expiration date in milliseconds (from application properties).
     */
    public JwtTokenProvider(@Value("${asapp.jwt-secret}") String jwtSecret, @Value("${asapp.jwt-expiration}") String jwtExpirationDate) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpirationDate = Long.valueOf(jwtExpirationDate);
    }

    /**
     * Generates a JWT token for the given authenticated user.
     * <p>
     * The token contains the username and role as claims, and has an expiration time set based on the configured expiration date.
     *
     * @param authentication the authentication object containing user details.
     * @return a signed JWT.
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Optional<String> optionalGrantedAuthority = authentication.getAuthorities()
                                                                  .stream()
                                                                  .findFirst()
                                                                  .map(GrantedAuthority::getAuthority);
        Role role = optionalGrantedAuthority.map(Role::valueOf)
                                            .orElse(Role.ANONYMOUS);
        Date issuedAtDate = new Date();
        Date expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationDate);

        return Jwts.builder()
                   .subject(username)
                   .claim("role", role.name())
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

}
