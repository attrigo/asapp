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
package com.bcn.asapp.dto.utils;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Custom JSON serializer that masks sensitive string values during serialization.
 * <p>
 * This serializer replaces the actual string value with a fixed masked string ("********") to prevent sensitive data exposure in JSON output.
 * </p>
 *
 * @author ttrigo
 * @since 0.2.0
 */
public class MaskSensibleStringSerializer extends JsonSerializer<String> {

    /**
     * Serializes the given string value by writing a masked placeholder instead of the actual content.
     *
     * @param value       the original string value to be masked
     * @param gen         the JSON generator used to write JSON content
     * @param serializers the provider that can be used to get serializers for serializing Objects value contains
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString("********");
    }

}
