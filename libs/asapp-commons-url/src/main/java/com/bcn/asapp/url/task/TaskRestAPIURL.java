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
package com.bcn.asapp.url.task;

/**
 * Task REST API URLs.
 *
 * @author ttrigo
 * @since 0.1.0
 */
public class TaskRestAPIURL {

    public static final String TASKS_ROOT_PATH = "/api/tasks";

    public static final String TASKS_GET_BY_ID_PATH = "/{id}";

    public static final String TASKS_GET_ALL_PATH = "";

    public static final String TASKS_GET_BY_PROJECT_ID_PATH = "/project/{id}";

    public static final String TASKS_CREATE_PATH = "";

    public static final String TASKS_UPDATE_BY_ID_PATH = "/{id}";

    public static final String TASKS_DELETE_BY_ID_PATH = "/{id}";

    public static final String TASKS_GET_BY_ID_FULL_PATH = TASKS_ROOT_PATH + TASKS_GET_BY_ID_PATH;

    public static final String TASKS_GET_ALL_FULL_PATH = TASKS_ROOT_PATH + TASKS_GET_ALL_PATH;

    public static final String TASKS_GET_BY_PROJECT_ID_FULL_PATH = TASKS_ROOT_PATH + TASKS_GET_BY_PROJECT_ID_PATH;

    public static final String TASKS_CREATE_FULL_PATH = TASKS_ROOT_PATH + TASKS_CREATE_PATH;

    public static final String TASKS_UPDATE_BY_ID_FULL_PATH = TASKS_ROOT_PATH + TASKS_UPDATE_BY_ID_PATH;

    public static final String TASKS_DELETE_BY_ID_FULL_PATH = TASKS_ROOT_PATH + TASKS_DELETE_BY_ID_PATH;

    private TaskRestAPIURL() {}

}
