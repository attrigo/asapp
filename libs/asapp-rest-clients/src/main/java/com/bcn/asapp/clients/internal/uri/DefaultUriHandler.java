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
package com.bcn.asapp.clients.internal.uri;

import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Default implementation of {@link UriHandler}.
 * <p>
 * Provides new instances of {@link UriBuilder} which are preconfigured with the base URI of a specific REST service.
 * <p>
 * Delegates the creation of {@link UriBuilder} to {@link UriComponentsBuilder}.
 */
public class DefaultUriHandler implements UriHandler {

    private final String baseUri;

    /**
     * Default constructor.
     * 
     * @param baseUri the base URI of the target REST service.
     */
    public DefaultUriHandler(String baseUri) {
        this.baseUri = baseUri;
    }

    public UriBuilder newInstance() {
        return UriComponentsBuilder.fromUriString(baseUri);
    }

}
