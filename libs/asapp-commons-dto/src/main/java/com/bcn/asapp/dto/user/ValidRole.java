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
package com.bcn.asapp.dto.user;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validation constraint annotation to ensure that a string value represents a valid Role.
 * <p>
 * This annotation is validated by a {@link jakarta.validation.ConstraintValidator} implementation that is discovered at runtime through the Service Provider
 * Interface (SPI) mechanism. The validator implementation must be registered in META-INF/services/jakarta.validation.ConstraintValidator.
 *
 * @author ttrigo
 * @see jakarta.validation.ConstraintValidator
 * @since 0.2.0
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Documented
public @interface ValidRole {

    /**
     * @return the error message template
     */
    String message() default "Invalid role value";

    /**
     * @return the validation groups to which this constraint belongs
     */
    Class<?>[] groups() default {};

    /**
     * @return the payload associated with the constraint
     */
    Class<? extends Payload>[] payload() default {};

}
