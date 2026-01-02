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

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.redis.testcontainers.RedisContainer;

@TestConfiguration(proxyBeanMethods = false)
@Testcontainers
public class TestContainerConfiguration {

    static {
        // Force Docker API version 1.44 for compatibility with Docker Engine 29.x
        // See: https://github.com/testcontainers/testcontainers-java/issues/11232
        System.setProperty("api.version", "1.44");
    }

    @Container
    public static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17.7"));

    @Container
    public static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:8.4.0-alpine"));

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return postgreSQLContainer;
    }

    @Bean
    @ServiceConnection
    public RedisContainer redisContainer() {
        return redisContainer;
    }

}
