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

package com.bcn.asapp.tasks.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Tests that request and response DTOs expose camelCase JSON property names (no {@code @JsonProperty} renaming).
 *
 * @author attrigo
 */
@AnalyzeClasses(packages = "com.bcn.asapp.tasks.infrastructure", importOptions = ImportOption.DoNotIncludeTests.class)
class JsonNamingConventionTests {

    @ArchTest
    static final ArchRule requestResponseDtoFieldsUseCamelCaseJson = fields().that()
                                                                             .areDeclaredInClassesThat()
                                                                             .resideInAnyPackage("..in.request..", "..in.response..")
                                                                             .should(haveJsonNameMatchingFieldName());

    private static ArchCondition<JavaField> haveJsonNameMatchingFieldName() {
        return new ArchCondition<>("have a JSON property name matching the camelCase Java field name") {

            @Override
            public void check(JavaField field, ConditionEvents events) {
                field.tryGetAnnotationOfType(JsonProperty.class)
                     .ifPresent(annotation -> {
                         var fieldName = field.getName();
                         var jsonName = annotation.value();

                         if (!jsonName.isEmpty() && !jsonName.equals(fieldName)) {
                             var message = "%s is annotated @JsonProperty(\"%s\"); request/response JSON must be camelCase — remove the annotation or set the value to \"%s\"".formatted(
                                     field.getFullName(), jsonName, fieldName);
                             events.add(SimpleConditionEvent.violated(field, message));
                         }
                     });
            }

        };
    }

}
