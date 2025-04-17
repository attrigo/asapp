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
package com.bcn.asapp.projects.testutil;

import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtTestGenerator {

    private static final String SUBJECT = "TEST USERNAME";

    private static final String ROLE = "USER";

    private static final String INVALID_TOKEN = "INVALID_TOKEN";

    private static final String INVALID_SIGNATURE = "M0LBjhuY5Xgk25aRFCTp72EXM2HEnRY7KHAIlNQCxzwsMw7HgQBbdN4Mka94siHP";

    private final String jwtSecret;

    private final Long jwtExpirationTime;

    public JwtTestGenerator(String jwtSecret, Long jwtExpirationTime) {
        this.jwtSecret = jwtSecret;
        this.jwtExpirationTime = jwtExpirationTime;
    }

    public String generateJwt() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .subject(SUBJECT)
                   .claim("role", ROLE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                   .compact();
    }

    public String generateJwtWithoutUsername() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .claim("role", ROLE)
                   .issuedAt(new Date())
                   .expiration(expirationDate)
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                   .compact();
    }

    public String generateJwtWithoutAuthorities() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .subject(SUBJECT)
                   .issuedAt(new Date())
                   .expiration(expirationDate)
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                   .compact();
    }

    public String generateJwtInvalid() {
        return INVALID_TOKEN;
    }

    public String generateJwtWithInvalidSignature() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .subject(SUBJECT)
                   .claim("role", ROLE)
                   .issuedAt(new Date())
                   .expiration(expirationDate)
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(INVALID_SIGNATURE)))
                   .compact();
    }

    public String generateJwtExpired() {
        return Jwts.builder()
                   .subject(SUBJECT)
                   .claim("role", ROLE)
                   .issuedAt(new Date())
                   .expiration(new Date())
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)))
                   .compact();
    }

    public String generateJwtNotSigned() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .subject(SUBJECT)
                   .claim("role", ROLE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .compact();
    }

}
