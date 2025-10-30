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
import org.springframework.web.util.UriComponentsBuilder;

import io.micrometer.common.util.StringUtils;

/**
 * Default implementation of {@link UriHandler} using Spring's {@link UriComponentsBuilder}.
 * <p>
 * This implementation provides thread-safe, stateless URI construction by creating fresh {@link UriBuilder} instances preconfigured with a base URI for each
 * invocation. It follows the same utility pattern as Spring Framework's web utilities in {@code org.springframework.web.util}, delegating URI builder creation
 * to {@link UriComponentsBuilder}.
 * <p>
 * Each call to {@link #newInstance()} returns a new {@link UriBuilder} initialized with the configured base URI, ensuring that concurrent REST client
 * operations do not interfere with each other through shared builder state.
 * <p>
 * <b>Thread Safety:</b> This class is immutable and thread-safe. The base URI is stored as a final field and never modified after construction. Each builder
 * instance is created fresh, preventing any shared mutable state.
 * <p>
 * <b>Usage Example:</b>
 *
 * <pre>
 * 
 * UriHandler handler = new DefaultUriHandler("http://localhost:8080/api");
 * 
 * URI uri = handler.newInstance()
 *                  .path("/users/{id}")
 *                  .build("123");
 * // Results in: http://localhost:8080/api/users/123
 * </pre>
 *
 * @since 0.1.0
 * @see UriComponentsBuilder
 * @author attrigo
 */
public class DefaultUriHandler implements UriHandler {

    private final String baseUri;

    /**
     * Constructs a new {@code DefaultUriHandler} with the specified base URI.
     * <p>
     * The base URI should be a complete, valid URI string representing the root URL of the target service (e.g., {@code "http://localhost:8080/api"}). This URI
     * will be prepended to all paths constructed using builders returned by {@link #newInstance()}.
     * <p>
     * <b>Validation:</b> The base URI must not be {@code null} or blank. If an invalid URI is provided, the behavior depends on Spring's
     * {@link UriComponentsBuilder} parsing rules.
     * <p>
     * <b>Examples:</b>
     * <ul>
     * <li>{@code new DefaultUriHandler("http://localhost:8080/api")} - HTTP with path</li>
     * <li>{@code new DefaultUriHandler("https://example.com")} - HTTPS without path</li>
     * <li>{@code new DefaultUriHandler("http://service-name:8080")} - Internal service name (Docker/Kubernetes)</li>
     * </ul>
     *
     * @param baseUri the base URI of the target REST service; must not be {@code null} or blank
     * @throws IllegalArgumentException if baseUri is {@code null} or blank
     */
    public DefaultUriHandler(String baseUri) {
        if (StringUtils.isBlank(baseUri)) {
            throw new IllegalArgumentException("Base URI must not be null or blank");
        }
        this.baseUri = baseUri;
    }

    /**
     * Creates a new {@link UriBuilder} instance preconfigured with the base URI.
     * <p>
     * Each invocation returns a fresh, independent builder initialized with the base URI provided during construction. This allows callers to safely append
     * paths, path variables, and query parameters without affecting other concurrent operations.
     * <p>
     * The returned builder delegates to Spring's {@link UriComponentsBuilder}, providing full support for URI templates, path expansion, query parameters, and
     * URI encoding.
     * <p>
     * <b>Thread Safety:</b> This method is thread-safe and can be called concurrently. Each call creates a new builder instance, preventing shared state
     * issues.
     * <p>
     * <b>Example Usage:</b>
     *
     * <pre>
     * 
     * // Simple path appending
     * URI uri1 = handler.newInstance()
     *                   .path("/users")
     *                   .build();
     * // Result: http://localhost:8080/api/users
     *
     * // Path variables and query parameters
     * URI uri2 = handler.newInstance()
     *                   .path("/users/{id}")
     *                   .queryParam("format", "json")
     *                   .build("123");
     * // Result: http://localhost:8080/api/users/123?format=json
     *
     * // Concurrent usage (safe)
     * CompletableFuture&lt;URI&gt; future1 = CompletableFuture.supplyAsync(() -&gt; handler.newInstance()
     *                                                                             .path("/endpoint1")
     *                                                                             .build());
     * 
     * CompletableFuture&lt;URI&gt; future2 = CompletableFuture.supplyAsync(() -&gt; handler.newInstance()
     *                                                                             .path("/endpoint2")
     *                                                                             .build());
     * // Both futures complete safely without interference
     * </pre>
     *
     * @return a new, independent {@link UriBuilder} initialized with the base URI
     */
    @Override
    public UriBuilder newInstance() {
        return UriComponentsBuilder.fromUriString(this.baseUri);
    }

}
