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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.uaa.auth.Role;

public class JwtAssertions {

    private static final String UT_JWT_SECRET = "Cnpr50yQ04Q5y7GFUvR3ODWLYRlPjeAgOy7Y0Woo6PCqiViiOxxS3vo1FOyjro7T";

    private JwtAssertions() {}

    public static void assertJwtUsername(String actualJwt, String expectedUsername) {
        var actualUsername = Jwts.parser()
                                 .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                 .build()
                                 .parseSignedClaims(actualJwt)
                                 .getPayload()
                                 .getSubject();

        assertEquals(actualUsername, expectedUsername);
    }

    public static void assertJwtAuthorities(String actualJwt, Role expectedRole) {
        var actualAuthority = Jwts.parser()
                                  .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(UT_JWT_SECRET)))
                                  .build()
                                  .parseSignedClaims(actualJwt)
                                  .getPayload()
                                  .get("role");

        assertEquals(actualAuthority, expectedRole.name());
    }

}
