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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import com.bcn.asapp.authentication.domain.user.Role;

class CustomUserDetailsTests {

    private final UUID userIdValue = UUID.fromString("ce1dd321-b023-4abf-8af9-eb4c69ebb4e0");

    private final String usernameValue = "user@asapp.com";

    private final String passwordValue = "{bcrypt}password";

    @Nested
    class CreateCustomUserDetails {

        @Test
        void ThenReturnsCustomUserDetailsWithEmptyAuthorities_GivenWithEmptyAuthorities() {
            // Given
            var emptyAuthorities = AuthorityUtils.NO_AUTHORITIES;

            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, emptyAuthorities);

            // Then
            assertThat(actual.getAuthorities()).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsCustomUserDetails_GivenAllParametersAreValid(Role role) {
            // Given
            var authorities = AuthorityUtils.createAuthorityList(role.name());

            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, authorities);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.getUserId()).isEqualTo(userIdValue);
            assertThat(actual.getUsername()).isEqualTo(usernameValue);
            assertThat(actual.getPassword()).isEqualTo(passwordValue);
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly(role.name());
        }

        @Test
        void ThenReturnsCustomUserDetailsWithMultipleAuthorities_GivenMultipleAuthorities() {
            // Given
            var multipleAuthorities = AuthorityUtils.createAuthorityList(USER.name(), ADMIN.name());

            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, multipleAuthorities);

            // Then
            assertThat(actual.getAuthorities()).hasSize(2)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactlyInAnyOrder(USER.name(), ADMIN.name());
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ThenReturnsUserId() {
            // Given
            var authorities = AuthorityUtils.createAuthorityList(USER.name());
            var userDetails = new CustomUserDetails(userIdValue, usernameValue, passwordValue, authorities);

            // When
            var actual = userDetails.getUserId();

            // Then
            assertThat(actual).isEqualTo(userIdValue);
        }

    }

}
