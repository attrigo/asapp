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
package com.bcn.asapp.uaa.auth.internal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bcn.asapp.uaa.auth.AuthService;
import com.bcn.asapp.uaa.auth.AuthenticationDTO;
import com.bcn.asapp.uaa.auth.UserCredentialsDTO;
import com.bcn.asapp.uaa.config.security.JwtTokenProvider;

/**
 * Standard implementation of {@link AuthService}.
 *
 * @author ttrigo
 * @see AuthenticationManager
 * @see SecurityContextHolder
 * @since 0.2.0
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Main constructor.
     *
     * @param authenticationManager the authentication manager used to authenticate the user.
     * @param jwtTokenProvider      the JWT provider used to perform JWT operations.
     */
    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticates the user based on the given credentials and generates a JWT upon successful authentication.
     * <p>
     * To correctly authenticate a user the operation must do the following actions:
     * <ul>
     * <li>Authenticate the user using the {@link AuthenticationManager}.</li>
     * <li>Set the authentication to the {@link SecurityContextHolder}</li>
     * <li>Generates a JWT using the {@link JwtTokenProvider}.</li>
     * </ul>
     */
    @Override
    public AuthenticationDTO login(UserCredentialsDTO userCredentials) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userCredentials.username(), userCredentials.password()));

        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);

        var jwt = jwtTokenProvider.generateToken(authentication);

        return new AuthenticationDTO(jwt);
    }

}
