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

package com.bcn.asapp.users.testutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.bcn.asapp.users.application.user.in.CreateUserUseCase;
import com.bcn.asapp.users.application.user.in.DeleteUserUseCase;
import com.bcn.asapp.users.application.user.in.ReadUserUseCase;
import com.bcn.asapp.users.application.user.in.UpdateUserUseCase;
import com.bcn.asapp.users.infrastructure.config.SecurityConfiguration;
import com.bcn.asapp.users.infrastructure.security.JwtVerifier;
import com.bcn.asapp.users.infrastructure.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.users.infrastructure.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.users.infrastructure.user.in.UserRestController;
import com.bcn.asapp.users.infrastructure.user.mapper.UserMapper;

/**
 * Configures web layer test context with mocked dependencies for REST controller integration tests.
 * <p>
 * This base class enables Spring test context reusing across multiple controller test classes. When test classes extend this base and share the same
 * configuration, Spring reuses the same ApplicationContext instead of creating a new one per test class, significantly reducing test execution time. Uses
 * {@code @WebMvcTest} to load only web layer components.
 *
 * @see WebMvcTest
 * @since 0.2.0
 */
@WebMvcTest(UserRestController.class)
@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
public class WebMvcTestContext {

    @MockitoBean
    private JwtVerifier jwtVerifier;

    @Autowired
    protected MockMvcTester mockMvcTester;

    @MockitoBean
    protected ReadUserUseCase readUserUseCase;

    @MockitoBean
    protected CreateUserUseCase createUserUseCase;

    @MockitoBean
    protected UpdateUserUseCase updateUserUseCase;

    @MockitoBean
    protected DeleteUserUseCase deleteUserUseCase;

    @MockitoBean
    protected UserMapper userMapper;

}
