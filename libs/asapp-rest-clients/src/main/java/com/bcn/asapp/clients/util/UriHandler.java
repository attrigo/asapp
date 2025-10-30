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

package com.bcn.asapp.clients.util;

import org.springframework.web.util.UriBuilder;

/**
 * Handler interface for creating URI builder instances in REST client operations.
 * <p>
 * This functional interface abstracts the creation of Spring's {@link UriBuilder}, allowing different implementations to provide builders preconfigured with
 * specific base URIs for different services. It follows the same organizational pattern as Spring Framework's utility classes in
 * {@code org.springframework.web.util}, providing URI handling support for REST client infrastructure.
 * <p>
 * <b>Purpose:</b>
 * <ul>
 * <li>Decouple REST client implementations from specific base URL configurations</li>
 * <li>Enable consistent URI construction across different service clients</li>
 * <li>Support thread-safe, stateless URI building in concurrent environments</li>
 * <li>Facilitate testing by allowing easy mocking or test-specific implementations</li>
 * </ul>
 * <p>
 * <b>Typical Usage Pattern:</b>
 *
 * <pre>
 * public class SomeServiceClient {
 * 
 *     private final RestClient restClient;
 * 
 *     private final UriHandler uriHandler;
 *
 *     public void callService(String param) {
 *         URI uri = uriHandler.newInstance()
 *                             .path("/api/endpoint/{id}")
 *                             .build(param);
 *
 *         restClient.get()
 *                   .uri(uri)
 *                   .retrieve()
 *                   .body(ResponseType.class);
 *     }
 * 
 * }
 * </pre>
 * <p>
 * <b>Implementation Notes:</b>
 * <ul>
 * <li>Each call to {@link #newInstance()} should return a fresh builder to avoid shared state issues</li>
 * <li>Implementations are typically created as Spring beans and injected into REST clients</li>
 * <li>The builder should be preconfigured with the target service's base URL</li>
 * </ul>
 *
 * @since 0.1.0
 * @see UriBuilder
 * @author attrigo
 */
@FunctionalInterface
public interface UriHandler {

    /**
     * Creates a new instance of {@link UriBuilder}.
     * <p>
     * Each invocation should return a fresh, independent builder instance to ensure thread-safety and prevent unintended state sharing between concurrent
     * requests. The returned builder is typically preconfigured with a base URI, allowing callers to append paths and parameters as needed.
     * <p>
     * <b>Thread Safety:</b> Implementations must ensure that each returned builder is a new instance, not a shared singleton, to support concurrent usage
     * without synchronization.
     * <p>
     * <b>Example:</b>
     *
     * <pre>
     * 
     * UriHandler handler = new DefaultUriHandler("http://localhost:8080/api");
     * 
     * URI uri = handler.newInstance()
     *                  .path("/users/{id}")
     *                  .queryParam("format", "json")
     *                  .build("123");
     * // Results in: http://localhost:8080/api/users/123?format=json
     * </pre>
     *
     * @return a new, independent instance of {@link UriBuilder} preconfigured for the target service
     */
    UriBuilder newInstance();

}
