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

package com.attrigo.asapp.tasks.infrastructure.error;

import static com.attrigo.asapp.url.tasks.TaskApiUrl.TASKS_GET_BY_ID_FULL_PATH;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import com.attrigo.asapp.tasks.testutil.WebMvcTestContext;

/**
 * Tests {@link GlobalExceptionHandler} routing of unhandled exceptions through the MVC dispatch pipeline.
 * <p>
 * Setup:
 * <li>Loads the web layer with a mock MVC environment and mocked service collaborators</li>
 * <p>
 * Coverage:
 * <li>Intercepts an unhandled exception escaping a use case and returns a 500 RFC 7807 Problem Detail instead of a raw Spring error</li>
 */
@WithMockUser
class GlobalExceptionHandlerIT extends WebMvcTestContext {

    @Test
    void ReturnsStatusInternalServerErrorAndBodyWithProblemDetail_UnexpectedError() {
        // Given
        var taskId = UUID.fromString("e3a8c5d1-7f9b-482b-9f6a-2d8e5b7c9f3a");
        var requestBuilder = get(TASKS_GET_BY_ID_FULL_PATH, taskId);

        given(readTaskUseCase.getTaskById(taskId)).willThrow(new RuntimeException("Simulated unexpected failure"));

        // When & Then
        mockMvcTester.perform(requestBuilder)
                     .assertThat()
                     .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                     .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                     .bodyJson()
                     .convertTo(String.class)
                     .satisfies(json -> assertThatJson(json).isObject()
                                                            .containsEntry("detail", "An internal error occurred")
                                                            .containsEntry("critical", true));
    }

}
