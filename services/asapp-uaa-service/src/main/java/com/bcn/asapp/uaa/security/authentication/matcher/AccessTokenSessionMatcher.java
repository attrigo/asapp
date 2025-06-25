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
package com.bcn.asapp.uaa.security.authentication.matcher;

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.User;
import com.bcn.asapp.uaa.security.core.UserRepository;

/**
 * Concrete implementation of {@link AbstractJwtSessionMatcher} for matching access tokens against user session.
 * <p>
 * This matcher verifies whether the provided access token is stored in the {@link AccessTokenRepository} for the specified user, effectively confirming that
 * the JWT represents an active and recognized session token.
 * <p>
 * Used primarily to ensure that tokens presented in authentication workflows correspond to tokens previously issued and persisted in the system
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Component
public class AccessTokenSessionMatcher extends AbstractJwtSessionMatcher {

    /**
     * Repository for retrieving access token information.
     */
    private final AccessTokenRepository accessTokenRepository;

    /**
     * Constructs a new {@code AccessTokenSessionMatcher} with the specified dependencies.
     *
     * @param userRepository        the repository used to load users by username
     * @param accessTokenRepository the repository used to check for JWT existence
     */
    public AccessTokenSessionMatcher(UserRepository userRepository, AccessTokenRepository accessTokenRepository) {
        super(userRepository);
        this.accessTokenRepository = accessTokenRepository;
    }

    /**
     * Determines if the given JWT is associated with the specified {@link User} by checking its presence in the {@link AccessTokenRepository}.
     *
     * @param user the user whose access tokens are to be checked
     * @param jwt  the raw JWT string to validate
     * @return {@code true} if the JWT is persisted and linked to the user, {@code false} otherwise
     */
    @Override
    public Boolean matchUserWithJwt(User user, String jwt) {
        return accessTokenRepository.existsByUserIdAndJwt(user.id(), jwt);
    }

}
