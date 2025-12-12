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

package com.bcn.asapp.authentication.application.authentication.out;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.infrastructure.security.DecodedJwt;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;

/**
 * Port for decoding and validating authentication tokens.
 * <p>
 * Defines the contract for decoding encoded tokens, verifying their signature and expiration, and extracting their claims.
 *
 * @since 0.2.0
 * @author attrigo
 */
@FunctionalInterface
public interface TokenDecoder {

    /**
     * Decodes and validates an encoded token.
     * <p>
     * Parses the token, verifies its signature and expiration, and extracts the claims.
     *
     * @param encodedToken the {@link EncodedToken} to decode
     * @return the {@link DecodedJwt} containing the decoded token information
     * @throws InvalidJwtException if the token is invalid, malformed, or expired
     */
    DecodedJwt decode(EncodedToken encodedToken);

}
