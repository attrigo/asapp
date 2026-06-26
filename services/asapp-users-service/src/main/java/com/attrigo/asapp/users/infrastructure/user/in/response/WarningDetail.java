/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.attrigo.asapp.users.infrastructure.user.in.response;

/**
 * Represents a structured non-fatal degradation warning surfaced in a successful response.
 *
 * @param code      the machine-readable warning code identifying the degradation type
 * @param field     the response field affected by the degradation
 * @param message   the human-readable description of the degradation
 * @param retryable whether the client may retry the request to obtain complete data
 * @since 0.4.0
 * @author attrigo
 */
public record WarningDetail(
        String code,
        String field,
        String message,
        boolean retryable
) {

    /**
     * Catalogs all known degradation warning reasons, each carrying its own code, field, message, and retryability.
     *
     * @since 0.4.0
     * @author attrigo
     */
    public enum Reason {

        TASK_IDS_UNAVAILABLE("task_ids_unavailable", "taskIds", "Tasks could not be retrieved and may be incomplete.", true);

        private final String code;

        private final String field;

        private final String message;

        private final boolean retryable;

        /**
         * Constructs a new {@code Reason} with the attributes of a degradation warning.
         *
         * @param code      the machine-readable warning code identifying the degradation type
         * @param field     the response field affected by the degradation
         * @param message   the human-readable description of the degradation
         * @param retryable whether the client may retry the request to obtain complete data
         */
        Reason(String code, String field, String message, boolean retryable) {
            this.code = code;
            this.field = field;
            this.message = message;
            this.retryable = retryable;
        }

        /**
         * Creates a {@link WarningDetail} instance from this warning reason.
         *
         * @return a new {@link WarningDetail} populated with this reason's values
         */
        public WarningDetail toDetail() {
            return new WarningDetail(code, field, message, retryable);
        }

    }

}
