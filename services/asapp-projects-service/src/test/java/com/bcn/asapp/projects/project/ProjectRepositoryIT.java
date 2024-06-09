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
package com.bcn.asapp.projects.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private ProjectRepository projectRepository;

    private String fakeProjectTitle;

    private String fakeProjectDescription;

    private LocalDateTime fakeProjectStartDate;

    @BeforeEach
    void beforeEach() {
        projectRepository.deleteAll();

        this.fakeProjectTitle = "IT Title";
        this.fakeProjectDescription = "IT Description";
        this.fakeProjectStartDate = LocalDateTime.now()
                                                 .truncatedTo(ChronoUnit.MILLIS);
    }

    // deleteProjectById
    @Test
    @DisplayName("GIVEN project id not exists WHEN delete a project by id THEN does not delete the project And returns zero")
    void ProjectIdNotExists_DeleteProjectById_DoesNotDeleteProjectAndReturnsZero() {
        // When
        var idToDelete = UUID.randomUUID();

        var actual = projectRepository.deleteProjectById(idToDelete);

        // Then
        assertEquals(0L, actual);
    }

    @Test
    @DisplayName("GIVEN project id exists WHEN delete a project by id THEN deletes the project And returns the amount of projects deleted")
    void ProjectIdExists_DeleteProjectById_DeletesProjectAndReturnsAmountOfProjectsDeleted() {
        // Given
        var fakeProject = new Project(null, fakeProjectTitle, fakeProjectDescription, fakeProjectStartDate);
        var projectToBeDeleted = projectRepository.save(fakeProject);
        assertNotNull(projectToBeDeleted);

        // When
        var idToDelete = projectToBeDeleted.id();

        var actual = projectRepository.deleteProjectById(idToDelete);

        // Then
        assertEquals(1L, actual);

        assertFalse(projectRepository.findById(projectToBeDeleted.id())
                                     .isPresent());
    }

}
