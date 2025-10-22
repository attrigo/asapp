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

package com.bcn.asapp.users.infrastructure.user.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.users.domain.user.PhoneNumber;

/**
 * MapStruct mapper for converting between {@link PhoneNumber} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface PhoneNumberMapper {

    /**
     * Maps a {@link String} phone number value to a {@link PhoneNumber} value object.
     *
     * @param phoneNumber the phone number value
     * @return the {@link PhoneNumber}
     */
    PhoneNumber toPhoneNumber(String phoneNumber);

    /**
     * Maps a {@link PhoneNumber} value object to a {@link String} phone number value.
     *
     * @param phoneNumber the {@link PhoneNumber}
     * @return the phone number value, or {@code null} if phone number is {@code null}
     */
    default String toString(PhoneNumber phoneNumber) {
        return phoneNumber != null ? phoneNumber.value() : null;
    }

}
