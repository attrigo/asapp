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

package com.bcn.asapp.uaa.testutil;

import java.util.LinkedHashMap;
import java.util.List;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.springframework.http.ProblemDetail;

import com.bcn.asapp.uaa.infrastructure.error.InvalidRequestParameter;

public class ProblemDetailAssertions extends AbstractAssert<ProblemDetailAssertions, ProblemDetail> {

    ProblemDetailAssertions(ProblemDetail actual) {
        super(actual, ProblemDetailAssertions.class);
    }

    public static ProblemDetailAssertions assertThatProblemDetail(ProblemDetail actualProblemDetail) {
        return new ProblemDetailAssertions(actualProblemDetail);
    }

    public ProblemDetailAssertions hasType(String expectedType) {
        isNotNull();

        var actualType = actual.getType();
        Assertions.assertThat(actualType)
                  .isNotNull()
                  .describedAs("type")
                  .asString()
                  .isEqualTo(expectedType);

        return myself;
    }

    public ProblemDetailAssertions hasTitle(String expectedTitle) {
        isNotNull();

        var actualTitle = actual.getTitle();
        Assertions.assertThat(actualTitle)
                  .isNotNull()
                  .describedAs("title")
                  .isEqualTo(expectedTitle);

        return myself;
    }

    public ProblemDetailAssertions hasStatus(int expectedStatus) {
        isNotNull();

        var actualStatus = actual.getStatus();
        Assertions.assertThat(actualStatus)
                  .describedAs("status")
                  .isEqualTo(expectedStatus);

        return myself;
    }

    public ProblemDetailAssertions hasDetail(String expectedDetail) {
        isNotNull();
        var actualDetail = actual.getDetail();
        Assertions.assertThat(actualDetail)
                  .isNotNull()
                  .describedAs("detail")
                  .isEqualTo(expectedDetail);
        return myself;
    }

    public ProblemDetailAssertions containsDetails(String... expectedDetails) {
        isNotNull();
        var actualDetail = actual.getDetail();
        Assertions.assertThat(actualDetail)
                  .isNotNull()
                  .describedAs("details")
                  .contains(expectedDetails);
        return myself;
    }

    public ProblemDetailAssertions hasInstance(String expectedInstance) {
        isNotNull();
        var actualInstance = actual.getInstance();
        Assertions.assertThat(actualInstance)
                  .isNotNull()
                  .describedAs("instance")
                  .asString()
                  .isEqualTo(expectedInstance);
        return myself;
    }

    public ProblemDetailAssertions containsErrorProperty(String entity, String field, String message) {
        return containsErrorProperty(new InvalidRequestParameter(entity, field, message));
    }

    public ProblemDetailAssertions containsErrorProperty(InvalidRequestParameter expectedErrorProperty) {
        isNotNull();

        var actualProperties = actual.getProperties();
        Assertions.assertThat(actualProperties)
                  .isNotNull()
                  .describedAs("error properties")
                  .extractingByKey("errors")
                  .asInstanceOf(InstanceOfAssertFactories.LIST)
                  .isNotEmpty()
                  .satisfies(errors -> Assertions.assertThatList(errors)
                                                 .isNotEmpty()
                                                 .asInstanceOf(InstanceOfAssertFactories.list(LinkedHashMap.class))
                                                 .isNotEmpty());

        var actualErrorsList = (List<?>) actualProperties.get("errors");
        var actualErrorProperties = actualErrorsList.stream()
                                                    .map(LinkedHashMap.class::cast)
                                                    .map(actualError -> {
                                                        var entity = String.valueOf(actualError.get("entity"));
                                                        var field = String.valueOf(actualError.get("field"));
                                                        var message = String.valueOf(actualError.get("message"));
                                                        return new InvalidRequestParameter(entity, field, message);
                                                    })
                                                    .toList();
        Assertions.assertThatList(actualErrorProperties)
                  .describedAs("error properties")
                  .contains(expectedErrorProperty);

        return myself;
    }

}
