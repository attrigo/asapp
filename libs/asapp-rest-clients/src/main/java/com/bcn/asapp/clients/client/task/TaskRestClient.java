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
package com.bcn.asapp.clients.client.task;

import static com.bcn.asapp.url.task.TaskRestAPIURL.TASKS_GET_BY_PROJECT_ID_FULL_PATH;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.bcn.asapp.clients.internal.uri.UriHandler;
import com.bcn.asapp.dto.task.TaskDTO;

/**
 * {@link RestClient} implementation of the {@link TaskClient}.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@Component
public class TaskRestClient implements TaskClient {

    private final RestClient restClient;

    private final UriHandler tasksServiceUriHandler;

    /**
     * Default constructor.
     *
     * @param restClientBuilder      the Spring's {@link RestClient.Builder}.
     * @param tasksServiceUriHandler the {@link UriHandler} to build REST tasks services URIs.
     */
    public TaskRestClient(RestClient.Builder restClientBuilder, UriHandler tasksServiceUriHandler) {
        this.restClient = restClientBuilder.build();
        this.tasksServiceUriHandler = tasksServiceUriHandler;
    }

    @Override
    public List<TaskDTO> getTasksByProjectId(UUID id) {
        URI uri = tasksServiceUriHandler.newInstance()
                                        .path(TASKS_GET_BY_PROJECT_ID_FULL_PATH)
                                        .build(id);

        return restClient.get()
                         .uri(uri)
                         .retrieve()
                         .body(new ParameterizedTypeReference<>() {});
    }

}
