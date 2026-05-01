/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.testutil;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;

import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;

/**
 * Provides field descriptors enriched with Bean Validation constraint descriptions for use in Spring REST Docs snippets.
 *
 * @since 0.3.0
 * @author attrigo
 */
public class RestDocsConstrainedFields {

    private final ConstraintDescriptions constraintDescriptions;

    /**
     * Constructs a new {@code RestDocsConstrainedFields} for the given request class.
     *
     * @param input the request class to read constraints from
     */
    public RestDocsConstrainedFields(Class<?> input) {
        this.constraintDescriptions = new ConstraintDescriptions(input);
    }

    /**
     * Returns a field descriptor for the given path, enriched with its Bean Validation constraints.
     * <p>
     * Use when the JSON path matches the Java property name.
     *
     * @param path the JSON path and Java property name
     * @return the enriched field descriptor
     */
    public FieldDescriptor withPath(String path) {
        return withPath(path, path);
    }

    /**
     * Returns a field descriptor for the given path, enriched with its Bean Validation constraints.
     * <p>
     * Use when the JSON path differs from the Java property name (e.g. {@code user_id} vs {@code userId}).
     *
     * @param jsonPath     the JSON field path used in the snippet
     * @param javaProperty the Java property name used to look up constraints
     * @return the enriched field descriptor
     */
    public FieldDescriptor withPath(String jsonPath, String javaProperty) {
        return fieldWithPath(jsonPath).attributes(key("constraints").value(String.join(". ", constraintDescriptions.descriptionsForProperty(javaProperty))));
    }

}
