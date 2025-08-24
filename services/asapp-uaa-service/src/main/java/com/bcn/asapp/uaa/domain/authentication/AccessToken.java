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

package com.bcn.asapp.uaa.domain.authentication;

import java.time.Instant;

public record AccessToken(
        String jwt,
        Instant issuedAt,
        Instant expiresAt
) {

    public AccessToken {
        validate();
    }

    // TODO: Should put the validation in a separate method or keep it in constructor
    private void validate() {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("JWT must not be null or empty");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("Issued at timestamp must not be null");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expires at timestamp must not be null");
        }
        if (issuedAt.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Issued at timestamp must be before expires at timestamp");
        }
    }

}
