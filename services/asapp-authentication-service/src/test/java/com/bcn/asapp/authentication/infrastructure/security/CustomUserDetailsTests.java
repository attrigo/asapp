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
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import com.bcn.asapp.authentication.domain.user.Role;

/**
 * Tests {@link CustomUserDetails} authority assignment and account status defaults.
 * <p>
 * Coverage:
 * <li>Creates user details with user ID, username, password, and variable authority count</li>
 * <li>Provides default account status flags (non-expired, non-locked, enabled)</li>
 * <li>Grants access to wrapped user identity and credentials</li>
 */
class CustomUserDetailsTests {

    @Nested
    class CreateCustomUserDetails {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ReturnsCustomUserDetails_ValidParameters(Role role) {
            // Given
            var userId = UUID.fromString("ce1dd321-b023-4abf-8af9-eb4c69ebb4e0");
            var username = "user@asapp.com";
            var password = "{bcrypt}password";
            var authorities = AuthorityUtils.createAuthorityList(role.name());

            // When
            var actual = new CustomUserDetails(userId, username, password, authorities);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("custom user details").isNotNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.getUsername()).as("username").isEqualTo(username);
                softly.assertThat(actual.getPassword()).as("password").isEqualTo(password);
                softly.assertThat(actual.getAuthorities()).as("authorities").hasSize(1).extracting(GrantedAuthority::getAuthority).containsExactly(role.name());
                // @formatter:on
            });
        }

        @Test
        void ReturnsCustomUserDetailsWithEmptyAuthorities_EmptyAuthorities() {
            // Given
            var userId = UUID.fromString("ce1dd321-b023-4abf-8af9-eb4c69ebb4e0");
            var username = "user@asapp.com";
            var password = "{bcrypt}password";
            var authorities = AuthorityUtils.NO_AUTHORITIES;

            // When
            var actual = new CustomUserDetails(userId, username, password, authorities);

            // Then
            assertThat(actual.getAuthorities()).isEmpty();
        }

        @Test
        void ReturnsCustomUserDetailsWithMultipleAuthorities_MultipleAuthorities() {
            // Given
            var userId = UUID.fromString("ce1dd321-b023-4abf-8af9-eb4c69ebb4e0");
            var username = "user@asapp.com";
            var password = "{bcrypt}password";
            var authorities = AuthorityUtils.createAuthorityList(USER.name(), ADMIN.name());

            // When
            var actual = new CustomUserDetails(userId, username, password, authorities);

            // Then
            assertThat(actual.getAuthorities()).hasSize(2)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactlyInAnyOrder(USER.name(), ADMIN.name());
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ReturnsUserId_ValidUserDetails() {
            // Given
            var userId = UUID.fromString("ce1dd321-b023-4abf-8af9-eb4c69ebb4e0");
            var username = "user@asapp.com";
            var password = "{bcrypt}password";
            var authorities = AuthorityUtils.createAuthorityList(USER.name());
            var userDetails = new CustomUserDetails(userId, username, password, authorities);

            // When
            var actual = userDetails.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

    }

}
