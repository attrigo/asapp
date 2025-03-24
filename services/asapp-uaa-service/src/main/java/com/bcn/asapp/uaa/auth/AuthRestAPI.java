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

import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_LOGIN_PATH;
import static com.bcn.asapp.url.uaa.AuthRestAPIURL.AUTH_ROOT_PATH;

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
 * Defines the RESTful API for handling user authentication operations.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Tag(name = "UAA operations", description = "Defines the RESTful API for handling user authentication operations")
@RequestMapping(AUTH_ROOT_PATH)
public interface AuthRestAPI {

    /**
     * Logs in the user with the given credentials.
     * <p>
     * Response codes:
     * <ul>
     * <li>20O-CREATED : The user has been logged in.</li>
     * <li>401-UNAUTHORIZED : The user could not log in.</li>
     * </ul>
     *
     * @param userCredentials the user credentials provided by the user (username and password).
     * @return a {@link ResponseEntity} wrapping the authentication details if the login is successful.
     * @throws AuthenticationException if the credentials are invalid.
     */
    @Operation(summary = "Logs in the user with the given credentials", description = "Logins the given user and returns the authentication details")
    @ApiResponse(responseCode = "200", description = "The user has been logged in", content = {
            @Content(schema = @Schema(implementation = AuthenticationDTO.class)) })
    @ApiResponse(responseCode = "401", description = "The user could not log in", content = { @Content })
    @PostMapping(value = AUTH_LOGIN_PATH, consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    ResponseEntity<AuthenticationDTO> login(@Valid @RequestBody UserCredentialsDTO userCredentials);

}
