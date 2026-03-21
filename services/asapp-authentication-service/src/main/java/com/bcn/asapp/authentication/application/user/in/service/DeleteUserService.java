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

package com.bcn.asapp.authentication.application.user.in.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.bcn.asapp.authentication.application.ApplicationService;
import com.bcn.asapp.authentication.application.PersistenceException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.user.in.DeleteUserUseCase;
import com.bcn.asapp.authentication.application.user.out.UserRepository;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Application service responsible for orchestrating user deletion operations.
 * <p>
 * Coordinates the user deletion workflow including removal from persistent storage and token revocation from fast-access store.
 * <p>
 * <strong>Orchestration Flow:</strong>
 * <ol>
 * <li>Retrieves all JWT pairs for the user</li>
 * <li>Deletes all JWT authentications from repository</li>
 * <li>Deletes user from repository</li>
 * <li>Deletes all token pairs from fast-access store (immediate revocation)</li>
 * </ol>
 * <p>
 * Token deactivation occurs after successful repository deletion, ensuring no compensation is needed if repository operations fail.
 *
 * @since 0.2.0
 * @author attrigo
 */
@ApplicationService
public class DeleteUserService implements DeleteUserUseCase {

    private final JwtStore jwtStore;

    private final UserRepository userRepository;

    private final JwtAuthenticationRepository jwtAuthenticationRepository;

    /**
     * Constructs a new {@code DeleteUserService} with required dependencies.
     *
     * @param jwtStore                    the JWT store for fast-access store operations
     * @param userRepository              the repository for user data access
     * @param jwtAuthenticationRepository the repository for JWT authentications data access
     */
    public DeleteUserService(JwtStore jwtStore, UserRepository userRepository, JwtAuthenticationRepository jwtAuthenticationRepository) {
        this.jwtStore = jwtStore;
        this.userRepository = userRepository;
        this.jwtAuthenticationRepository = jwtAuthenticationRepository;
    }

    /**
     * Deletes an existing user by its unique identifier.
     * <p>
     * Orchestrates the user deletion workflow: removal from repository, then token revocation from fast-access store.
     * <p>
     * Repository operations run first within the transaction. Token deactivation is performed only after successful deletion, so no compensation is required if
     * repository operations fail.
     *
     * @param id the user's unique identifier
     * @return {@code true} if the user was deleted, {@code false} if not found
     * @throws IllegalArgumentException if the id is invalid
     * @throws PersistenceException     if user or authentication deletion fails
     */
    @Override
    @Transactional
    public Boolean deleteUserById(UUID id) {
        var userId = UserId.of(id);

        var jwtPairs = retrieveJwtPairs(userId);

        deleteUserAuthentications(userId);
        var deleted = deleteUser(userId);

        if (deleted) {
            deactivateAllTokens(jwtPairs);
        }

        return deleted;
    }

    /**
     * Retrieves all JWT pairs for the user from their authentications.
     *
     * @param userId the user's unique identifier
     * @return the list of {@link JwtPair} for the user
     */
    private List<JwtPair> retrieveJwtPairs(UserId userId) {
        return jwtAuthenticationRepository.findAllByUserId(userId)
                                          .stream()
                                          .map(JwtAuthentication::getJwtPair)
                                          .toList();
    }

    /**
     * Deactivates all token pairs by removing them from the fast-access store.
     *
     * @param jwtPairs the {@link JwtPair} to deactivate
     */
    private void deactivateAllTokens(List<JwtPair> jwtPairs) {
        jwtPairs.forEach(jwtStore::delete);
    }

    /**
     * Deletes all JWT authentications for the user from the repository.
     *
     * @param userId the user's unique identifier
     */
    private void deleteUserAuthentications(UserId userId) {
        jwtAuthenticationRepository.deleteAllByUserId(userId);
    }

    /**
     * Deletes the user from the repository by identifier.
     *
     * @param userId the user's unique identifier
     * @return {@code true} if deleted, {@code false} if not found
     */
    private Boolean deleteUser(UserId userId) {
        return userRepository.deleteById(userId);
    }

}
