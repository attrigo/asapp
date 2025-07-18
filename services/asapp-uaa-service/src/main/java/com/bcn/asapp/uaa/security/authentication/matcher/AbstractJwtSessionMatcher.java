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

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bcn.asapp.uaa.security.authentication.DecodedJwt;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

/**
 * Abstract base implementation of {@link JwtSessionMatcher} for matching a JWT against user session.
 * <p>
 * This class encapsulates the common workflow for loading the {@link User} by username from the JWT and delegates the actual JWT-to-session matching logic to
 * subclasses via the {@link #matchUserWithJwt(User, String)} method.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public abstract class AbstractJwtSessionMatcher implements JwtSessionMatcher {

    /**
     * Repository for managing user entities.
     */
    private final UserRepository userRepository;

    /**
     * Constructs a new {@code AbstractJwtSessionMatcher} with the specified {@link UserRepository}.
     *
     * @param userRepository the repository for managing user entities
     */
    protected AbstractJwtSessionMatcher(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Attempts to match the provided {@link DecodedJwt} with the session data of the corresponding {@link User}.
     * <p>
     * The method delegates to {@link #matchUserWithJwt(User, String)} to perform the actual matching logic.
     *
     * @param jwtToMatch the decoded JWT to match
     * @return {@code true} if the JWT matches the user's session data, {@code false} otherwise
     * @throws UsernameNotFoundException if no user is found for the JWT subject
     */
    @Override
    public final Boolean match(DecodedJwt jwtToMatch) {
        var username = jwtToMatch.getSubject();
        var jwt = jwtToMatch.getJwt();

        var user = userRepository.findByUsername(username)
                                 .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return matchUserWithJwt(user, jwt);
    }

    /**
     * Performs the domain-specific logic to determine whether the provided JWT matches the session state of the given {@link User}.
     * <p>
     * Implementing classes must define the criteria by which a JWT is considered a match for the given {@link User}.
     *
     * @param user the user whose session is being validated
     * @param jwt  the raw JWT string
     * @return {@code true} if the user session matches the JWT, {@code false} otherwise
     */
    protected abstract Boolean matchUserWithJwt(User user, String jwt);

}
