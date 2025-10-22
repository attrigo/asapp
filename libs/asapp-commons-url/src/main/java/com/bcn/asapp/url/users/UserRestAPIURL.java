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

package com.bcn.asapp.url.users;

/**
 * Defines the paths for user domain endpoints of asapp-users-service.
 *
 * @since 0.2.0
 * @author ttrigo
 */
public class UserRestAPIURL {

    public static final String USERS_ROOT_PATH = "/api/users";

    public static final String USERS_GET_BY_ID_PATH = "/{id}";

    public static final String USERS_GET_ALL_PATH = "";

    public static final String USERS_CREATE_PATH = "";

    public static final String USERS_UPDATE_BY_ID_PATH = "/{id}";

    public static final String USERS_DELETE_BY_ID_PATH = "/{id}";

    public static final String USERS_GET_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_GET_BY_ID_PATH;

    public static final String USERS_GET_ALL_FULL_PATH = USERS_ROOT_PATH + USERS_GET_ALL_PATH;

    public static final String USERS_CREATE_FULL_PATH = USERS_ROOT_PATH + USERS_CREATE_PATH;

    public static final String USERS_UPDATE_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_UPDATE_BY_ID_PATH;

    public static final String USERS_DELETE_BY_ID_FULL_PATH = USERS_ROOT_PATH + USERS_DELETE_BY_ID_PATH;

    private UserRestAPIURL() {}

}
