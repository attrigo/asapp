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

package com.bcn.asapp.users.infrastructure.user.mapper;

import static com.bcn.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.users.application.user.in.result.UserWithTasksResult;
import com.bcn.asapp.users.infrastructure.user.in.response.WarningDetail;

/**
 * Tests {@link UserMapper} mapping from application results to REST responses.
 * <p>
 * Coverage:
 * <li>Maps a degraded result to an empty task list and a single structured warning</li>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { UserMapperImpl.class, UserObjectFactory.class, UserIdMapperImpl.class, FirstNameMapperImpl.class, LastNameMapperImpl.class,
        EmailMapperImpl.class, PhoneNumberMapperImpl.class })
class UserMapperTests {

    @Autowired
    private UserMapper userMapper;

    @Nested
    class ToGetUserByIdResponse {

        @Test
        void ReturnsEmptyTaskIdsAndStructuredWarning_TasksUnavailable() {
            // Given
            var user = aUser();
            var degradedResult = UserWithTasksResult.unavailable(user);
            var expectedWarningDetail = WarningDetail.Reason.TASKS_UNAVAILABLE.toDetail();

            // When
            var actual = userMapper.toGetUserByIdResponse(degradedResult);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.taskIds()).as("task IDs").isEmpty();
                softly.assertThat(actual.warnings()).as("degradation warnings").containsExactly(expectedWarningDetail);
                // @formatter:on
            });
        }

    }

}
