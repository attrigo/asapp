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

import java.time.Instant;

import org.mapstruct.Mapper;

import com.bcn.asapp.uaa.domain.authentication.Issued;

/**
 * MapStruct mapper for converting between {@link Issued} and {@link Instant}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface IssuedMapper {

    /**
     * Maps a {@link Instant} issued value to an {@link Issued} value object.
     *
     * @param issued the issued instant value
     * @return the {@link Issued}
     */
    Issued toIssued(Instant issued);

    /**
     * Maps a {@link Issued} value object to a {@link Instant} issued value.
     *
     * @param issued the {@link Issued}
     * @return the issued instant value, or {@code null} if issued is {@code null}
     */
    default Instant toInstant(Issued issued) {
        return issued != null ? issued.value() : null;
    }

}
