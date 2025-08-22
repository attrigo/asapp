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

package com.bcn.asapp.url.uaa;

/**
 * Defines the paths for authentication endpoints of UAA service.
 *
 * @author ttrigo
 * @since 0.2.0
 */
public class AuthRestAPIURL {

    public static final String AUTH_ROOT_PATH = "/api/auth";

    public static final String AUTH_TOKEN_PATH = "/token";

    public static final String AUTH_REFRESH_TOKEN_PATH = "/refresh";

    public static final String AUTH_REVOKE_PATH = "/revoke";

    public static final String AUTH_TOKEN_FULL_PATH = AUTH_ROOT_PATH + AUTH_TOKEN_PATH;

    public static final String AUTH_REFRESH_TOKEN_FULL_PATH = AUTH_ROOT_PATH + AUTH_REFRESH_TOKEN_PATH;

    public static final String AUTH_REVOKE_FULL_PATH = AUTH_ROOT_PATH + AUTH_REVOKE_PATH;

    private AuthRestAPIURL() {}

}
