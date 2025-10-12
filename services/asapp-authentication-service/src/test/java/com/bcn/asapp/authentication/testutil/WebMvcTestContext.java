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

package com.bcn.asapp.authentication.testutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.bcn.asapp.authentication.application.authentication.in.AuthenticateUseCase;
import com.bcn.asapp.authentication.application.authentication.in.RefreshAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.in.RevokeAuthenticationUseCase;
import com.bcn.asapp.authentication.application.authentication.out.JwtVerifier;
import com.bcn.asapp.authentication.application.user.in.service.CreateUserService;
import com.bcn.asapp.authentication.application.user.in.service.DeleteUserService;
import com.bcn.asapp.authentication.application.user.in.service.ReadUserService;
import com.bcn.asapp.authentication.application.user.in.service.UpdateUserService;
import com.bcn.asapp.authentication.infrastructure.authentication.in.AuthenticationRestController;
import com.bcn.asapp.authentication.infrastructure.authentication.mapper.JwtAuthenticationMapper;
import com.bcn.asapp.authentication.infrastructure.config.SecurityConfiguration;
import com.bcn.asapp.authentication.infrastructure.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.authentication.infrastructure.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.authentication.infrastructure.user.in.UserRestController;
import com.bcn.asapp.authentication.infrastructure.user.mapper.UserMapper;

@WebMvcTest(controllers = { AuthenticationRestController.class, UserRestController.class })
@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
public class WebMvcTestContext {

    @Autowired
    protected MockMvcTester mockMvc;

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @MockitoBean
    private AuthenticateUseCase authenticateUseCaseMock;

    @MockitoBean
    private RefreshAuthenticationUseCase refreshAuthenticationUseCaseMock;

    @MockitoBean
    private RevokeAuthenticationUseCase revokeAuthenticationUseCaseMock;

    @MockitoBean
    private JwtAuthenticationMapper jwtAuthenticationMapperMock;

    @MockitoBean
    private ReadUserService readUserServiceMock;

    @MockitoBean
    private CreateUserService createUserServiceMock;

    @MockitoBean
    private UpdateUserService updateUserServiceMock;

    @MockitoBean
    private DeleteUserService deleteUserServiceMock;

    @MockitoBean
    private UserMapper userMapperMock;

}
