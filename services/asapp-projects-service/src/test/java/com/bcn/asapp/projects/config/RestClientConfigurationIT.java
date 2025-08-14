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

package com.bcn.asapp.projects.config;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.MediaType.APPLICATION_JSON;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.projects.AsappProjectsServiceApplication;
import com.bcn.asapp.projects.config.security.JwtAuthenticationToken;
import com.bcn.asapp.projects.testconfig.SecurityTestConfiguration;
import com.bcn.asapp.projects.testutil.JwtTestGenerator;

@SpringBootTest(classes = AsappProjectsServiceApplication.class)
@Import(SecurityTestConfiguration.class)
@Testcontainers(disabledWithoutDocker = true, parallel = true)
class RestClientConfigurationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Container
    static MockServerContainer mockServerContainer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    static MockServerClient mockServerClient;

    @Autowired
    private RestClient.Builder restClientBuilder;

    @Autowired
    private JwtTestGenerator jwtTestGenerator;

    private RestClient restClient;

    @BeforeAll
    public static void beforeAll() {
        mockServerClient = new MockServerClient(mockServerContainer.getHost(), mockServerContainer.getServerPort());
    }

    @BeforeEach
    void beforeEach() {
        SecurityContextHolder.clearContext();

        restClient = restClientBuilder.baseUrl(mockServerContainer.getEndpoint())
                                      .build();
    }

    @Test
    @DisplayName("GIVEN JWT is present in security context WHEN make HTTP call with RestClient THEN request is intercepted and the JWT is added to the request as Authorization header")
    void JwtIsPresentInSecurityContext_MakeHttpCallWithRestClient_JwtIsAddedToRequestAsAuthorizationHeader() {
        // Given
        var authentication = new JwtAuthenticationToken("USERNAME", "PASSWORD", List.of(new SimpleGrantedAuthority("USER")), jwtTestGenerator.generateJwt());
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);

        var request = request().withPath("/test")
                               .withHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authentication.getJwt());
        var response = response().withStatusCode(200)
                                 .withContentType(APPLICATION_JSON)
                                 .withBody("{\"result\":\"ok\"}");
        mockServerClient.when(request)
                        .respond(response);

        // When
        restClient.get()
                  .uri("/test")
                  .retrieve()
                  .toEntity(String.class);

        // Then
        var expectedRequest = request().withPath("/test")
                                       .withHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authentication.getJwt());
        mockServerClient.verify(expectedRequest);
    }

}
