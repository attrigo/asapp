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

package com.bcn.asapp.authentication.domain.user;

/**
 * Domain service responsible for password encoding operations.
 * <p>
 * Provides functionality to transform raw passwords into their encoded form, abstracting the underlying hashing algorithm implementation.
 *
 * @since 0.2.0
 * @author attrigo
 */
public interface PasswordService {

    /**
     * Encodes a raw password into its hashed representation.
     *
     * @param rawPassword the {@link RawPassword} to encode
     * @return the {@link EncodedPassword} with algorithm prefix
     */
    EncodedPassword encode(RawPassword rawPassword);

}
