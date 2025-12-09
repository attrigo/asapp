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

package com.bcn.asapp.authentication.infrastructure.authentication.in;

import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REFRESH_TOKEN_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_REVOKE_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_ROOT_PATH;
import static com.bcn.asapp.url.authentication.AuthenticationRestAPIURL.AUTH_TOKEN_PATH;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import com.bcn.asapp.authentication.infrastructure.authentication.in.request.AuthenticateRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RefreshAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.request.RevokeAuthenticationRequest;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.AuthenticateResponse;
import com.bcn.asapp.authentication.infrastructure.authentication.in.response.RefreshAuthenticationResponse;

/**
 * REST API contract for authentication operations.
 * <p>
 * Defines the HTTP endpoints for JWT authentication operations, including OpenAPI documentation.
 *
 * @since 0.2.0
 * @author attrigo
 */
@RequestMapping(AUTH_ROOT_PATH)
@Tag(name = "Authentication operations", description = "REST API contract for authentication operations")
public interface AuthenticationRestAPI {

    /**
     * Authenticates a user with the given credentials and provides a new JWT authentication.
     * <p>
     * Each authentication creates a new session with fresh tokens. Previous authentications for the user remain valid until they expire or are explicitly
     * revoked, allowing multiple concurrent sessions.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-OK: The user has been authenticated successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data</li>
     * <li>401-UNAUTHORIZED: The user could not be authenticated due to invalid credentials.</li>
     * </ul>
     *
     * @param request the {@link AuthenticateRequest} containing user credentials
     * @return the {@link AuthenticateResponse} containing access and refresh tokens
     */
    @PostMapping(value = AUTH_TOKEN_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticates a user with the given credentials and provides a new JWT authentication", description = "Authenticates a user by validating their credentials. Upon successful authentication, it generates and returns a new JWT authentication token pair (access and refresh tokens). Each authentication creates a new session; previous sessions remain valid until they expire or are explicitly revoked.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentication request containing the user's login credentials (username and password)", required = true, content = @Content(schema = @Schema(implementation = AuthenticateRequest.class)))
    @ApiResponse(responseCode = "200", description = "The user has been authenticated successfully", content = {
            @Content(schema = @Schema(implementation = AuthenticateResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The user could not be authenticated due to invalid credentials", content = { @Content })
    AuthenticateResponse authenticate(@RequestBody @Valid AuthenticateRequest request);

    /**
     * Refreshes a JWT authentication using a refresh token.
     * <p>
     * Updates the existing session with new tokens, invalidating the old ones.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-OK: The JWT authentication has been refreshed successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data</li>
     * <li>401-UNAUTHORIZED: The refresh token is invalid, expired, does not belong to an authenticated user, or the refresh process fails.</li>
     * </ul>
     *
     * @param request the {@link RefreshAuthenticationRequest} containing the refresh token
     * @return the {@link RefreshAuthenticationResponse} containing new access and refresh tokens
     */
    @PostMapping(value = AUTH_REFRESH_TOKEN_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Refreshes a JWT authentication using a refresh token", description = "Updates the existing session with new JWT tokens (both access and refresh), invalidating the old ones. Used to extend the session without requiring the user to re-authenticate.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Refresh authentication request containing the refresh token to be used for generating new tokens", required = true, content = @Content(schema = @Schema(implementation = RefreshAuthenticationRequest.class)))
    @ApiResponse(responseCode = "200", description = "The JWT authentication tokens have been refreshed successfully", content = {
            @Content(schema = @Schema(implementation = RefreshAuthenticationResponse.class)) })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The refresh token is invalid, expired, does not belong to an authenticated user, or the refresh process fails", content = {
            @Content })
    RefreshAuthenticationResponse refreshAuthentication(@RequestBody @Valid RefreshAuthenticationRequest request);

    /**
     * Revokes a JWT authentication using an access token.
     * <p>
     * Invalidates the JWT authentication, effectively logging out the user.
     * <p>
     * Response codes:
     * <ul>
     * <li>200-OK: The JWT authentication has been revoked successfully.</li>
     * <li>400-BAD_REQUEST: The request body is malformed or contains invalid data.</li>
     * <li>401-UNAUTHORIZED: The access token is invalid, expired, does not belong to an authenticated user, or the revoke process fails.</li>
     * </ul>
     *
     * @param request the {@link RevokeAuthenticationRequest} containing the access token
     */
    @PostMapping(value = AUTH_REVOKE_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Revokes a JWT authentication using an access token", description = "Invalidates the current JWT authentication by revoking the provided access token. This effectively logs out the user and prevents further use of the token for accessing protected resources.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Revoke authentication request containing the access token to be invalidated", required = true, content = @Content(schema = @Schema(implementation = RevokeAuthenticationRequest.class)))
    @ApiResponse(responseCode = "200", description = "The JWT authentication has been revoked successfully", content = { @Content })
    @ApiResponse(responseCode = "400", description = "The request body is malformed or contains invalid data", content = {
            @Content(schema = @Schema(implementation = ProblemDetail.class)) })
    @ApiResponse(responseCode = "401", description = "The access token is invalid, expired, does not belong to an authenticated user, or the revoke process fails", content = {
            @Content })
    void revokeAuthentication(@RequestBody @Valid RevokeAuthenticationRequest request);

}
