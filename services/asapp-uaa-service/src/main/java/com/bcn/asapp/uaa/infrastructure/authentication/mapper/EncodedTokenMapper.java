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

import com.bcn.asapp.uaa.domain.authentication.EncodedToken;

/**
 * MapStruct mapper for converting between {@link EncodedToken} and {@link String}.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring")
public interface EncodedTokenMapper {

    /**
     * Maps a {@link String} encoded token value to a {@link EncodedToken} value object.
     *
     * @param token the token value
     * @return the {@link EncodedToken}
     */
    EncodedToken toEncodedToken(String token);

    /**
     * Maps a {@link EncodedToken} value object to a {@link String} encoded token value.
     *
     * @param encodedToken the {@link EncodedToken}
     * @return the token value, or {@code null} if encodedToken is {@code null}
     */
    default String toString(EncodedToken encodedToken) {
        return encodedToken != null ? encodedToken.value() : null;
    }

}
