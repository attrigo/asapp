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
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_ACCESS_CLAIM_VALUE;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.security.authentication.DecodedJwt.TOKEN_USE_REFRESH_CLAIM_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.util.StringUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.user.Role;

public class JwtAssertions {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private JwtAssertions() {}

    public static void assertJwtType(String actualJwt, JwtType expectedType) {
        var actualClaims = Jwts.parser()
                               .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                               .build()
                               .parseSignedClaims(actualJwt);

        var tokenType = actualClaims.getHeader()
                                    .getType();
        var tokenUseClaim = actualClaims.getPayload()
                                        .get(TOKEN_USE_CLAIM_NAME, String.class);

        assertTrue(StringUtils.hasText(tokenType));
        assertTrue(tokenType.equals(ACCESS_TOKEN_TYPE) || tokenType.equals(REFRESH_TOKEN_TYPE));

        if (JwtType.ACCESS_TOKEN.equals(expectedType)) {
            assertTrue(ACCESS_TOKEN_TYPE.equals(tokenType) && TOKEN_USE_ACCESS_CLAIM_VALUE.equals(tokenUseClaim));
        } else {
            assertTrue(REFRESH_TOKEN_TYPE.equals(tokenType) && TOKEN_USE_REFRESH_CLAIM_VALUE.equals(tokenUseClaim));
        }

    }

    public static void assertJwtUsername(String actualJwt, String expectedUsername) {
        var actualUsername = Jwts.parser()
                                 .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                 .build()
                                 .parseSignedClaims(actualJwt)
                                 .getPayload()
                                 .getSubject();

        assertEquals(actualUsername, expectedUsername);
    }

    public static void assertJwtRole(String actualJwt, Role expectedRole) {
        var actualAuthority = Jwts.parser()
                                  .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                  .build()
                                  .parseSignedClaims(actualJwt)
                                  .getPayload()
                                  .get("role");

        assertEquals(actualAuthority, expectedRole.name());
    }

    public static void assertJwtIssuedAt(String actualJwt) {
        var actualIssuedAt = Jwts.parser()
                                 .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                 .build()
                                 .parseSignedClaims(actualJwt)
                                 .getPayload()
                                 .getIssuedAt();

        assertNotNull(actualIssuedAt);
    }

    public static void assertJwtExpiresAt(String actualJwt) {
        var actualExpiresAt = Jwts.parser()
                                  .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                  .build()
                                  .parseSignedClaims(actualJwt)
                                  .getPayload()
                                  .getExpiration();

        assertNotNull(actualExpiresAt);
    }

}
