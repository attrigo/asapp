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

package com.bcn.asapp.authentication.infrastructure.authentication.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;

import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Inactivated;

/**
 * MapStruct mapper for converting between {@link Inactivated} and {@link Instant}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface InactivatedMapper {

    /**
     * Maps a {@link Instant} inactivated value to an {@link Inactivated} value object.
     *
     * @param inactivated the inactivated instant value
     * @return the {@link Expiration}
     */
    Inactivated toInactivated(Instant inactivated);

    /**
     * Maps a {@link Inactivated} value object to a {@link Instant} inactivated value.
     *
     * @param inactivated the {@link Expiration}
     * @return the inactivated instant value, or {@code null} if inactivated is {@code null}
     */
    default Instant toInstant(Inactivated inactivated) {
        return inactivated != null ? inactivated.value() : null;
    }

}
