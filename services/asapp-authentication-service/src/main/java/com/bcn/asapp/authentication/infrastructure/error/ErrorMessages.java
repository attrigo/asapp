/**
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bcn.asapp.authentication.infrastructure.error;

/**
 * Centralizes error titles, fixed detail messages, and machine-readable error codes used by
 * {@link GlobalExceptionHandler}.
 *
 * @since 0.4.0
 * @author attrigo
 */
final class ErrorMessages {

    // Titles
    static final String BAD_REQUEST_TITLE = "Bad Request";

    static final String INVALID_ARGUMENT_TITLE = "Invalid Argument";

    static final String AUTHENTICATION_FAILED_TITLE = "Authentication Failed";

    static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";

    static final String SERVICE_UNAVAILABLE_TITLE = "Service Unavailable";

    // Fixed details
    static final String VALIDATION_FAILED_DETAIL = "Request validation failed";

    static final String INVALID_ARGUMENT_DETAIL = "Invalid argument provided";

    static final String INVALID_CREDENTIALS_DETAIL = "Invalid credentials";

    static final String INVALID_TOKEN_DETAIL = "Invalid token";

    static final String INTERNAL_ERROR_DETAIL = "An internal error occurred";

    static final String SERVICE_UNAVAILABLE_DETAIL = "Service temporarily unavailable";

    // Error codes
    static final String ERROR_PROPERTY = "error";

    static final String INVALID_GRANT_ERROR = "invalid_grant";

    static final String SERVER_ERROR = "server_error";

    static final String TEMPORARILY_UNAVAILABLE_ERROR = "temporarily_unavailable";

    private ErrorMessages() {
    }

}
