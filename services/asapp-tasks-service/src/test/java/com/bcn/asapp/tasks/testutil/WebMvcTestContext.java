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

package com.bcn.asapp.tasks.testutil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.bcn.asapp.tasks.application.task.in.service.CreateTaskService;
import com.bcn.asapp.tasks.application.task.in.service.DeleteTaskService;
import com.bcn.asapp.tasks.application.task.in.service.ReadTaskService;
import com.bcn.asapp.tasks.application.task.in.service.UpdateTaskService;
import com.bcn.asapp.tasks.infrastructure.config.SecurityConfiguration;
import com.bcn.asapp.tasks.infrastructure.security.JwtVerifier;
import com.bcn.asapp.tasks.infrastructure.security.web.JwtAuthenticationEntryPoint;
import com.bcn.asapp.tasks.infrastructure.security.web.JwtAuthenticationFilter;
import com.bcn.asapp.tasks.infrastructure.task.in.TaskRestController;
import com.bcn.asapp.tasks.infrastructure.task.mapper.TaskMapper;

@WebMvcTest(TaskRestController.class)
@Import(value = { SecurityConfiguration.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class })
public class WebMvcTestContext {

    @Autowired
    protected MockMvcTester mockMvc;

    @MockitoBean
    private JwtVerifier jwtVerifierMock;

    @MockitoBean
    private ReadTaskService readTaskServiceMock;

    @MockitoBean
    private CreateTaskService createTaskServiceMock;

    @MockitoBean
    private UpdateTaskService updateTaskServiceMock;

    @MockitoBean
    private DeleteTaskService deleteTaskServiceMock;

    @MockitoBean
    private TaskMapper taskMapperMock;

}
