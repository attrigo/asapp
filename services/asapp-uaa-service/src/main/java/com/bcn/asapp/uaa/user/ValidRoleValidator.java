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
package com.bcn.asapp.uaa.user;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.bcn.asapp.dto.user.ValidRole;

/**
 * Validator implementation for the {@link ValidRole} constraint annotation.
 * <p>
 * This validator checks if a given string value corresponds to a valid {@link Role} enum value.
 * <p>
 * This implementation is discovered at runtime through the Service Provider Interface (SPI) mechanism and must be registered in
 * META-INF/services/jakarta.validation.ConstraintValidator.
 *
 * @author ttrigo
 * @see jakarta.validation.ConstraintValidator
 * @see ValidRole
 * @since 0.2.0
 */
public class ValidRoleValidator implements ConstraintValidator<ValidRole, String> {

    /**
     * Validates if the given value represents a valid {@link Role}.
     *
     * @param value   the string value to validate, may be null
     * @param context context in which the constraint is evaluated
     * @return {@code true} if the value is null or represents a valid {@link Role}, otherwise {@code false}
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
