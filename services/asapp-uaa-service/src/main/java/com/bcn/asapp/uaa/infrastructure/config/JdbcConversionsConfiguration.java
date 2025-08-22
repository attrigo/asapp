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

package com.bcn.asapp.uaa.infrastructure.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.bcn.asapp.uaa.infrastructure.authentication.out.entity.JwtClaimsEntity;

/**
 * Configuration class for custom JDBC type conversions.
 * <p>
 * Extends {@link AbstractJdbcConfiguration} to register custom converters for mapping between domain objects and PostgresSQL-specific types, particularly for
 * JWT claims stored as JSONB.
 *
 * @since 0.2.0
 * @see AbstractJdbcConfiguration
 * @author attrigo
 */
@Configuration
public class JdbcConversionsConfiguration extends AbstractJdbcConfiguration {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code JdbcConversionsConfiguration} with required dependencies.
     *
     * @param objectMapper the Jackson ObjectMapper for JSON serialization
     */
    public JdbcConversionsConfiguration(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Registers custom JDBC converters.
     *
     * @return a {@link List} of custom converters for JWT claims
     */
    @Override
    protected List<?> userConverters() {
        return Arrays.asList(new ClaimsReadingConverter(objectMapper), new ClaimsWritingConverter(objectMapper));
    }

    /**
     * Converter for reading JWT claims from PostgresSQL JSONB to domain entity.
     * <p>
     * Converts PostgresSQL {@link PGobject} JSONB values to {@link JwtClaimsEntity} instances.
     *
     * @author attrigo
     * @since 0.2.0
     */
    @ReadingConverter
    static class ClaimsReadingConverter implements Converter<PGobject, JwtClaimsEntity> {

        private final ObjectMapper objectMapper;

        /**
         * Constructs a new {@code ClaimsReadingConverter}.
         *
         * @param objectMapper the Jackson ObjectMapper for JSON deserialization
         */
        public ClaimsReadingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        /**
         * Converts a PostgresSQL JSONB object to a JWT claims entity.
         *
         * @param source the {@link PGobject} containing JSONB data
         * @return the {@link JwtClaimsEntity} containing parsed claims
         * @throws IllegalArgumentException if JSON parsing fails
         */
        @Override
        public JwtClaimsEntity convert(PGobject source) {
            try {
                var claimsMap = objectMapper.readValue(source.getValue(), new TypeReference<HashMap<String, Object>>() {});
                return new JwtClaimsEntity(claimsMap);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to convert JSON string to JWT claims", e);
            }
        }

    }

    /**
     * Converter for writing JWT claims from domain entity to PostgresSQL JSONB.
     * <p>
     * Converts {@link JwtClaimsEntity} instances to PostgresSQL {@link PGobject} JSONB values.
     *
     * @author attrigo
     * @since 0.2.0
     */
    @WritingConverter
    static class ClaimsWritingConverter implements Converter<JwtClaimsEntity, PGobject> {

        private final ObjectMapper objectMapper;

        /**
         * Constructs a new {@code ClaimsWritingConverter}.
         *
         * @param objectMapper the Jackson ObjectMapper for JSON serialization
         */
        public ClaimsWritingConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        /**
         * Converts a JWT claims entity to a PostgresSQL JSONB object.
         *
         * @param source the {@link JwtClaimsEntity} to convert
         * @return the {@link PGobject} containing JSONB data
         * @throws IllegalArgumentException if JSON serialization fails
         */
        @Override
        public PGobject convert(JwtClaimsEntity source) {
            try {
                String json = objectMapper.writeValueAsString(source.claims());
                var jsonObject = new PGobject();
                jsonObject.setType("jsonb");
                jsonObject.setValue(json);
                return jsonObject;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to convert JWT claim to PGobject", e);
            }
        }

    }

}
