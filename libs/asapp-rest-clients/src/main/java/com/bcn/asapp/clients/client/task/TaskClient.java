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

import java.util.List;
import java.util.UUID;

import com.bcn.asapp.dto.task.TaskDTO;

/**
 * REST Client to perform HTTP operations against the REST tasks service.
 *
 * @author ttrigo
 * @since 0.1.0
 */
public interface TaskClient {

    /**
     * Gets the tasks of the given project id.
     * <p>
     * The operation returns {@literal null} as a fallback value in case there are some issue with tasks service.
     *
     * @param id the id of the project.
     * @return the tasks of the given project, empty if there aren't tasks for the given project, or null if the tasks service is not available.
     */
    List<TaskDTO> getTasksByProjectId(UUID id);

}
