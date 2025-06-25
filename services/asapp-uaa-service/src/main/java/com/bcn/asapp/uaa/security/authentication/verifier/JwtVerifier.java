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
package com.bcn.asapp.uaa.security.authentication.verifier;

import com.bcn.asapp.uaa.security.authentication.JwtAuthenticationToken;

/**
 * Defines the contract for verifying JSON Web Tokens (JWTs).
 *
 * <p>
 * Implementations are responsible for validating the integrity, type, and session association of a raw JWT string and returning an authenticated token.
 */
public interface JwtVerifier {

    /**
     * Verifies the provided raw JWT string and returns an authenticated token representation if verification succeeds.
     *
     * @param jwt the raw JWT string to verify
     * @return an authenticated {@link JwtAuthenticationToken} upon successful verification
     * @throws org.springframework.security.core.AuthenticationException if the JWT is invalid or verification fails
     */
    JwtAuthenticationToken verify(String jwt);

}
