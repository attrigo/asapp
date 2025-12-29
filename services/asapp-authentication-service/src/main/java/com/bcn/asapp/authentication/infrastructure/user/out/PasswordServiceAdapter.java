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

package com.bcn.asapp.authentication.infrastructure.user.out;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.PasswordService;
import com.bcn.asapp.authentication.domain.user.RawPassword;

/**
 * Adapter implementation of {@link PasswordService} using Spring Security's password encoding.
 * <p>
 * Bridges the domain layer with Spring Security's {@link PasswordEncoder} infrastructure, providing password hashing capabilities while keeping the domain
 * layer framework-agnostic.
 *
 * @since 0.2.0
 * @see PasswordEncoder
 * @author attrigo
 */
@Component
public class PasswordServiceAdapter implements PasswordService {

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a new {@code PasswordServiceAdapter} with required dependencies.
     *
     * @param passwordEncoder the Spring Security password encoder
     */
    public PasswordServiceAdapter(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Encodes a raw password into its hashed representation.
     *
     * @param rawPassword the {@link RawPassword} to encode
     * @return the {@link EncodedPassword} with algorithm prefix
     * @throws IllegalArgumentException if the raw password is invalid
     */
    @Override
    public EncodedPassword encode(RawPassword rawPassword) {
        var encodedPassword = passwordEncoder.encode(rawPassword.value());

        return EncodedPassword.of(encodedPassword);
    }

}
