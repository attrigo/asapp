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

package com.bcn.asapp.authentication.infrastructure.user.in.request;

import org.apache.commons.lang3.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.bcn.asapp.authentication.domain.user.Role;

/**
 * Validator for the {@link ValidRole} annotation.
 * <p>
 * Validates that a string is not blank and corresponds to a valid {@link Role} enum value.
 *
 * @since 0.2.0
 * @see ConstraintValidator
 * @author attrigo
 */
public class ValidRoleValidator implements ConstraintValidator<ValidRole, String> {

    /**
     * Validates that the value represents a valid role.
     * <p>
     * Checks if the value is blank or does not match any {@link Role} enum value.
     *
     * @param value   the string to validate
     * @param context the validation context
     * @return {@code true} if valid, {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        try {
            Role.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
