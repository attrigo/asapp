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

/**
 * MapStruct mapper for converting between {@link Expiration} and {@link Instant}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface ExpirationMapper {

    /**
     * Maps a {@link Instant} expiration value to an {@link Expiration} value object.
     *
     * @param expiration the expiration instant value
     * @return the {@link Expiration}
     */
    Expiration toExpiration(Instant expiration);

    /**
     * Maps a {@link Expiration} value object to a {@link Instant} expiration value.
     *
     * @param expiration the {@link Expiration}
     * @return the expiration instant value, or {@code null} if expiration is {@code null}
     */
    default Instant toInstant(Expiration expiration) {
        return expiration != null ? expiration.value() : null;
    }

}
