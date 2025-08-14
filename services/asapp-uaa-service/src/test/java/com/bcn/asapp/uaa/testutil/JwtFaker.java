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

package com.bcn.asapp.uaa.testutil;

import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_REFRESH_CLAIM_VALUE;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.SecretKey;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.security.authentication.DecodedJwt;
import com.bcn.asapp.uaa.security.core.JwtType;

public class JwtFaker {

    private static final String SUBJECT = "TEST USERNAME";

    private static final String ROLE = "USER";

    private static final String INVALID_TOKEN = "INVALID_TOKEN";

    private static final String INVALID_SIGNATURE = "M0LBjhuY5Xgk25aRFCTp72EXM2HEnRY7KHAIlNQCxzwsMw7HgQBbdN4Mka94siHP";

    private final SecretKey secretKey;

    private final Long jwtExpirationTime;

    public JwtFaker() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T"));
        this.jwtExpirationTime = 300000L;
    }

    public JwtFaker(String secretKey, Long jwtExpirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
        this.jwtExpirationTime = jwtExpirationTime;
    }

    public String fakeJwt(JwtType jwtType) {
        var issuedAtDate = Date.from(getRandomInstant());
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    public String fakeJwtWithoutType() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    public String fakeJwtWithInvalidType() {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type("INVALID TYPE")
                   .and()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .claim(TOKEN_USE_CLAIM_NAME, "INVALID TYPE")
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    public String fakeJwtInvalid() {
        return INVALID_TOKEN;
    }

    public String fakeJwtWithInvalidSignature(JwtType jwtType) {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(INVALID_SIGNATURE)))
                   .compact();
    }

    public String fakeJwtExpired(JwtType jwtType) {
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(new Date())
                   .expiration(new Date())
                   .signWith(secretKey)
                   .compact();
    }

    public String fakeJwtNotSigned(JwtType jwtType) {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .subject(SUBJECT)
                   .claim(ROLE_CLAIM_NAME, ROLE)
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .compact();
    }

    public String fakeJwtWithoutUsername(JwtType jwtType) {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    public String fakeJwtWithoutAuthorities(JwtType jwtType) {
        var issuedAtDate = new Date();
        var expirationDate = new Date(issuedAtDate.getTime() + jwtExpirationTime);
        return Jwts.builder()
                   .header()
                   .type(JwtType.ACCESS_TOKEN.equals(jwtType) ? ACCESS_TOKEN_TYPE : REFRESH_TOKEN_TYPE)
                   .and()
                   .subject(SUBJECT)
                   .claim(TOKEN_USE_CLAIM_NAME, JwtType.ACCESS_TOKEN.equals(jwtType) ? TOKEN_USE_ACCESS_CLAIM_VALUE : TOKEN_USE_REFRESH_CLAIM_VALUE)
                   .issuedAt(issuedAtDate)
                   .expiration(expirationDate)
                   .signWith(secretKey)
                   .compact();
    }

    public DecodedJwt fakeDecodedJwt(String jwt) {
        var jwsClaims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parse(jwt)
                            .accept(Jws.CLAIMS);
        return new DecodedJwt(jwt, jwsClaims.getHeader(), jwsClaims.getPayload());
    }

    private Instant getRandomInstant() {
        long startMillis = Instant.now()
                                  .minus(Duration.ofSeconds(10))
                                  .getEpochSecond();
        long endMillis = Instant.now()
                                .getEpochSecond();
        long randomMillis = ThreadLocalRandom.current()
                                             .nextLong(startMillis, endMillis);
        return Instant.ofEpochSecond(randomMillis);
    }

}
