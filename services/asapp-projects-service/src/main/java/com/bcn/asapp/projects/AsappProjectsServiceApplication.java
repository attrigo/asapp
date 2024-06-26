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
package com.bcn.asapp.projects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @author ttrigo
 * @since 0.1.0
 */
@SpringBootApplication(scanBasePackages = "com.bcn.asapp")
public class AsappProjectsServiceApplication {

    /**
     * Main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(AsappProjectsServiceApplication.class, args);
    }

}
