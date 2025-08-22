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

package com.bcn.asapp.uaa.infrastructure.authentication.mapper;

import org.mapstruct.Mapper;

import com.bcn.asapp.uaa.domain.authentication.Subject;

/**
 * MapStruct mapper for converting between {@link Subject} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface SubjectMapper {

    /**
     * Maps a {@link String} subject value to a {@link Subject} value object.
     *
     * @param subject the subject value
     * @return the {@link Subject}
     */
    Subject toSubject(String subject);

    /**
     * Maps a {@link Subject} value object to a {@link String} subject value.
     *
     * @param subject the {@link Subject}
     * @return the subject value, or {@code null} if subject is {@code null}
     */
    default String toString(Subject subject) {
        return subject != null ? subject.value() : null;
    }

}
