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
package com.bcn.asapp.uaa.auth;

import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REFRESH_TOKEN_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_REVOKE_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_ROOT_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_TOKEN_PATH;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST API interface that exposes user authentication endpoints.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Tag(name = "UAA operations", description = "REST API interface for User Authentication and Authorization (UAA) operations")
@RequestMapping(AUTH_ROOT_PATH)
public interface AuthRestAPI {

    /**
     * Authenticates a user with given credentials and returns authentication tokens.
     * <p>
     * In case the given user is already authenticated generates new authentication tokens overriding the existing ones.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-OK : The user has been authenticated successfully.</li>
     * <li>401-UNAUTHORIZED : The user could not be authenticated.</li>
     * </ul>
     *
     * @param userCredentials the user credentials (username and password), must not be {@literal null} and must be valid
     * @return a {@link ResponseEntity} wrapping {@link JwtAuthenticationDTO} with access and refresh tokens upon successful authentication
     * @throws AuthenticationException if authentication fails due to invalid credentials or other errors
     */
    @Operation(summary = "Authenticates a user with the given credentials", description = "Authenticates a user with given credentials and returns authentication tokens, in case the given user is already authenticated generates new authentication tokens overriding the existing ones")
    @ApiResponse(responseCode = "200", description = "The user has been authenticated successfully", content = {
            @Content(schema = @Schema(implementation = JwtAuthenticationDTO.class)) })
    @ApiResponse(responseCode = "401", description = "The user could not be authenticated", content = { @Content })
    @PostMapping(value = AUTH_TOKEN_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<JwtAuthenticationDTO> authenticate(@Valid @RequestBody UserCredentialsDTO userCredentials);

    /**
     * Refreshes an existing JWT authentication token using a valid refresh token.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-OK : The authentication token has been refreshed successfully.</li>
     * <li>401-UNAUTHORIZED :The refresh token is invalid, expired, or the refresh process fails.</li>
     * </ul>
     *
     * @param refreshToken the refresh token DTO used to obtain new tokens, must not be {@literal null}
     * @return a {@link ResponseEntity} containing the refreshed {@link JwtAuthenticationDTO} with the refreshed access and refresh tokens
     * @throws AuthenticationException if the refresh token is invalid, expired, or cannot be processed
     */
    @Operation(summary = "Refreshes the JWT authentication token", description = "Refreshes an existing JWT authentication token using a valid refresh token")
    @ApiResponse(responseCode = "200", description = "The authentication token has been refreshed successfully", content = {
            @Content(schema = @Schema(implementation = JwtAuthenticationDTO.class)) })
    @ApiResponse(responseCode = "401", description = "The refresh token is invalid, expired, or the refresh process fails", content = { @Content })
    @PostMapping(value = AUTH_REFRESH_TOKEN_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<JwtAuthenticationDTO> refreshAuthentication(@Valid @RequestBody RefreshTokenDTO refreshToken);

    /**
     * Revokes the JWT authentication of a user by invalidating the provided access token.
     * <p>
     * Invalidates the JWT authentication (access and refresh tokens), effectively logging out the user.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: The JWT authentication has been revoked successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: The access token is invalid, expired, or the revocation process fails.</li>
     * </ul>
     *
     * @param accessToken the access token DTO to be invalidated
     * @return a {@link ResponseEntity} with no content if the revocation was successful
     * @throws AuthenticationException if the access token is invalid, expired, or cannot be processed
     */
    @Operation(summary = "Revokes the JWT authentication", description = "Revokes the JWT authentication by invalidating the given access token.")
    @ApiResponse(responseCode = "200", description = "The JWT authentication has been revoked successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = { @Content })
    @ApiResponse(responseCode = "401", description = "The access token is invalid, expired, or the revocation process fails", content = { @Content })
    @PostMapping(value = AUTH_REVOKE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<Void> revokeAuthentication(@Valid @RequestBody AccessTokenDTO accessToken);

}
